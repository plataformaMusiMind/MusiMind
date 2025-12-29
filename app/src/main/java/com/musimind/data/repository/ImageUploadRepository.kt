package com.musimind.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing image uploads to Supabase Storage
 */
@Singleton
class ImageUploadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: Storage
) {
    companion object {
        private const val AVATARS_BUCKET = "avatars"
        private const val IMAGES_BUCKET = "images"
        private const val MAX_IMAGE_SIZE = 1024 // Max dimension before compression
        private const val COMPRESSION_QUALITY = 85 // JPEG quality percentage
    }
    
    /**
     * Upload user avatar image
     * @param userId User ID
     * @param imageUri URI of the image to upload
     * @return Public URL of the uploaded image
     */
    suspend fun uploadAvatar(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val compressedBytes = compressImage(imageUri) 
                ?: return@withContext Result.failure(Exception("Não foi possível processar a imagem"))
            
            val fileName = "user_${userId}_${System.currentTimeMillis()}.jpg"
            val path = "users/$userId/$fileName"
            
            // Upload to Supabase Storage
            val bucket = storage.from(AVATARS_BUCKET)
            bucket.upload(path, compressedBytes, upsert = true)
            
            // Get public URL
            val publicUrl = bucket.publicUrl(path)
            
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload a general image (for content, etc.)
     * @param imageUri URI of the image to upload
     * @param folder Subfolder within images bucket
     * @return Public URL of the uploaded image
     */
    suspend fun uploadImage(imageUri: Uri, folder: String = "general"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val compressedBytes = compressImage(imageUri)
                ?: return@withContext Result.failure(Exception("Não foi possível processar a imagem"))
            
            val fileName = "${UUID.randomUUID()}.jpg"
            val path = "$folder/$fileName"
            
            val bucket = storage.from(IMAGES_BUCKET)
            bucket.upload(path, compressedBytes, upsert = true)
            
            val publicUrl = bucket.publicUrl(path)
            
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an avatar
     * @param avatarUrl URL of the avatar to delete
     */
    suspend fun deleteAvatar(avatarUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Extract path from URL
            val path = extractPathFromUrl(avatarUrl, AVATARS_BUCKET)
            if (path != null) {
                storage.from(AVATARS_BUCKET).delete(path)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Compress image to reduce upload size
     */
    private fun compressImage(imageUri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return null
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return null
            
            // Calculate new dimensions maintaining aspect ratio
            val maxDimension = MAX_IMAGE_SIZE
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val scaleFactor = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }
            
            val scaledBitmap = if (scaleFactor < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scaleFactor).toInt(),
                    (height * scaleFactor).toInt(),
                    true
                )
            } else {
                originalBitmap
            }
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            
            // Clean up
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract storage path from public URL
     */
    private fun extractPathFromUrl(url: String, bucketName: String): String? {
        return try {
            val marker = "/storage/v1/object/public/$bucketName/"
            val index = url.indexOf(marker)
            if (index >= 0) {
                url.substring(index + marker.length)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
