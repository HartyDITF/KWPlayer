package fyi.kiyori.modernmediaplayer

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import fyi.kiyori.modernmediaplayer.data.database.KwDatabase
import fyi.kiyori.modernmediaplayer.data.database.MediaEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers

data class LocalMediaItem(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modified: Long
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")
}

object MediaLibrary {
    private const val PREFS = "kw_player_library"
    private const val FOLDERS = "folders"
    private const val ITEMS = "items"
    private const val FAVORITES = "favorites"
    private const val HISTORY = "history"

    fun folders(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(FOLDERS, emptySet())?.toSet() ?: emptySet()

    fun addFolder(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val values = prefs.getStringSet(FOLDERS, emptySet())?.toMutableSet() ?: mutableSetOf()
        values.add(uri.toString())
        prefs.edit().putStringSet(FOLDERS, values).apply()
    }

    fun folderLabel(uri: String): String = runCatching {
        DocumentsContract.getTreeDocumentId(Uri.parse(uri))
            .substringAfterLast(':')
            .substringAfterLast('/')
            .ifBlank { "Папка" }
    }.getOrDefault("Папка")

    fun isInFolder(itemUri: Uri, folderUri: String): Boolean = runCatching {
        DocumentsContract.getTreeDocumentId(itemUri) ==
            DocumentsContract.getTreeDocumentId(Uri.parse(folderUri))
    }.getOrDefault(false)

    fun removeFolder(context: Context, uri: String) {
        val treeUri = Uri.parse(uri)
        context.contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == treeUri }
            ?.let { permission ->
                var flags = 0
                if (permission.isReadPermission) flags = flags or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (permission.isWritePermission) flags = flags or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                if (flags != 0) {
                    runCatching { context.contentResolver.releasePersistableUriPermission(treeUri, flags) }
                }
            }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val values = prefs.getStringSet(FOLDERS, emptySet())?.toMutableSet() ?: mutableSetOf()
        values.remove(uri)
        prefs.edit().putStringSet(FOLDERS, values).apply()
    }

    fun scan(context: Context): List<LocalMediaItem> {
        val resolver = context.contentResolver
        val found = LinkedHashMap<String, LocalMediaItem>()
        val queue = ArrayDeque<Triple<Uri, String, Int>>()
        folders(context).forEach { raw ->
            runCatching {
                val treeUri = Uri.parse(raw)
                queue.add(Triple(treeUri, DocumentsContract.getTreeDocumentId(treeUri), 0))
            }
        }
        val visited = HashSet<String>()
        while (queue.isNotEmpty()) {
            val (treeUri, parentId, depth) = queue.removeFirst()
            if (depth > 32 || !visited.add("$treeUri|$parentId")) continue
            val childrenUri = runCatching {
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            }.getOrNull() ?: continue
            var cursor: Cursor? = null
            try {
                cursor = resolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    ),
                    null, null, null
                )
                cursor?.use { rows ->
                    val idCol = rows.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = rows.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = rows.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = rows.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modifiedCol = rows.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    while (rows.moveToNext()) {
                        if (idCol < 0 || nameCol < 0 || mimeCol < 0) continue
                        val id = rows.getString(idCol) ?: continue
                        val name = rows.getString(nameCol) ?: continue
                        val mime = rows.getString(mimeCol) ?: ""
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(Triple(treeUri, id, depth + 1))
                        } else if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                            found[childUri.toString()] = LocalMediaItem(
                                childUri, name, mime,
                                if (sizeCol >= 0 && !rows.isNull(sizeCol)) rows.getLong(sizeCol) else 0L,
                                if (modifiedCol >= 0 && !rows.isNull(modifiedCol)) rows.getLong(modifiedCol) else 0L
                            )
                        }
                    }
                }
            } catch (_: SecurityException) {
                // Folder access can be revoked by the user.
            } catch (_: Exception) {
                // An unavailable provider must not stop scanning other selected folders.
            } finally {
                cursor?.close()
            }
        }
        val result = found.values.sortedBy { it.name.lowercase() }
        saveItems(context, result)
        runCatching { runBlocking(Dispatchers.IO) { KwDatabase.get(context).mediaDao().upsertAll(result.map { it.toEntity() }) } }
        return result
    }

    fun loadItems(context: Context): List<LocalMediaItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ITEMS, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(LocalMediaItem(
                        Uri.parse(item.getString("uri")),
                        item.getString("name"),
                        item.getString("mime"),
                        item.optLong("size"),
                        item.optLong("modified")
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun loadFromRoom(context: Context): List<LocalMediaItem> = KwDatabase.get(context).mediaDao().getAll().map { it.toLocalMediaItem() }

    suspend fun saveToRoom(context: Context, items: List<LocalMediaItem>) {
        KwDatabase.get(context).mediaDao().upsertAll(items.map { it.toEntity() })
    }

    private fun LocalMediaItem.toEntity() = MediaEntity(uri.toString(), name, mimeType, size, modified)
    private fun MediaEntity.toLocalMediaItem() = LocalMediaItem(Uri.parse(uri), name, mimeType, size, modified)

    private fun saveItems(context: Context, items: List<LocalMediaItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject()
                .put("uri", item.uri.toString())
                .put("name", item.name)
                .put("mime", item.mimeType)
                .put("size", item.size)
                .put("modified", item.modified))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(ITEMS, array.toString()).apply()
    }

    fun favorites(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun toggleFavorite(context: Context, uri: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val values = prefs.getStringSet(FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val added = if (values.add(uri)) true else {
            values.remove(uri)
            false
        }
        prefs.edit().putStringSet(FAVORITES, values).apply()
        return added
    }

    fun history(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(HISTORY, null)?.let { raw ->
                runCatching {
                    val array = JSONArray(raw)
                    buildList { for (i in 0 until array.length()) add(array.getString(i)) }
                }.getOrDefault(emptyList())
            } ?: emptyList()

    fun addHistory(context: Context, uri: String) {
        val values = history(context).toMutableList()
        values.remove(uri)
        values.add(0, uri)
        val array = JSONArray()
        values.take(100).forEach(array::put)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(HISTORY, array.toString()).apply()
    }
}
