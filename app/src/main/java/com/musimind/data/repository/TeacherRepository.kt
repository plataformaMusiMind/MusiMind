package com.musimind.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.musimind.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for teacher/school functionality
 */
@Singleton
class TeacherRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String? get() = auth.currentUser?.uid
    
    private val classesCollection get() = firestore.collection("classes")
    private val assignmentsCollection get() = firestore.collection("assignments")
    private val usersCollection get() = firestore.collection("users")
    
    /**
     * Create a new class
     */
    suspend fun createClass(name: String, description: String): StudentClass {
        val teacherId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        val classId = classesCollection.document().id
        val inviteCode = generateInviteCode()
        
        val studentClass = StudentClass(
            id = classId,
            name = name,
            description = description,
            teacherId = teacherId,
            inviteCode = inviteCode
        )
        
        classesCollection.document(classId).set(studentClass).await()
        
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
            classesCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
                .toObjects(StudentClass::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get students in a class with their progress
     */
    suspend fun getClassStudents(classId: String): List<StudentProgress> {
        return try {
            val classDoc = classesCollection.document(classId).get().await()
            val studentClass = classDoc.toObject(StudentClass::class.java) ?: return emptyList()
            
            studentClass.students.mapNotNull { studentId ->
                getStudentProgress(studentId)
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
            val userDoc = usersCollection.document(studentId).get().await()
            
            StudentProgress(
                userId = studentId,
                displayName = userDoc.getString("displayName") ?: "Aluno",
                avatarUrl = userDoc.getString("avatarUrl"),
                level = userDoc.getLong("level")?.toInt() ?: 1,
                totalXp = userDoc.getLong("totalXp")?.toInt() ?: 0,
                currentStreak = userDoc.getLong("currentStreak")?.toInt() ?: 0,
                exercisesCompleted = userDoc.getLong("exercisesCompleted")?.toInt() ?: 0,
                lessonsCompleted = userDoc.getLong("lessonsCompleted")?.toInt() ?: 0,
                averageAccuracy = userDoc.getDouble("averageAccuracy")?.toFloat() ?: 0f,
                lastActiveAt = userDoc.getLong("lastActiveAt") ?: 0,
                weeklyXp = userDoc.getLong("weeklyXp")?.toInt() ?: 0
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
            val classQuery = classesCollection
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()
            
            if (classQuery.isEmpty) return false
            
            val classDoc = classQuery.documents.first()
            val currentStudents = classDoc.get("students") as? List<*> ?: emptyList<String>()
            
            if (currentStudents.contains(studentId)) return true // Already joined
            
            classDoc.reference.update("students", currentStudents + studentId).await()
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
            val classDoc = classesCollection.document(classId).get().await()
            val currentStudents = classDoc.get("students") as? List<*> ?: return
            
            classesCollection.document(classId)
                .update("students", currentStudents.filter { it != studentId })
                .await()
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
        
        val assignmentId = assignmentsCollection.document().id
        
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
        
        assignmentsCollection.document(assignmentId).set(assignment).await()
        
        return assignment
    }
    
    /**
     * Get assignments for a class
     */
    suspend fun getClassAssignments(classId: String): List<Assignment> {
        return try {
            assignmentsCollection
                .whereEqualTo("classId", classId)
                .orderBy("dueDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Assignment::class.java)
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
            val allStudentIds = classes.flatMap { it.students }.distinct()
            
            val studentProgresses = allStudentIds.mapNotNull { getStudentProgress(it) }
            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            
            TeacherStats(
                totalStudents = allStudentIds.size,
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
        classesCollection.document(classId).delete().await()
    }
}
