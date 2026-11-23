package fyi.kiyori.modernmediaplayer.data.database
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "media_items")
data class MediaEntity(@PrimaryKey val uri: String, val name: String, val mimeType: String, val size: Long, val modified: Long, val lastPositionMs: Long = 0L, val durationMs: Long = 0L, val lastPlayedAt: Long = 0L, val favorite: Boolean = false)
