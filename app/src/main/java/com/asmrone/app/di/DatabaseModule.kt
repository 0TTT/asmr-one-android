package com.asmrone.app.di

import android.content.Context
import androidx.room.Room
import com.asmrone.app.data.local.AsmrOneDatabase
import com.asmrone.app.data.local.DownloadDao
import com.asmrone.app.data.local.FavoriteDao
import com.asmrone.app.data.local.HistoryDao
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
    fun provideDatabase(@ApplicationContext context: Context): AsmrOneDatabase =
        Room.databaseBuilder(context, AsmrOneDatabase::class.java, "asmr_one.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteDao(db: AsmrOneDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: AsmrOneDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideDownloadDao(db: AsmrOneDatabase): DownloadDao = db.downloadDao()
}
