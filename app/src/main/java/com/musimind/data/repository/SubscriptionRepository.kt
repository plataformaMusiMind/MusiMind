package com.musimind.data.repository

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for subscription management and feature gating
 * 
 * Philosophy: "Taste of Everything"
 * - FREEMIUM: 20% of all features - limited but functional
 * - SPALLA: 70% - solid experience with most features
 * - MAESTRO: 100% - unlimited, full access
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    private val currentUserId: String? 
        get() = auth.currentSessionOrNull()?.user?.id
    
    // Cached subscription state
    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    val subscriptionState: Flow<SubscriptionState> = _subscriptionState.asStateFlow()
    
    // Cached feature limits
    private var featureLimitsCache: Map<String, FeatureLimit> = emptyMap()
    private var tierLimitsCache: Map<String, TierLimit> = emptyMap()
    
    /**
     * Get current subscription state from cache
     */
    fun getCachedState(): SubscriptionState = _subscriptionState.value
    
    /**
     * Get effective tier (considering if subscription is active)
     */
    fun getEffectiveTier(): SubscriptionTier = _subscriptionState.value.effectiveTier
    
    /**
     * Refresh subscription state from server
     */
    suspend fun refreshSubscriptionState(): Result<SubscriptionState> {
        val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val userSub = postgrest.from("users")
                .select {
                    filter { eq("auth_id", userId) }
                }
                .decodeSingleOrNull<UserSubscription>()
            
            val state = userSub?.toSubscriptionState() ?: SubscriptionState()
            _subscriptionState.value = state
            
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user can access a specific feature
     * Uses server-side function for accurate limits
     */
    suspend fun checkFeatureAccess(featureKey: String): FeatureAccessResult {
        val userId = currentUserId ?: return FeatureAccessResult(
            allowed = true, // Allow if not authenticated (will be limited by UI)
            tier = "freemium",
            percentage = 20
        )
        
        return try {
            val result = postgrest.rpc(
                "check_feature_access",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_feature_key", featureKey)
                }
            ).decodeAs<FeatureAccessResult>()
            
            result
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionRepo", "Error checking feature access: ${e.message}")
            // On error, allow with freemium limits
            FeatureAccessResult(
                allowed = true,
                tier = _subscriptionState.value.effectiveTier.name.lowercase(),
                percentage = _subscriptionState.value.effectiveTier.percentage
            )
        }
    }
    
    /**
     * Quick check using cached tier (for UI, not security)
     */
    fun quickCheckAccess(featureKey: String): Boolean {
        // For quick UI checks, always allow (server validates on actual use)
        return true
    }
    
    /**
     * Get percentage of content unlocked for a feature
     */
    fun getUnlockedPercentage(featureKey: String): Int {
        val tier = _subscriptionState.value.effectiveTier
        val feature = featureLimitsCache[featureKey]
        
        return when (tier) {
            SubscriptionTier.FREEMIUM -> feature?.freemiumPercentage ?: 20
            SubscriptionTier.SPALLA -> feature?.spallaPercentage ?: 70
            SubscriptionTier.MAESTRO -> feature?.maestroPercentage ?: 100
        }
    }
    
    /**
     * Increment usage counter for a feature
     */
    suspend fun incrementUsage(featureKey: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            postgrest.rpc(
                "increment_feature_usage",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_feature_key", featureKey)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get numeric limit for current tier
     */
    suspend fun getTierLimit(limitKey: String): Int {
        val userId = currentUserId ?: return getDefaultLimit(limitKey)
        
        return try {
            val result = postgrest.rpc(
                "get_tier_limit",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_limit_key", limitKey)
                }
            ).decodeAs<Int>()
            
            result
        } catch (e: Exception) {
            getDefaultLimit(limitKey)
        }
    }
    
    /**
     * Get default limit based on cached tier
     */
    private fun getDefaultLimit(limitKey: String): Int {
        val tier = _subscriptionState.value.effectiveTier
        val limit = tierLimitsCache[limitKey]
        
        return when (tier) {
            SubscriptionTier.FREEMIUM -> limit?.freemiumValue ?: 3
            SubscriptionTier.SPALLA -> limit?.spallaValue ?: 30
            SubscriptionTier.MAESTRO -> limit?.maestroValue ?: 999999
        }
    }
    
    /**
     * Load and cache all feature limits
     */
    suspend fun loadFeatureLimits(): Result<List<FeatureLimit>> {
        return try {
            val limits = postgrest.from("feature_limits")
                .select()
                .decodeList<FeatureLimit>()
            
            featureLimitsCache = limits.associateBy { it.featureKey }
            Result.success(limits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load and cache all tier limits
     */
    suspend fun loadTierLimits(): Result<List<TierLimit>> {
        return try {
            val limits = postgrest.from("tier_limits")
                .select()
                .decodeList<TierLimit>()
            
            tierLimitsCache = limits.associateBy { it.limitKey }
            Result.success(limits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all feature limits for a category
     */
    fun getFeaturesByCategory(category: String): List<FeatureLimit> {
        return featureLimitsCache.values.filter { it.category == category }
    }
    
    /**
     * Check if user has reached their teacher student limit
     */
    suspend fun canAddStudent(): Result<Boolean> {
        val currentCount = getStudentCount()
        val limit = getTierLimit(LimitKey.MAX_STUDENTS)
        return Result.success(currentCount < limit)
    }
    
    /**
     * Check if school can add more teachers
     */
    suspend fun canAddTeacher(): Result<Boolean> {
        val currentCount = getSchoolTeacherCount()
        val limit = getTierLimit(LimitKey.MAX_SCHOOL_TEACHERS)
        return Result.success(currentCount < limit)
    }
    
    /**
     * Get current student count for teacher
     */
    private suspend fun getStudentCount(): Int {
        val userId = currentUserId ?: return 0
        
        return try {
            // Count from class_students where teacher is current user
            val result = postgrest.from("class_students")
                .select {
                    filter { eq("teacher_id", userId) }
                }
                .decodeList<JsonObject>()
            
            result.size
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get current teacher count for school
     */
    private suspend fun getSchoolTeacherCount(): Int {
        val userId = currentUserId ?: return 0
        
        return try {
            // Get school owned by user, then count teachers
            val school = postgrest.from("schools")
                .select {
                    filter { eq("owner_id", userId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (school == null) return 0
            
            val schoolId = school["id"]?.toString()?.replace("\"", "") ?: return 0
            
            val teachers = postgrest.from("school_teachers")
                .select {
                    filter { eq("school_id", schoolId) }
                }
                .decodeList<JsonObject>()
            
            teachers.size
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get remaining daily uses for a feature
     */
    suspend fun getRemainingDailyUses(featureKey: String): Int? {
        val access = checkFeatureAccess(featureKey)
        return access.dailyRemaining
    }
    
    /**
     * Check if user is in grace period (past due but still active)
     */
    fun isInGracePeriod(): Boolean {
        return _subscriptionState.value.status == SubscriptionStatus.PAST_DUE
    }
    
    /**
     * Get days remaining in trial
     */
    fun getTrialDaysRemaining(): Int? {
        val state = _subscriptionState.value
        if (state.status != SubscriptionStatus.TRIALING) return null
        
        val trialEnd = state.trialEndsAt ?: return null
        
        return try {
            val endDate = java.time.Instant.parse(trialEnd)
            val now = java.time.Instant.now()
            val days = java.time.Duration.between(now, endDate).toDays()
            days.toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create checkout session URL for upgrading
     */
    suspend fun createCheckoutSession(
        priceId: String,
        successUrl: String = "musimind://subscription/success",
        cancelUrl: String = "musimind://subscription/cancel"
    ): Result<String> {
        val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val response = postgrest.rpc(
                "create_checkout_session", // Edge function
                buildJsonObject {
                    put("user_id", userId)
                    put("price_id", priceId)
                    put("success_url", successUrl)
                    put("cancel_url", cancelUrl)
                }
            ).decodeAs<CheckoutResponse>()
            
            Result.success(response.url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel current subscription
     */
    suspend fun cancelSubscription(): Result<Unit> {
        val subscriptionId = _subscriptionState.value.stripeSubscriptionId
            ?: return Result.failure(Exception("No active subscription"))
        
        return try {
            postgrest.rpc(
                "cancel_subscription", // Edge function
                buildJsonObject {
                    put("subscription_id", subscriptionId)
                }
            )
            
            // Refresh state
            refreshSubscriptionState()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Resume paused subscription
     */
    suspend fun resumeSubscription(): Result<Unit> {
        val subscriptionId = _subscriptionState.value.stripeSubscriptionId
            ?: return Result.failure(Exception("No subscription to resume"))
        
        return try {
            postgrest.rpc(
                "resume_subscription", // Edge function
                buildJsonObject {
                    put("subscription_id", subscriptionId)
                }
            )
            
            refreshSubscriptionState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initialize repository on app start
     */
    suspend fun initialize() {
        refreshSubscriptionState()
        loadFeatureLimits()
        loadTierLimits()
    }
}

@kotlinx.serialization.Serializable
private data class CheckoutResponse(
    val url: String
)
