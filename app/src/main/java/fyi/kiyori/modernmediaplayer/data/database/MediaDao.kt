package fyi.kiyori.modernmediaplayer.data.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao interface MediaDao {
 @Query("SELECT * FROM media_items ORDER BY name COLLATE NOCASE") suspend fun getAll(): List<MediaEntity>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(items: List<MediaEntity>)
 @Query("UPDATE media_items SET lastPositionMs = :positionMs WHERE uri = :uri") suspend fun updatePosition(uri: String, positionMs: Long)
 @Query("SELECT lastPositionMs FROM media_items WHERE uri = :uri LIMIT 1") suspend fun getPosition(uri: String): Long?
}
