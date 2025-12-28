package com.musimind.di

import android.content.Context
import com.musimind.domain.gamification.LivesManager
import com.musimind.domain.gamification.ProgressionManager
import com.musimind.domain.gamification.RewardsManager
import com.musimind.domain.notification.MusiMindNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Hilt Module for Gamification Dependencies
 * 
 * Provides:
 * - LivesManager
 * - ProgressionManager
 * - RewardsManager
 * - NotificationManager
 */

@Module
@InstallIn(SingletonComponent::class)
object GamificationModule {
    
    @Provides
    @Singleton
    fun provideLivesManager(
        postgrest: Postgrest,
        auth: Auth
    ): LivesManager {
        return LivesManager(postgrest, auth)
    }
    
    @Provides
    @Singleton
    fun provideProgressionManager(
        postgrest: Postgrest,
        auth: Auth
    ): ProgressionManager {
        return ProgressionManager(postgrest, auth)
    }
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        postgrest: Postgrest,
        auth: Auth
    ): MusiMindNotificationManager {
        return MusiMindNotificationManager(context, postgrest, auth)
    }
    
    @Provides
    @Singleton
    fun provideRewardsManager(
        postgrest: Postgrest,
        auth: Auth,
        livesManager: LivesManager,
        progressionManager: ProgressionManager,
        notificationManager: MusiMindNotificationManager
    ): RewardsManager {
        return RewardsManager(
            postgrest,
            auth,
            livesManager,
            progressionManager,
            notificationManager
        )
    }
}
