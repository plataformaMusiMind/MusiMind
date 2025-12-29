package com.musimind.di

import android.content.Context
import com.musimind.domain.locale.LocaleManager
import com.musimind.domain.locale.MusicTerminologyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for locale/i18n dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object LocaleModule {
    
    @Provides
    @Singleton
    fun provideLocaleManager(
        @ApplicationContext context: Context
    ): LocaleManager {
        return LocaleManager(context)
    }
    
    @Provides
    @Singleton
    fun provideMusicTerminologyProvider(
        localeManager: LocaleManager
    ): MusicTerminologyProvider {
        return MusicTerminologyProvider(localeManager)
    }
}
