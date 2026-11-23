package fyi.kiyori.modernmediaplayer.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(items: List<MediaEntity>)

    @Query("UPDATE media_items SET name = :name, mimeType = :mimeType, size = :size, modified = :modified WHERE uri = :uri")
    suspend fun updateMetadata(uri: String, name: String, mimeType: String, size: Long, modified: Long)

    @Transaction
    suspend fun upsertAll(items: List<MediaEntity>) {
        insertMissing(items)
        items.forEach { updateMetadata(it.uri, it.name, it.mimeType, it.size, it.modified) }
    }

    @Query("UPDATE media_items SET lastPositionMs = :positionMs, durationMs = :durationMs WHERE uri = :uri")
    suspend fun updatePlaybackProgress(uri: String, positionMs: Long, durationMs: Long)

    @Query("SELECT uri FROM media_items WHERE durationMs > 0 AND lastPositionMs >= 10000 AND lastPositionMs < durationMs * 0.95")
    suspend fun getUnwatchedUris(): List<String>

    @Query("SELECT lastPositionMs FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getPosition(uri: String): Long?
}
