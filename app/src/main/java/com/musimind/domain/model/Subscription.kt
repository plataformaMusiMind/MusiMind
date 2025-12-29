package com.musimind.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subscription tier enum with progressive access levels
 * Philosophy: Everything unlocked, but with progressive limits
 * 
 * FREEMIUM: 20% of everything - "taste of all"
 * SPALLA: 70% of everything - solid experience
 * MAESTRO: 100% - full access, no limits
 */
enum class SubscriptionTier(
    val level: Int,
    val displayName: String,
    val percentage: Int
) {
    FREEMIUM(0, "Gratuito", 20),
    SPALLA(1, "Spalla", 70),
    MAESTRO(2, "Maestro", 100);

    fun hasAccessTo(requiredTier: SubscriptionTier): Boolean {
        return this.level >= requiredTier.level
    }
    
    fun getUpgradeTarget(): SubscriptionTier? {
        return when (this) {
            FREEMIUM -> SPALLA
            SPALLA -> MAESTRO
            MAESTRO -> null
        }
    }
    
    companion object {
        fun fromString(value: String?): SubscriptionTier {
            return when (value?.lowercase()) {
                "spalla" -> SPALLA
                "maestro" -> MAESTRO
                else -> FREEMIUM
            }
        }
    }
}

/**
 * Subscription status
 */
enum class SubscriptionStatus {
    NONE,
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    PAUSED;
    
    fun isActive(): Boolean = this == ACTIVE || this == TRIALING
    
    companion object {
        fun fromString(value: String?): SubscriptionStatus {
            return when (value?.lowercase()) {
                "trialing" -> TRIALING
                "active" -> ACTIVE
                "past_due" -> PAST_DUE
                "canceled" -> CANCELED
                "paused" -> PAUSED
                else -> NONE
            }
        }
    }
}

/**
 * Complete subscription state
 */
data class SubscriptionState(
    val tier: SubscriptionTier = SubscriptionTier.FREEMIUM,
    val status: SubscriptionStatus = SubscriptionStatus.NONE,
    val periodEnd: String? = null,
    val trialEndsAt: String? = null,
    val currency: String = "BRL",
    val stripeCustomerId: String? = null,
    val stripeSubscriptionId: String? = null
) {
    val isActive: Boolean 
        get() = status.isActive()
    
    val effectiveTier: SubscriptionTier
        get() = if (isActive) tier else SubscriptionTier.FREEMIUM
    
    val isInTrial: Boolean
        get() = status == SubscriptionStatus.TRIALING
    
    val isPastDue: Boolean
        get() = status == SubscriptionStatus.PAST_DUE
}

/**
 * Result of checking feature access
 */
@Serializable
data class FeatureAccessResult(
    val allowed: Boolean = false,
    val tier: String = "freemium",
    val percentage: Int = 20,
    
    @SerialName("daily_limit")
    val dailyLimit: Int? = null,
    
    @SerialName("daily_used")
    val dailyUsed: Int = 0,
    
    @SerialName("daily_remaining")
    val dailyRemaining: Int? = null,
    
    @SerialName("monthly_limit")
    val monthlyLimit: Int? = null,
    
    @SerialName("monthly_used")
    val monthlyUsed: Int = 0,
    
    @SerialName("monthly_remaining")
    val monthlyRemaining: Int? = null,
    
    val reason: String? = null,
    
    @SerialName("upgrade_to")
    val upgradeTo: String? = null
) {
    val isLimitReached: Boolean 
        get() = !allowed && (reason == "daily_limit_reached" || reason == "monthly_limit_reached")
    
    val upgradeTarget: SubscriptionTier?
        get() = when (upgradeTo) {
            "spalla" -> SubscriptionTier.SPALLA
            "maestro" -> SubscriptionTier.MAESTRO
            else -> null
        }
}

/**
 * Feature limit configuration
 */
@Serializable
data class FeatureLimit(
    val id: String = "",
    
    @SerialName("feature_key")
    val featureKey: String = "",
    
    @SerialName("feature_name")
    val featureName: String = "",
    
    val category: String = "",
    val description: String? = null,
    
    @SerialName("freemium_daily_limit")
    val freemiumDailyLimit: Int? = null,
    
    @SerialName("freemium_percentage")
    val freemiumPercentage: Int = 20,
    
    @SerialName("spalla_daily_limit")
    val spallaDailyLimit: Int? = null,
    
    @SerialName("spalla_percentage")
    val spallaPercentage: Int = 70,
    
    @SerialName("maestro_daily_limit")
    val maestroDailyLimit: Int? = null,
    
    @SerialName("maestro_percentage")
    val maestroPercentage: Int = 100,
    
    @SerialName("show_upgrade_prompt")
    val showUpgradePrompt: Boolean = true,
    
    @SerialName("premium_badge")
    val premiumBadge: String? = null
)

/**
 * Tier-specific numeric limits
 */
@Serializable
data class TierLimit(
    val id: String = "",
    
    @SerialName("limit_key")
    val limitKey: String = "",
    
    @SerialName("limit_name")
    val limitName: String = "",
    
    val description: String? = null,
    
    @SerialName("freemium_value")
    val freemiumValue: Int = 0,
    
    @SerialName("spalla_value")
    val spallaValue: Int = 0,
    
    @SerialName("maestro_value")
    val maestroValue: Int = 999999
)

/**
 * User subscription data from database
 */
@Serializable
data class UserSubscription(
    @SerialName("auth_id")
    val authId: String = "",
    
    @SerialName("subscription_tier")
    val subscriptionTier: String? = "freemium",
    
    @SerialName("subscription_status")
    val subscriptionStatus: String? = "none",
    
    @SerialName("subscription_period_end")
    val subscriptionPeriodEnd: String? = null,
    
    @SerialName("trial_ends_at")
    val trialEndsAt: String? = null,
    
    @SerialName("subscription_currency")
    val subscriptionCurrency: String? = "BRL",
    
    @SerialName("stripe_customer_id")
    val stripeCustomerId: String? = null,
    
    @SerialName("stripe_subscription_id")
    val stripeSubscriptionId: String? = null
) {
    fun toSubscriptionState(): SubscriptionState {
        return SubscriptionState(
            tier = SubscriptionTier.fromString(subscriptionTier),
            status = SubscriptionStatus.fromString(subscriptionStatus),
            periodEnd = subscriptionPeriodEnd,
            trialEndsAt = trialEndsAt,
            currency = subscriptionCurrency ?: "BRL",
            stripeCustomerId = stripeCustomerId,
            stripeSubscriptionId = stripeSubscriptionId
        )
    }
}

/**
 * Stripe price IDs for each plan
 */
object StripePrices {
    // Spalla prices
    const val SPALLA_BRL_MONTHLY = "price_spalla_brl_monthly"
    const val SPALLA_BRL_YEARLY = "price_spalla_brl_yearly"
    const val SPALLA_USD_MONTHLY = "price_spalla_usd_monthly"
    const val SPALLA_USD_YEARLY = "price_spalla_usd_yearly"
    
    // Maestro prices
    const val MAESTRO_BRL_MONTHLY = "price_maestro_brl_monthly"
    const val MAESTRO_BRL_YEARLY = "price_maestro_brl_yearly"
    const val MAESTRO_USD_MONTHLY = "price_maestro_usd_monthly"
    const val MAESTRO_USD_YEARLY = "price_maestro_usd_yearly"
    
    fun getPriceId(tier: SubscriptionTier, currency: String, yearly: Boolean): String {
        val tierPrefix = when (tier) {
            SubscriptionTier.SPALLA -> "spalla"
            SubscriptionTier.MAESTRO -> "maestro"
            else -> throw IllegalArgumentException("Invalid tier")
        }
        val currencyPart = currency.lowercase()
        val period = if (yearly) "yearly" else "monthly"
        return "price_${tierPrefix}_${currencyPart}_${period}"
    }
}

/**
 * Prices for display
 */
data class SubscriptionPricing(
    val tier: SubscriptionTier,
    val monthlyPrice: Double,
    val yearlyPrice: Double,
    val currency: String,
    val yearlyDiscount: Int = 17 // 17% off yearly
) {
    val formattedMonthly: String
        get() = when (currency) {
            "BRL" -> "R$ %.2f".format(monthlyPrice)
            "USD" -> "$ %.2f".format(monthlyPrice)
            else -> "$currency %.2f".format(monthlyPrice)
        }
    
    val formattedYearly: String
        get() = when (currency) {
            "BRL" -> "R$ %.2f".format(yearlyPrice)
            "USD" -> "$ %.2f".format(yearlyPrice)
            else -> "$currency %.2f".format(yearlyPrice)
        }
    
    val formattedMonthlyInYearly: String
        get() {
            val monthly = yearlyPrice / 12
            return when (currency) {
                "BRL" -> "R$ %.2f/mês".format(monthly)
                "USD" -> "$ %.2f/mo".format(monthly)
                else -> "$currency %.2f/mo".format(monthly)
            }
        }
    
    companion object {
        val SPALLA_BRL = SubscriptionPricing(
            tier = SubscriptionTier.SPALLA,
            monthlyPrice = 19.90,
            yearlyPrice = 199.00,
            currency = "BRL"
        )
        
        val SPALLA_USD = SubscriptionPricing(
            tier = SubscriptionTier.SPALLA,
            monthlyPrice = 4.99,
            yearlyPrice = 49.90,
            currency = "USD"
        )
        
        val MAESTRO_BRL = SubscriptionPricing(
            tier = SubscriptionTier.MAESTRO,
            monthlyPrice = 29.90,
            yearlyPrice = 299.00,
            currency = "BRL"
        )
        
        val MAESTRO_USD = SubscriptionPricing(
            tier = SubscriptionTier.MAESTRO,
            monthlyPrice = 9.90,
            yearlyPrice = 99.00,
            currency = "USD"
        )
        
        fun forTier(tier: SubscriptionTier, currency: String): SubscriptionPricing {
            return when {
                tier == SubscriptionTier.SPALLA && currency == "BRL" -> SPALLA_BRL
                tier == SubscriptionTier.SPALLA && currency == "USD" -> SPALLA_USD
                tier == SubscriptionTier.MAESTRO && currency == "BRL" -> MAESTRO_BRL
                tier == SubscriptionTier.MAESTRO && currency == "USD" -> MAESTRO_USD
                else -> SPALLA_BRL
            }
        }
    }
}

/**
 * Common feature keys for type safety
 */
object FeatureKey {
    // Exercises
    const val SOLFEGE_EXERCISES = "solfege_exercises"
    const val RHYTHM_EXERCISES = "rhythm_exercises"
    const val INTERVAL_EXERCISES = "interval_exercises"
    const val CHORD_EXERCISES = "chord_exercises"
    const val MELODY_EXERCISES = "melody_exercises"
    const val HARMONY_EXERCISES = "harmony_exercises"
    const val THEORY_LESSONS = "theory_lessons"
    
    // Games
    const val NOTE_CATCHER = "note_catcher"
    const val RHYTHM_TAP = "rhythm_tap"
    const val SOLFEGE_SING = "solfege_sing"
    const val INTERVAL_HERO = "interval_hero"
    const val CHORD_BUILDER = "chord_builder"
    const val MELODY_MEMORY = "melody_memory"
    const val SCALE_PUZZLE = "scale_puzzle"
    const val KEY_SHOOTER = "key_shooter"
    const val TEMPO_RUN = "tempo_run"
    const val PROGRESSION_QUEST = "progression_quest"
    const val DAILY_CHALLENGE = "daily_challenge"
    
    // Social
    const val QUIZ_MULTIPLAYER = "quiz_multiplayer"
    const val DUELS = "duels"
    const val FRIENDS = "friends"
    const val LEADERBOARD = "leaderboard"
    const val ACTIVITY_FEED = "activity_feed"
    
    // Teacher
    const val TEACHER_STUDENTS = "teacher_students"
    const val TEACHER_CLASSES = "teacher_classes"
    const val CUSTOM_ASSESSMENTS = "custom_assessments"
    const val STUDENT_REPORTS = "student_reports"
    const val SCHOOL_TEACHERS = "school_teachers"
    
    // Analytics
    const val PERFORMANCE_HISTORY = "performance_history"
    const val DETAILED_FEEDBACK = "detailed_feedback"
    const val PROGRESS_CHARTS = "progress_charts"
    const val AI_RECOMMENDATIONS = "ai_recommendations"
    const val WEAK_POINT_ANALYSIS = "weak_point_analysis"
    
    // Premium
    const val OFFLINE_MODE = "offline_mode"
    const val AD_FREE = "ad_free"
    const val MULTI_DEVICE_SYNC = "multi_device_sync"
    const val EXPORT_DATA = "export_data"
    const val PRIORITY_SUPPORT = "priority_support"
}

/**
 * Common limit keys for type safety
 */
object LimitKey {
    const val MAX_STUDENTS = "max_students"
    const val MAX_CLASSES = "max_classes"
    const val MAX_ASSESSMENTS = "max_assessments"
    const val MAX_ASSESSMENT_QUESTIONS = "max_assessment_questions"
    const val MAX_SCHOOL_TEACHERS = "max_school_teachers"
    const val MAX_SCHOOL_STUDENTS = "max_school_students"
    const val MAX_FRIENDS = "max_friends"
    const val MAX_DAILY_DUELS = "max_daily_duels"
    const val MAX_DAILY_QUIZ = "max_daily_quiz"
    const val HISTORY_DAYS = "history_days"
    const val CHART_PERIOD_DAYS = "chart_period_days"
    const val UNLOCKED_LEVELS_PERCENT = "unlocked_levels_percent"
    const val EXERCISES_PER_CATEGORY = "exercises_per_category"
}
