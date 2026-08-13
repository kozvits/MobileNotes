package com.mobilenotes.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.mobilenotes.app.data.local.dao.FolderDao
import com.mobilenotes.app.data.local.dao.NoteDao
import com.mobilenotes.app.data.local.dao.NoteTagDao
import com.mobilenotes.app.data.local.dao.NoteVersionDao
import com.mobilenotes.app.data.local.dao.TagDao
import com.mobilenotes.app.data.local.entity.FolderEntity
import com.mobilenotes.app.data.local.entity.NoteEntity
import com.mobilenotes.app.data.local.entity.NoteTagCrossRef
import com.mobilenotes.app.data.local.entity.NoteVersionEntity
import com.mobilenotes.app.data.local.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        FolderEntity::class,
        NoteVersionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun folderDao(): FolderDao
    abstract fun noteVersionDao(): NoteVersionDao

    companion object {
        /** v1 -> v2: add `emoji` column to `tags` table (nullable, defaults to NULL). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tags ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
                // Normalize NULL to empty string so downstream code can rely on non-null.
                db.execSQL("UPDATE tags SET emoji = '' WHERE emoji IS NULL")
            }
        }

        private val MIGRATIONS = arrayOf(MIGRATION_1_2)

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobilenotes.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
