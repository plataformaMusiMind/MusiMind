package com.musimind.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.musimind.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository using Firebase Firestore
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {
    
    private val usersCollection = firestore.collection("users")
    
    override suspend fun getCurrentUser(): User? {
        val userId = firebaseAuth.currentUser?.uid ?: return null
        return getUserById(userId)
    }
    
    override suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id)
                .set(user, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserById(userId: String): User? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        
        if (userId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun userExists(userId: String): Boolean {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun updateUserFields(userId: String, fields: Map<String, Any?>): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update(fields)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
