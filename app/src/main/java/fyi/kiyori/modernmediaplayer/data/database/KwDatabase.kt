package fyi.kiyori.modernmediaplayer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MediaEntity::class], version = 2, exportSchema = false)
abstract class KwDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_items ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: KwDatabase? = null

        fun get(context: Context): KwDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KwDatabase::class.java,
                "kw_player_library.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
