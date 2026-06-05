package com.mobilenotes.app.di

import android.content.Context
import androidx.room.Room
import com.mobilenotes.app.data.local.AppDatabase
import com.mobilenotes.app.data.local.dao.FolderDao
import com.mobilenotes.app.data.local.dao.NoteDao
import com.mobilenotes.app.data.local.dao.NoteTagDao
import com.mobilenotes.app.data.local.dao.NoteVersionDao
import com.mobilenotes.app.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideNoteTagDao(database: AppDatabase): NoteTagDao = database.noteTagDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideNoteVersionDao(database: AppDatabase): NoteVersionDao = database.noteVersionDao()
}
