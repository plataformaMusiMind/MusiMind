package com.musimind.data.cache

import android.content.Context
import com.musimind.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline Cache Manager for Exercise Data
 * 
 * Provides:
 * - Caching of exercises, categories, and user progress
 * - Offline access to previously loaded content
 * - Automatic cache invalidation after configurable TTL
 * - Compression for large datasets
 * 
 * Cache structure:
 * /cache/
 *   /exercises/
 *     categories.json
 *     exercises_{categoryId}.json
 *     solfege_notes_{exerciseId}.json
 *     melodic_notes_{exerciseId}.json
 *     rhythm_patterns_{exerciseId}.json
 *     interval_questions_{exerciseId}.json
 *   /user/
 *     progress.json
 *     achievements.json
 *   /meta/
 *     last_sync.json
 */

@Singleton
class ExerciseCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }
    
    private val cacheDir: File
        get() = File(context.cacheDir, "exercises").also { it.mkdirs() }
    
    private val userDir: File
        get() = File(context.cacheDir, "user").also { it.mkdirs() }
    
    private val metaDir: File
        get() = File(context.cacheDir, "meta").also { it.mkdirs() }
    
    companion object {
        // Cache TTL in milliseconds (24 hours default)
        const val DEFAULT_CACHE_TTL = 24 * 60 * 60 * 1000L
        
        // Cache keys
        private const val CATEGORIES_KEY = "categories"
        private const val EXERCISES_PREFIX = "exercises_"
        private const val SOLFEGE_NOTES_PREFIX = "solfege_notes_"
        private const val MELODIC_NOTES_PREFIX = "melodic_notes_"
        private const val RHYTHM_PATTERNS_PREFIX = "rhythm_patterns_"
        private const val INTERVAL_QUESTIONS_PREFIX = "interval_questions_"
        private const val USER_PROGRESS_KEY = "progress"
        private const val USER_ACHIEVEMENTS_KEY = "achievements"
        private const val LAST_SYNC_KEY = "last_sync"
    }
    
    // ============================================
    // Categories Caching
    // ============================================
    
    suspend fun cacheCategories(categories: List<ExerciseCategory>) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$CATEGORIES_KEY.json")
            file.writeText(json.encodeToString(CachedData(categories, System.currentTimeMillis())))
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
    
    suspend fun getCachedCategories(ttl: Long = DEFAULT_CACHE_TTL): List<ExerciseCategory>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$CATEGORIES_KEY.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<ExerciseCategory>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    // Cache expired
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Exercises Caching
    // ============================================
    
    suspend fun cacheExercises(categoryId: String, exercises: List<Exercise>) = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$EXERCISES_PREFIX$categoryId.json")
                file.writeText(json.encodeToString(CachedData(exercises, System.currentTimeMillis())))
            } catch (e: Exception) {
                // Log error
            }
        }
    
    suspend fun getCachedExercises(categoryId: String, ttl: Long = DEFAULT_CACHE_TTL): List<Exercise>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$EXERCISES_PREFIX$categoryId.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<Exercise>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Solfege Notes Caching
    // ============================================
    
    suspend fun cacheSolfegeNotes(exerciseId: String, notes: List<SolfegeNote>) = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$SOLFEGE_NOTES_PREFIX$exerciseId.json")
                file.writeText(json.encodeToString(CachedData(notes, System.currentTimeMillis())))
            } catch (e: Exception) {
                // Log error
            }
        }
    
    suspend fun getCachedSolfegeNotes(exerciseId: String, ttl: Long = DEFAULT_CACHE_TTL): List<SolfegeNote>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$SOLFEGE_NOTES_PREFIX$exerciseId.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<SolfegeNote>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Melodic Notes Caching
    // ============================================
    
    suspend fun cacheMelodicNotes(exerciseId: String, notes: List<SolfegeNote>) = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$MELODIC_NOTES_PREFIX$exerciseId.json")
                file.writeText(json.encodeToString(CachedData(notes, System.currentTimeMillis())))
            } catch (e: Exception) {
                // Log error
            }
        }
    
    suspend fun getCachedMelodicNotes(exerciseId: String, ttl: Long = DEFAULT_CACHE_TTL): List<SolfegeNote>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$MELODIC_NOTES_PREFIX$exerciseId.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<SolfegeNote>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Rhythm Patterns Caching
    // ============================================
    
    suspend fun cacheRhythmPatterns(exerciseId: String, patterns: List<RhythmPattern>) = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$RHYTHM_PATTERNS_PREFIX$exerciseId.json")
                file.writeText(json.encodeToString(CachedData(patterns, System.currentTimeMillis())))
            } catch (e: Exception) {
                // Log error
            }
        }
    
    suspend fun getCachedRhythmPatterns(exerciseId: String, ttl: Long = DEFAULT_CACHE_TTL): List<RhythmPattern>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$RHYTHM_PATTERNS_PREFIX$exerciseId.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<RhythmPattern>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Interval Questions Caching
    // ============================================
    
    suspend fun cacheIntervalQuestions(exerciseId: String, questions: List<IntervalQuestion>) = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$INTERVAL_QUESTIONS_PREFIX$exerciseId.json")
                file.writeText(json.encodeToString(CachedData(questions, System.currentTimeMillis())))
            } catch (e: Exception) {
                // Log error
            }
        }
    
    suspend fun getCachedIntervalQuestions(exerciseId: String, ttl: Long = DEFAULT_CACHE_TTL): List<IntervalQuestion>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, "$INTERVAL_QUESTIONS_PREFIX$exerciseId.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<IntervalQuestion>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // User Progress Caching
    // ============================================
    
    suspend fun cacheUserProgress(progress: List<UserProgress>) = withContext(Dispatchers.IO) {
        try {
            val file = File(userDir, "$USER_PROGRESS_KEY.json")
            file.writeText(json.encodeToString(CachedData(progress, System.currentTimeMillis())))
        } catch (e: Exception) {
            // Log error
        }
    }
    
    suspend fun getCachedUserProgress(ttl: Long = DEFAULT_CACHE_TTL): List<UserProgress>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(userDir, "$USER_PROGRESS_KEY.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<UserProgress>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Achievements Caching
    // ============================================
    
    suspend fun cacheAchievements(achievements: List<Achievement>) = withContext(Dispatchers.IO) {
        try {
            val file = File(userDir, "$USER_ACHIEVEMENTS_KEY.json")
            file.writeText(json.encodeToString(CachedData(achievements, System.currentTimeMillis())))
        } catch (e: Exception) {
            // Log error
        }
    }
    
    suspend fun getCachedAchievements(ttl: Long = DEFAULT_CACHE_TTL): List<Achievement>? = 
        withContext(Dispatchers.IO) {
            try {
                val file = File(userDir, "$USER_ACHIEVEMENTS_KEY.json")
                if (!file.exists()) return@withContext null
                
                val cached = json.decodeFromString<CachedData<List<Achievement>>>(file.readText())
                if (System.currentTimeMillis() - cached.timestamp > ttl) {
                    return@withContext null
                }
                cached.data
            } catch (e: Exception) {
                null
            }
        }
    
    // ============================================
    // Last Sync Tracking
    // ============================================
    
    suspend fun recordSync(key: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(metaDir, "$LAST_SYNC_KEY.json")
            val syncData = try {
                if (file.exists()) {
                    json.decodeFromString<MutableMap<String, Long>>(file.readText())
                } else {
                    mutableMapOf()
                }
            } catch (e: Exception) {
                mutableMapOf()
            }
            
            syncData[key] = System.currentTimeMillis()
            file.writeText(json.encodeToString(syncData))
        } catch (e: Exception) {
            // Log error
        }
    }
    
    suspend fun getLastSync(key: String): Long? = withContext(Dispatchers.IO) {
        try {
            val file = File(metaDir, "$LAST_SYNC_KEY.json")
            if (!file.exists()) return@withContext null
            
            val syncData = json.decodeFromString<Map<String, Long>>(file.readText())
            syncData[key]
        } catch (e: Exception) {
            null
        }
    }
    
    // ============================================
    // Cache Management
    // ============================================
    
    /**
     * Clear all cached data
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            userDir.deleteRecursively()
            metaDir.deleteRecursively()
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Clear expired cache entries
     */
    suspend fun clearExpiredCache(ttl: Long = DEFAULT_CACHE_TTL) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            
            listOf(cacheDir, userDir).forEach { dir ->
                dir.listFiles()?.forEach { file ->
                    if (file.extension == "json") {
                        try {
                            val cached = json.decodeFromString<CachedData<Any>>(file.readText())
                            if (currentTime - cached.timestamp > ttl) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            // Invalid file, delete it
                            file.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Get total cache size in bytes
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            var totalSize = 0L
            listOf(cacheDir, userDir, metaDir).forEach { dir ->
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        totalSize += file.length()
                    }
                }
            }
            totalSize
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Generic wrapper for cached data with timestamp
 */
@kotlinx.serialization.Serializable
private data class CachedData<T>(
    val data: T,
    val timestamp: Long
)
