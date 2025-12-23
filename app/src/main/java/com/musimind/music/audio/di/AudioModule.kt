package com.musimind.music.audio.di

import android.content.Context
import com.musimind.music.audio.metronome.Metronome
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.audio.pitch.PitchDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for audio-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    
    @Provides
    @Singleton
    fun providePitchDetector(
        @ApplicationContext context: Context
    ): PitchDetector {
        return PitchDetector(context)
    }
    
    @Provides
    @Singleton
    fun provideMetronome(
        @ApplicationContext context: Context
    ): Metronome {
        return Metronome(context)
    }
    
    @Provides
    @Singleton
    fun provideMidiPlayer(
        @ApplicationContext context: Context
    ): MidiPlayer {
        return MidiPlayer(context)
    }
}
