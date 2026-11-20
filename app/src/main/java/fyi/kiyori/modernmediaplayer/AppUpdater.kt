package fyi.kiyori.modernmediaplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class KwRelease(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long
)

object AppUpdater {
    private const val RELEASE_API =
        "https://api.github.com/repos/HartyDITF/KWPlayer/releases/latest"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun check(onResult: (KwRelease?, String?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "KW-Player-Android")
                }
                if (connection.responseCode !in 200..299) {
                    error("GitHub вернул HTTP ${connection.responseCode}")
                }
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val version = json.optString("tag_name").removePrefix("v")
                val assets = json.optJSONArray("assets") ?: error("В релизе нет APK")
                var apkUrl: String? = null
                var apkSize = 0L
                for (index in 0 until assets.length()) {
                    val asset = assets.getJSONObject(index)
                    if (asset.optString("name") == "KWPlayer.apk") {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size")
                        break
                    }
                }
                val url = apkUrl ?: error("В последнем релизе не найден KWPlayer.apk")
                val release = KwRelease(
                    versionName = version,
                    notes = json.optString("body").ifBlank { "Описание изменений не добавлено." },
                    apkUrl = url,
                    apkSize = apkSize
                )
                mainHandler.post {
                    if (compareVersions(release.versionName, BuildConfig.VERSION_NAME) > 0) {
                        onResult(release, null)
                    } else {
                        onResult(null, null)
                    }
                }
            } catch (error: Exception) {
                mainHandler.post {
                    onResult(null, error.message ?: "Не удалось проверить обновления")
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    fun download(context: android.content.Context, release: KwRelease, onResult: (File?, String?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "KW-Player-Android")
                }
                if (connection.responseCode !in 200..299) {
                    error("Не удалось скачать APK: HTTP ${connection.responseCode}")
                }
                val apk = File(context.cacheDir, "KWPlayer-update.apk")
                connection.inputStream.use { input ->
                    apk.outputStream().use { output -> input.copyTo(output) }
                }
                if (!apk.isFile || apk.length() == 0L) error("Скачанный APK пуст")
                mainHandler.post { onResult(apk, null) }
            } catch (error: Exception) {
                mainHandler.post {
                    onResult(null, error.message ?: "Ошибка загрузки APK")
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    fun install(activity: Activity, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}")
                )
            )
            return false
        }

        val apkUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
        return true
    }

    private fun compareVersions(first: String, second: String): Int {
        val left = first.split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
        val right = second.split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(left.size, right.size)) {
            val difference = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }
}
