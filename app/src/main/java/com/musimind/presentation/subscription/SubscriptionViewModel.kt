package com.musimind.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.SubscriptionRepository
import com.musimind.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for subscription management and paywall
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()
    
    init {
        loadSubscriptionState()
    }
    
    /**
     * Load current subscription state
     */
    fun loadSubscriptionState() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = repository.refreshSubscriptionState()
            
            result.onSuccess { subscription ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        subscriptionState = subscription,
                        currentTier = subscription.effectiveTier,
                        trialDaysRemaining = repository.getTrialDaysRemaining()
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Check access to a specific feature
     */
    suspend fun checkFeatureAccess(featureKey: String): FeatureAccessResult {
        return repository.checkFeatureAccess(featureKey)
    }
    
    /**
     * Record usage of a feature
     */
    fun recordUsage(featureKey: String) {
        viewModelScope.launch {
            repository.incrementUsage(featureKey)
        }
    }
    
    /**
     * Get tier limit value
     */
    suspend fun getTierLimit(limitKey: String): Int {
        return repository.getTierLimit(limitKey)
    }
    
    /**
     * Get unlock percentage for a feature category
     */
    fun getUnlockPercentage(): Int {
        return _state.value.currentTier.percentage
    }
    
    /**
     * Select a plan for checkout
     */
    fun selectPlan(tier: SubscriptionTier, yearly: Boolean) {
        _state.update { 
            it.copy(
                selectedTier = tier,
                isYearly = yearly
            )
        }
    }
    
    /**
     * Get pricing for display
     */
    fun getPricing(tier: SubscriptionTier, currency: String = "BRL"): SubscriptionPricing {
        return SubscriptionPricing.forTier(tier, currency)
    }
    
    /**
     * Start checkout process
     */
    fun startCheckout(tier: SubscriptionTier, yearly: Boolean, currency: String = "BRL") {
        viewModelScope.launch {
            _state.update { it.copy(isCheckoutLoading = true) }
            
            val priceId = StripePrices.getPriceId(tier, currency, yearly)
            
            val result = repository.createCheckoutSession(priceId)
            
            result.onSuccess { url ->
                _state.update { 
                    it.copy(
                        isCheckoutLoading = false,
                        checkoutUrl = url
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isCheckoutLoading = false,
                        error = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Clear checkout URL after navigation
     */
    fun clearCheckoutUrl() {
        _state.update { it.copy(checkoutUrl = null) }
    }
    
    /**
     * Cancel subscription
     */
    fun cancelSubscription() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = repository.cancelSubscription()
            
            result.onSuccess {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        message = "Assinatura cancelada. Você manterá acesso até o fim do período."
                    )
                }
                loadSubscriptionState()
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Resume subscription
     */
    fun resumeSubscription() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = repository.resumeSubscription()
            
            result.onSuccess {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        message = "Assinatura reativada com sucesso!"
                    )
                }
                loadSubscriptionState()
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Clear error/message
     */
    fun clearMessages() {
        _state.update { it.copy(error = null, message = null) }
    }
    
    /**
     * Check if can add student (for teachers)
     */
    suspend fun canAddStudent(): Boolean {
        return repository.canAddStudent().getOrDefault(false)
    }
    
    /**
     * Check if can add teacher (for schools)
     */
    suspend fun canAddTeacher(): Boolean {
        return repository.canAddTeacher().getOrDefault(false)
    }
}

/**
 * UI State for subscription screen
 */
data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isCheckoutLoading: Boolean = false,
    val subscriptionState: SubscriptionState = SubscriptionState(),
    val currentTier: SubscriptionTier = SubscriptionTier.FREEMIUM,
    val selectedTier: SubscriptionTier? = null,
    val isYearly: Boolean = true,
    val trialDaysRemaining: Int? = null,
    val checkoutUrl: String? = null,
    val error: String? = null,
    val message: String? = null
) {
    val isActive: Boolean get() = subscriptionState.isActive
    val isInTrial: Boolean get() = subscriptionState.isInTrial
    val isPastDue: Boolean get() = subscriptionState.isPastDue
}
