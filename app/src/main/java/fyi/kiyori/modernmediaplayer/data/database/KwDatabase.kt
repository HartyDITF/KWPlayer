package fyi.kiyori.modernmediaplayer.data.database
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(entities = [MediaEntity::class], version = 1, exportSchema = false)
abstract class KwDatabase : RoomDatabase() {
 abstract fun mediaDao(): MediaDao
 companion object {
  @Volatile private var instance: KwDatabase? = null
  fun get(context: Context): KwDatabase = instance ?: synchronized(this) {
   instance ?: Room.databaseBuilder(context.applicationContext, KwDatabase::class.java, "kw_player_library.db").build().also { instance = it }
  }
 }
}
