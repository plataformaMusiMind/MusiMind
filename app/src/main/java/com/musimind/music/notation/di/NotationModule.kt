package com.musimind.music.notation.di

import android.content.Context
import com.musimind.music.notation.engine.NotationEngine
import com.musimind.music.notation.layout.ScoreLayoutEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for music notation dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NotationModule {
    
    @Provides
    @Singleton
    fun provideNotationEngine(
        @ApplicationContext context: Context
    ): NotationEngine {
        return NotationEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideScoreLayoutEngine(): ScoreLayoutEngine {
        return ScoreLayoutEngine()
    }
}
