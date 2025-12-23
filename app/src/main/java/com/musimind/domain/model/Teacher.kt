package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Teacher/School specific models
 */

/**
 * Class/Group of students managed by teacher
 */
@Serializable
data class StudentClass(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val teacherId: String = "",
    val schoolId: String? = null,
    val students: List<String> = emptyList(), // Student user IDs
    val createdAt: Long = System.currentTimeMillis(),
    val inviteCode: String = ""
) {
    val studentCount: Int get() = students.size
}

/**
 * Student progress summary for teacher view
 */
@Serializable
data class StudentProgress(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val level: Int = 1,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val exercisesCompleted: Int = 0,
    val lessonsCompleted: Int = 0,
    val averageAccuracy: Float = 0f,
    val lastActiveAt: Long = 0,
    val weeklyXp: Int = 0,
    val categoryProgress: Map<String, CategoryProgress> = emptyMap()
) {
    val isActive: Boolean get() = 
        System.currentTimeMillis() - lastActiveAt < 7 * 24 * 60 * 60 * 1000 // Active in last 7 days
    
    val performanceLevel: PerformanceLevel get() = when {
        averageAccuracy >= 0.9f && currentStreak >= 7 -> PerformanceLevel.EXCELLENT
        averageAccuracy >= 0.75f && currentStreak >= 3 -> PerformanceLevel.GOOD
        averageAccuracy >= 0.5f -> PerformanceLevel.MODERATE
        else -> PerformanceLevel.NEEDS_ATTENTION
    }
}

@Serializable
data class CategoryProgress(
    val category: String,
    val exercisesCompleted: Int = 0,
    val averageAccuracy: Float = 0f,
    val lastPracticed: Long = 0
)

enum class PerformanceLevel {
    EXCELLENT,
    GOOD,
    MODERATE,
    NEEDS_ATTENTION
}

/**
 * Assignment created by teacher
 */
@Serializable
data class Assignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val teacherId: String = "",
    val classId: String = "",
    val category: MusicCategory = MusicCategory.SOLFEGE,
    val exerciseIds: List<String> = emptyList(),
    val dueDate: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val submissions: Map<String, AssignmentSubmission> = emptyMap()
) {
    val submissionCount: Int get() = submissions.size
    val isPastDue: Boolean get() = System.currentTimeMillis() > dueDate
}

@Serializable
data class AssignmentSubmission(
    val studentId: String,
    val submittedAt: Long,
    val score: Float, // 0-100
    val exerciseResults: List<ExerciseResult> = emptyList()
)

@Serializable
data class ExerciseResult(
    val exerciseId: String,
    val score: Float,
    val completedAt: Long
)

/**
 * Teacher dashboard statistics
 */
@Serializable
data class TeacherStats(
    val totalStudents: Int = 0,
    val totalClasses: Int = 0,
    val activeStudentsThisWeek: Int = 0,
    val totalExercisesCompleted: Int = 0,
    val averageStudentAccuracy: Float = 0f,
    val topPerformers: List<StudentProgress> = emptyList(),
    val needsAttention: List<StudentProgress> = emptyList()
)

/**
 * School dashboard model
 */
@Serializable
data class SchoolStats(
    val schoolId: String = "",
    val schoolName: String = "",
    val totalTeachers: Int = 0,
    val totalStudents: Int = 0,
    val totalClasses: Int = 0,
    val activeStudentsThisMonth: Int = 0,
    val averageAccuracy: Float = 0f,
    val topClasses: List<ClassSummary> = emptyList()
)

@Serializable
data class ClassSummary(
    val classId: String,
    val className: String,
    val teacherName: String,
    val studentCount: Int,
    val averageAccuracy: Float,
    val weeklyXpTotal: Int
)

/**
 * Custom quiz created by teacher
 */
@Serializable
data class TeacherQuiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val teacherId: String = "",
    val classId: String? = null, // null = available to all students
    val questions: List<QuizQuestion> = emptyList(),
    val timePerQuestion: Int = 30, // seconds
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String? = null,
    val category: MusicCategory = MusicCategory.SOLFEGE
)
