package com.mobilenotes.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobilenotes.app.data.local.converter.Converters
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
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun folderDao(): FolderDao
    abstract fun noteVersionDao(): NoteVersionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobilenotes.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
