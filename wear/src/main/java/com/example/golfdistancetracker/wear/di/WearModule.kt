package com.example.golfdistancetracker.wear.di

import android.content.Context
import com.example.golfdistancetracker.wear.LocationHelper
import com.example.golfdistancetracker.wear.SwingAnalyzer
import com.example.golfdistancetracker.wear.WearDataService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearModule {

    @Provides
    @Singleton
    fun provideSwingAnalyzer(@ApplicationContext context: Context): SwingAnalyzer {
        return SwingAnalyzer(context)
    }

    @Provides
    @Singleton
    fun provideLocationHelper(@ApplicationContext context: Context): LocationHelper {
        return LocationHelper(context)
    }

    @Provides
    @Singleton
    fun provideWearDataService(@ApplicationContext context: Context): WearDataService {
        return WearDataService(context)
    }
}
