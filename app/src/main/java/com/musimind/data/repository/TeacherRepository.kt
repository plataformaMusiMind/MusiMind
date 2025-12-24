package com.musimind.data.repository

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for teacher/school functionality
 * Uses Supabase Postgrest
 */
@Singleton
class TeacherRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    /**
     * Create a new class
     */
    suspend fun createClass(name: String, description: String): StudentClass {
        val teacherId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        val classId = UUID.randomUUID().toString()
        val inviteCode = generateInviteCode()
        
        val studentClass = StudentClass(
            id = classId,
            name = name,
            description = description,
            teacherId = teacherId,
            inviteCode = inviteCode
        )
        
        postgrest.from("classes").insert(studentClass)
        
        return studentClass
    }
    
    private fun generateInviteCode(): String {
        return UUID.randomUUID().toString().take(6).uppercase()
    }
    
    /**
     * Get teacher's classes
     */
    suspend fun getTeacherClasses(): List<StudentClass> {
        val teacherId = currentUserId ?: return emptyList()
        
        return try {
            postgrest.from("classes")
                .select {
                    filter { eq("teacher_id", teacherId) }
                }
                .decodeList<StudentClass>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get students in a class with their progress
     */
    suspend fun getClassStudents(classId: String): List<StudentProgress> {
        return try {
            val members = postgrest.from("school_members")
                .select {
                    filter { 
                        eq("school_id", classId)
                        eq("role", "student")
                    }
                }
                .decodeList<SchoolMember>()
            
            members.mapNotNull { member ->
                getStudentProgress(member.userId)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get individual student progress
     */
    suspend fun getStudentProgress(studentId: String): StudentProgress? {
        return try {
            val user = postgrest.from("users")
                .select {
                    filter { eq("id", studentId) }
                }
                .decodeSingleOrNull<User>() ?: return null
            
            StudentProgress(
                userId = studentId,
                displayName = user.fullName.ifBlank { "Aluno" },
                avatarUrl = user.avatarUrl,
                level = user.level,
                totalXp = user.xp,
                currentStreak = user.streak,
                exercisesCompleted = 0, // TODO: Track separately
                lessonsCompleted = 0,
                averageAccuracy = 0f,
                lastActiveAt = 0L, // TODO: Parse from user.lastActiveAt
                weeklyXp = 0
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Add student to class via invite code
     */
    suspend fun joinClassByCode(inviteCode: String): Boolean {
        val studentId = currentUserId ?: return false
        
        return try {
            val school = postgrest.from("schools")
                .select {
                    filter { eq("invite_code", inviteCode) }
                }
                .decodeSingleOrNull<School>() ?: return false
            
            postgrest.from("school_members").insert(
                mapOf(
                    "school_id" to school.id,
                    "user_id" to studentId,
                    "role" to "student",
                    "status" to "active"
                )
            )
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Remove student from class
     */
    suspend fun removeStudentFromClass(classId: String, studentId: String) {
        try {
            postgrest.from("school_members").delete {
                filter {
                    eq("school_id", classId)
                    eq("user_id", studentId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Create assignment for class
     */
    suspend fun createAssignment(
        classId: String,
        title: String,
        description: String,
        category: MusicCategory,
        exerciseIds: List<String>,
        dueDate: Long
    ): Assignment {
        val teacherId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        val assignmentId = UUID.randomUUID().toString()
        
        val assignment = Assignment(
            id = assignmentId,
            title = title,
            description = description,
            teacherId = teacherId,
            classId = classId,
            category = category,
            exerciseIds = exerciseIds,
            dueDate = dueDate
        )
        
        postgrest.from("assignments").insert(assignment)
        
        return assignment
    }
    
    /**
     * Get assignments for a class
     */
    suspend fun getClassAssignments(classId: String): List<Assignment> {
        return try {
            postgrest.from("assignments")
                .select {
                    filter { eq("class_id", classId) }
                    order("due_date", Order.DESCENDING)
                }
                .decodeList<Assignment>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get teacher dashboard statistics
     */
    suspend fun getTeacherStats(): TeacherStats {
        val teacherId = currentUserId ?: return TeacherStats()
        
        return try {
            val classes = getTeacherClasses()
            val allStudentIds = mutableListOf<String>()
            
            // Get all student IDs from all classes
            for (classItem in classes) {
                val students = getClassStudents(classItem.id)
                allStudentIds.addAll(students.map { it.userId })
            }
            
            val uniqueStudentIds = allStudentIds.distinct()
            val studentProgresses = uniqueStudentIds.mapNotNull { getStudentProgress(it) }
            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            
            TeacherStats(
                totalStudents = uniqueStudentIds.size,
                totalClasses = classes.size,
                activeStudentsThisWeek = studentProgresses.count { it.lastActiveAt > oneWeekAgo },
                totalExercisesCompleted = studentProgresses.sumOf { it.exercisesCompleted },
                averageStudentAccuracy = if (studentProgresses.isNotEmpty()) 
                    studentProgresses.map { it.averageAccuracy }.average().toFloat() 
                else 0f,
                topPerformers = studentProgresses
                    .filter { it.performanceLevel == PerformanceLevel.EXCELLENT }
                    .sortedByDescending { it.weeklyXp }
                    .take(5),
                needsAttention = studentProgresses
                    .filter { it.performanceLevel == PerformanceLevel.NEEDS_ATTENTION }
                    .take(5)
            )
        } catch (e: Exception) {
            TeacherStats()
        }
    }
    
    /**
     * Delete a class
     */
    suspend fun deleteClass(classId: String) {
        postgrest.from("classes").delete {
            filter { eq("id", classId) }
        }
    }
}

// Helper data classes for Supabase response
@kotlinx.serialization.Serializable
private data class SchoolMember(
    val id: String = "",
    @kotlinx.serialization.SerialName("school_id")
    val schoolId: String = "",
    @kotlinx.serialization.SerialName("user_id")
    val userId: String = "",
    val role: String = "",
    val status: String = ""
)

@kotlinx.serialization.Serializable
private data class School(
    val id: String = "",
    val name: String = "",
    @kotlinx.serialization.SerialName("invite_code")
    val inviteCode: String? = null
)
