package com.musimind.di

import com.musimind.domain.games.GamesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Módulo Hilt para injeção de dependências dos jogos
 */
@Module
@InstallIn(SingletonComponent::class)
object GamesModule {
    
    @Provides
    @Singleton
    fun provideGamesRepository(
        postgrest: Postgrest
    ): GamesRepository {
        return GamesRepository(postgrest)
    }
}
