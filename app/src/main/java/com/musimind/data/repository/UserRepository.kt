package com.musimind.data.repository

import com.musimind.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user data operations
 */
interface UserRepository {
    /**
     * Get currently authenticated user from Supabase
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Create or update user in Supabase
     */
    suspend fun createOrUpdateUser(user: User): Result<Unit>
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): User?
    
    /**
     * Observe current user changes
     */
    fun observeCurrentUser(): Flow<User?>
    
    /**
     * Check if user exists in Supabase
     */
    suspend fun userExists(userId: String): Boolean
    
    /**
     * Update user fields
     */
    suspend fun updateUserFields(userId: String, fields: Map<String, Any?>): Result<Unit>
    
    /**
     * Update a single user field
     */
    suspend fun updateUserField(userId: String, field: String, value: Any?): Result<Unit>
    
    /**
     * Delete user
     */
    suspend fun deleteUser(userId: String): Result<Unit>
}
