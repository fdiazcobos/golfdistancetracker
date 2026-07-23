package com.example.golfdistancetracker.di

import android.content.Context
import com.example.golfdistancetracker.util.CompassHelper
import com.example.golfdistancetracker.util.LocationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideLocationHelper(@ApplicationContext context: Context) = LocationHelper(context)

    @Provides
    @Singleton
    fun provideCompassHelper(@ApplicationContext context: Context) = CompassHelper(context)
}
