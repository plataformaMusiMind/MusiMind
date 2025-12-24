package com.musimind.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

/**
 * Enum representing onboarding steps
 * User must complete all steps in order before accessing the main app
 */
enum class OnboardingStep(val order: Int) {
    NOT_STARTED(0),
    USER_TYPE(1),      // Select if Student, Teacher, or School
    PLAN_SELECTION(2), // Select subscription plan
    AVATAR(3),         // Select avatar
    TUTORIAL(4),       // App tutorial/walkthrough
    COMPLETED(5)       // All done, can access main app
}

/**
 * Manager for tracking onboarding progress using DataStore
 * This provides local cache so users can resume from where they stopped
 */
@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.onboardingDataStore
    
    companion object {
        private val KEY_CURRENT_STEP = intPreferencesKey("current_onboarding_step")
        private val KEY_USER_ID = stringPreferencesKey("onboarding_user_id")
        private val KEY_USER_TYPE = stringPreferencesKey("selected_user_type")
        private val KEY_PLAN = stringPreferencesKey("selected_plan")
        private val KEY_AVATAR = stringPreferencesKey("selected_avatar")
    }
    
    /**
     * Get current onboarding step
     */
    val currentStep: Flow<OnboardingStep> = dataStore.data.map { prefs ->
        val stepOrder = prefs[KEY_CURRENT_STEP] ?: 0
        OnboardingStep.entries.find { it.order == stepOrder } ?: OnboardingStep.NOT_STARTED
    }
    
    /**
     * Get saved user ID for this onboarding session
     */
    val savedUserId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }
    
    /**
     * Get selected user type
     */
    val selectedUserType: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_TYPE]
    }
    
    /**
     * Get selected plan
     */
    val selectedPlan: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_PLAN]
    }
    
    /**
     * Get selected avatar
     */
    val selectedAvatar: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_AVATAR]
    }
    
    /**
     * Start onboarding for a user
     */
    suspend fun startOnboarding(userId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_CURRENT_STEP] = OnboardingStep.USER_TYPE.order
        }
    }
    
    /**
     * Save user type selection and advance to next step
     */
    suspend fun saveUserType(userType: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_TYPE] = userType
            prefs[KEY_CURRENT_STEP] = OnboardingStep.PLAN_SELECTION.order
        }
    }
    
    /**
     * Save plan selection and advance to next step
     */
    suspend fun savePlan(plan: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PLAN] = plan
            prefs[KEY_CURRENT_STEP] = OnboardingStep.AVATAR.order
        }
    }
    
    /**
     * Save avatar selection and advance to next step
     */
    suspend fun saveAvatar(avatarUrl: String) {
        dataStore.edit { prefs ->
            prefs[KEY_AVATAR] = avatarUrl
            prefs[KEY_CURRENT_STEP] = OnboardingStep.TUTORIAL.order
        }
    }
    
    /**
     * Complete the tutorial and mark onboarding as finished
     */
    suspend fun completeTutorial() {
        dataStore.edit { prefs ->
            prefs[KEY_CURRENT_STEP] = OnboardingStep.COMPLETED.order
        }
    }
    
    /**
     * Mark onboarding as complete
     */
    suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[KEY_CURRENT_STEP] = OnboardingStep.COMPLETED.order
        }
    }
    
    /**
     * Reset onboarding (for testing or when user logs out)
     */
    suspend fun resetOnboarding() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
    
    /**
     * Check if onboarding is complete
     */
    suspend fun isOnboardingComplete(): Boolean {
        var isComplete = false
        dataStore.data.collect { prefs ->
            val stepOrder = prefs[KEY_CURRENT_STEP] ?: 0
            isComplete = stepOrder >= OnboardingStep.COMPLETED.order
        }
        return isComplete
    }
    
    /**
     * Get the next screen route based on current step
     */
    fun getNextScreenRoute(step: OnboardingStep): String {
        return when (step) {
            OnboardingStep.NOT_STARTED -> "login"
            OnboardingStep.USER_TYPE -> "user_type"
            OnboardingStep.PLAN_SELECTION -> "plan_selection"
            OnboardingStep.AVATAR -> "avatar_selection"
            OnboardingStep.TUTORIAL -> "onboarding_tutorial"
            OnboardingStep.COMPLETED -> "home"
        }
    }
}
