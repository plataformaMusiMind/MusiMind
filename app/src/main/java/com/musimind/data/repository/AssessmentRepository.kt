package com.musimind.data.repository

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for custom assessments created by teachers/schools
 * 
 * Supports various question types:
 * - Theory questions
 * - Note identification on score
 * - Interval recognition (with audio)
 * - Chord recognition (with audio)
 * - Solfege/pitch matching
 */
@Singleton
class AssessmentRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    /**
     * Create a new assessment
     */
    suspend fun createAssessment(
        title: String,
        description: String,
        category: String,
        isPublic: Boolean = false,
        timeLimitSeconds: Int = 0,
        passingScore: Int = 60,
        creatorType: String = "teacher"
    ): Result<AssessmentEntity> {
        return try {
            val userId = currentUserId ?: throw IllegalStateException("Not logged in")
            
            val assessment = mapOf(
                "creator_id" to userId,
                "creator_type" to creatorType,
                "title" to title,
                "description" to description,
                "category" to category,
                "is_public" to isPublic,
                "time_limit_seconds" to timeLimitSeconds,
                "passing_score" to passingScore
            )
            
            val result = postgrest.from("custom_assessments")
                .insert(assessment) { select() }
                .decodeSingle<AssessmentEntity>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a question to an assessment
     */
    suspend fun addQuestion(
        assessmentId: String,
        questionType: String,
        questionText: String,
        options: List<QuestionOption>? = null,
        correctAnswer: String,
        audioUrl: String? = null,
        scoreRender: JsonObject? = null,
        timeLimitSeconds: Int = 15,
        points: Int = 10,
        orderIndex: Int = 0
    ): Result<AssessmentQuestionEntity> {
        return try {
            val question = mapOf(
                "assessment_id" to assessmentId,
                "question_type" to questionType,
                "question_text" to questionText,
                "options" to kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(QuestionOption.serializer()),
                    options ?: emptyList()
                ),
                "correct_answer" to correctAnswer,
                "audio_url" to audioUrl,
                "score_render" to scoreRender?.toString(),
                "time_limit_seconds" to timeLimitSeconds,
                "points" to points,
                "order_index" to orderIndex
            )
            
            val result = postgrest.from("assessment_questions")
                .insert(question) { select() }
                .decodeSingle<AssessmentQuestionEntity>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get assessments created by the current user
     */
    suspend fun getMyAssessments(): Result<List<AssessmentEntity>> {
        return try {
            val userId = currentUserId ?: return Result.success(emptyList())
            
            val assessments = postgrest.from("custom_assessments")
                .select {
                    filter { eq("creator_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<AssessmentEntity>()
            
            Result.success(assessments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get public assessments
     */
    suspend fun getPublicAssessments(category: String? = null): Result<List<AssessmentEntity>> {
        return try {
            val assessments = postgrest.from("custom_assessments")
                .select {
                    filter { 
                        eq("is_public", true)
                        category?.let { eq("category", it) }
                    }
                    order("created_at", Order.DESCENDING)
                    limit(50)
                }
                .decodeList<AssessmentEntity>()
            
            Result.success(assessments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get assessments assigned to a class
     */
    suspend fun getClassAssessments(classId: String): Result<List<AssessmentWithAssignment>> {
        return try {
            val assignments = postgrest.from("assessment_assignments")
                .select {
                    filter { eq("class_id", classId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<AssessmentAssignmentEntity>()
            
            val assessmentsWithAssignments = assignments.mapNotNull { assignment ->
                try {
                    val assessment = postgrest.from("custom_assessments")
                        .select { filter { eq("id", assignment.assessmentId) } }
                        .decodeSingleOrNull<AssessmentEntity>()
                    
                    assessment?.let {
                        AssessmentWithAssignment(
                            assessment = it,
                            assignment = assignment
                        )
                    }
                } catch (e: Exception) { null }
            }
            
            Result.success(assessmentsWithAssignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get questions for an assessment
     */
    suspend fun getAssessmentQuestions(assessmentId: String): Result<List<AssessmentQuestionEntity>> {
        return try {
            val questions = postgrest.from("assessment_questions")
                .select {
                    filter { eq("assessment_id", assessmentId) }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<AssessmentQuestionEntity>()
            
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start an assessment attempt
     */
    suspend fun startAttempt(assessmentId: String): Result<AssessmentAttemptEntity> {
        return try {
            val userId = currentUserId ?: throw IllegalStateException("Not logged in")
            
            val attempt = mapOf(
                "assessment_id" to assessmentId,
                "user_id" to userId,
                "started_at" to java.time.Instant.now().toString()
            )
            
            val result = postgrest.from("assessment_attempts")
                .insert(attempt) { select() }
                .decodeSingle<AssessmentAttemptEntity>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Complete an assessment attempt
     */
    suspend fun completeAttempt(
        attemptId: String,
        score: Int,
        totalPoints: Int,
        answers: JsonObject
    ): Result<AssessmentAttemptEntity> {
        return try {
            val percentage = if (totalPoints > 0) (score.toFloat() / totalPoints * 100) else 0f
            
            // Get assessment to check passing score
            val attempt = postgrest.from("assessment_attempts")
                .select { filter { eq("id", attemptId) } }
                .decodeSingleOrNull<AssessmentAttemptEntity>()
            
            val assessment = attempt?.let {
                postgrest.from("custom_assessments")
                    .select { filter { eq("id", it.assessmentId) } }
                    .decodeSingleOrNull<AssessmentEntity>()
            }
            
            val passed = percentage >= (assessment?.passingScore ?: 60)
            
            postgrest.from("assessment_attempts").update(
                mapOf(
                    "finished_at" to java.time.Instant.now().toString(),
                    "score" to score,
                    "total_points" to totalPoints,
                    "percentage" to percentage,
                    "passed" to passed,
                    "answers" to answers.toString()
                )
            ) {
                filter { eq("id", attemptId) }
            }
            
            val updatedAttempt = postgrest.from("assessment_attempts")
                .select { filter { eq("id", attemptId) } }
                .decodeSingle<AssessmentAttemptEntity>()
            
            Result.success(updatedAttempt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get attempts for an assessment (for teacher view)
     */
    suspend fun getAssessmentAttempts(assessmentId: String): Result<List<AssessmentAttemptEntity>> {
        return try {
            val attempts = postgrest.from("assessment_attempts")
                .select {
                    filter { eq("assessment_id", assessmentId) }
                    order("finished_at", Order.DESCENDING)
                }
                .decodeList<AssessmentAttemptEntity>()
            
            Result.success(attempts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's attempts (for student view)
     */
    suspend fun getMyAttempts(): Result<List<AssessmentAttemptEntity>> {
        return try {
            val userId = currentUserId ?: return Result.success(emptyList())
            
            val attempts = postgrest.from("assessment_attempts")
                .select {
                    filter { eq("user_id", userId) }
                    order("started_at", Order.DESCENDING)
                }
                .decodeList<AssessmentAttemptEntity>()
            
            Result.success(attempts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Assign assessment to a class
     */
    suspend fun assignToClass(
        assessmentId: String,
        classId: String,
        dueDate: String? = null
    ): Result<Unit> {
        return try {
            val userId = currentUserId ?: throw IllegalStateException("Not logged in")
            
            postgrest.from("assessment_assignments").insert(
                mapOf(
                    "assessment_id" to assessmentId,
                    "class_id" to classId,
                    "assigned_by" to userId,
                    "due_date" to dueDate
                )
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an assessment
     */
    suspend fun deleteAssessment(assessmentId: String): Result<Unit> {
        return try {
            postgrest.from("custom_assessments").delete {
                filter { eq("id", assessmentId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update assessment
     */
    suspend fun updateAssessment(
        assessmentId: String,
        title: String? = null,
        description: String? = null,
        isPublic: Boolean? = null,
        passingScore: Int? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            title?.let { updates["title"] = it }
            description?.let { updates["description"] = it }
            isPublic?.let { updates["is_public"] = it }
            passingScore?.let { updates["passing_score"] = it }
            updates["updated_at"] = java.time.Instant.now().toString()
            
            postgrest.from("custom_assessments").update(updates) {
                filter { eq("id", assessmentId) }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Entities
@Serializable
data class AssessmentEntity(
    val id: String = "",
    @SerialName("creator_id")
    val creatorId: String = "",
    @SerialName("creator_type")
    val creatorType: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String? = null,
    @SerialName("is_public")
    val isPublic: Boolean = false,
    @SerialName("time_limit_seconds")
    val timeLimitSeconds: Int = 0,
    @SerialName("passing_score")
    val passingScore: Int = 60,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class AssessmentQuestionEntity(
    val id: String = "",
    @SerialName("assessment_id")
    val assessmentId: String = "",
    @SerialName("question_type")
    val questionType: String = "",
    @SerialName("question_text")
    val questionText: String = "",
    @SerialName("question_data")
    val questionData: JsonObject? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
    @SerialName("score_render")
    val scoreRender: JsonObject? = null,
    val options: String? = null, // JSON string of options
    @SerialName("correct_answer")
    val correctAnswer: String = "",
    @SerialName("time_limit_seconds")
    val timeLimitSeconds: Int = 15,
    val points: Int = 10,
    @SerialName("order_index")
    val orderIndex: Int = 0
)

@Serializable
data class AssessmentAttemptEntity(
    val id: String = "",
    @SerialName("assessment_id")
    val assessmentId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("finished_at")
    val finishedAt: String? = null,
    val score: Int = 0,
    @SerialName("total_points")
    val totalPoints: Int = 0,
    val percentage: Float = 0f,
    val passed: Boolean = false,
    val answers: String? = null // JSON string
)

@Serializable
data class AssessmentAssignmentEntity(
    val id: String = "",
    @SerialName("assessment_id")
    val assessmentId: String = "",
    @SerialName("class_id")
    val classId: String = "",
    @SerialName("assigned_by")
    val assignedBy: String = "",
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

data class AssessmentWithAssignment(
    val assessment: AssessmentEntity,
    val assignment: AssessmentAssignmentEntity
)
