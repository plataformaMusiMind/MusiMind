package com.musimind.presentation.games.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * QR Code Generator for Quiz Multiplayer rooms
 */
object QRCodeGenerator {
    
    /**
     * Generate a simple QR code bitmap (placeholder until ZXing is added)
     * In production, use ZXing library for proper QR generation
     */
    fun generateQRCode(data: String, size: Int = 256): Bitmap {
        // Create a simple pattern based on data hash (placeholder)
        // In production: use com.google.zxing.qrcode.encoder.QRCodeWriter
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cellSize = size / 25
        val hash = data.hashCode()
        
        // Generate pseudo-random pattern based on data
        val random = Random(hash)
        
        for (y in 0 until 25) {
            for (x in 0 until 25) {
                val isBlack = when {
                    // Corner squares (positioning patterns)
                    (x < 7 && y < 7) || (x >= 18 && y < 7) || (x < 7 && y >= 18) -> {
                        isPositionPattern(x % 18, y % 18)
                    }
                    // Data area
                    else -> random.nextBoolean()
                }
                
                val color = if (isBlack) Color.BLACK else Color.WHITE
                for (cy in 0 until cellSize) {
                    for (cx in 0 until cellSize) {
                        val px = x * cellSize + cx
                        val py = y * cellSize + cy
                        if (px < size && py < size) {
                            bitmap.setPixel(px, py, color)
                        }
                    }
                }
            }
        }
        
        return bitmap
    }
    
    private fun isPositionPattern(x: Int, y: Int): Boolean {
        // Create the classic QR position pattern
        return when {
            x == 0 || x == 6 || y == 0 || y == 6 -> true
            x in 2..4 && y in 2..4 -> true
            else -> false
        }
    }
    
    /**
     * Generate a 6-character room code
     */
    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluding similar chars
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}

/**
 * Composable to display QR code with room info
 */
@Composable
fun QRCodeDisplay(
    roomCode: String,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(roomCode) {
        QRCodeGenerator.generateQRCode("musimind://room/$roomCode", 256)
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Escaneie para entrar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // QR Code
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code para entrar na sala",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "ou digite o código:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Room Code Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roomCode.forEach { char ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Compartilhe este código com seus amigos para\neles entrarem no quiz!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Composable for room code input
 */
@Composable
fun RoomCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0 until 6).forEach { index ->
            val char = code.getOrNull(index)?.toString() ?: ""
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (char.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (char.isNotEmpty()) 
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
