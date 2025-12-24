package com.musimind.data.repository

import com.musimind.domain.model.User
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository using Supabase Postgrest
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) : UserRepository {
    
    private val usersTable = "users"
    
    override suspend fun getCurrentUser(): User? {
        val authId = auth.currentSessionOrNull()?.user?.id ?: return null
        return getUserByAuthId(authId)
    }
    
    private suspend fun getUserByAuthId(authId: String): User? {
        return try {
            postgrest.from(usersTable)
                .select {
                    filter {
                        eq("auth_id", authId)
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            // Get the auth user id
            val authId = auth.currentSessionOrNull()?.user?.id
            
            // Create user with auth_id linked
            val userWithAuthId = if (authId != null && user.authId == null) {
                user.copy(authId = authId)
            } else {
                user
            }
            
            postgrest.from(usersTable).upsert(userWithAuthId) {
                // Use auth_id as the conflict target
                onConflict = "auth_id"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserById(userId: String): User? {
        return try {
            // First try to get by id
            val result = postgrest.from(usersTable)
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()
            
            // If not found, try by auth_id
            if (result == null) {
                postgrest.from(usersTable)
                    .select {
                        filter {
                            eq("auth_id", userId)
                        }
                    }
                    .decodeSingleOrNull<User>()
            } else {
                result
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun observeCurrentUser(): Flow<User?> = flow {
        val authId = auth.currentSessionOrNull()?.user?.id
        
        if (authId == null) {
            emit(null)
            return@flow
        }
        
        // For now, just emit current user once
        // TODO: Implement Supabase Realtime subscription for live updates
        val user = getUserByAuthId(authId)
        emit(user)
    }
    
    override suspend fun userExists(userId: String): Boolean {
        return try {
            // Check by auth_id first
            val result = postgrest.from(usersTable)
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("auth_id", userId)
                    }
                }
                .decodeSingleOrNull<Map<String, String>>()
            
            if (result != null) {
                true
            } else {
                // Also check by id
                val resultById = postgrest.from(usersTable)
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<Map<String, String>>()
                resultById != null
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun updateUserFields(userId: String, fields: Map<String, Any?>): Result<Unit> {
        return try {
            // Try update by auth_id first
            postgrest.from(usersTable).update(fields) {
                filter {
                    eq("auth_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // If that fails, try by id
            try {
                postgrest.from(usersTable).update(fields) {
                    filter {
                        eq("id", userId)
                    }
                }
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }
    
    override suspend fun updateUserField(userId: String, field: String, value: Any?): Result<Unit> {
        return updateUserFields(userId, mapOf(field to value))
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            postgrest.from(usersTable).delete {
                filter {
                    eq("auth_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
