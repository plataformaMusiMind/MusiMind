package com.musimind.di

import com.musimind.music.audio.GameAudioManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Audio dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    
    // GameAudioManager is provided automatically by @Inject constructor
    // This module can be extended for additional audio-related dependencies
}
