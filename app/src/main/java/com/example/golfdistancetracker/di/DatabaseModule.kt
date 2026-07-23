package com.example.golfdistancetracker.di

import android.content.Context
import androidx.room.Room
import com.example.golfdistancetracker.data.db.GolfDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): GolfDatabase {
        return Room.databaseBuilder(context, GolfDatabase::class.java, "golf_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideClubDao(db: GolfDatabase) = db.clubDao()

    @Provides
    fun provideShotDao(db: GolfDatabase) = db.shotDao()

    @Provides
    fun provideCourseDao(db: GolfDatabase) = db.courseDao()

    @Provides
    fun provideRoundDao(db: GolfDatabase) = db.roundDao()
}
