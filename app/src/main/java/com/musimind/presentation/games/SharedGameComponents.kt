package com.musimind.presentation.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componentes compartilhados entre os jogos
 */

/**
 * Tela de pausa compartilhada
 */
@Composable
fun PauseScreen(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏸️ PAUSADO",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Continuar")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Reiniciar", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(onClick = onQuit) {
                    Text("Sair", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/**
 * Tela de resultado compartilhada
 */
@Composable
fun ResultScreen(
    score: Int,
    stars: Int,
    correctCount: Int,
    wrongCount: Int,
    maxCombo: Int,
    xpEarned: Int,
    coinsEarned: Int,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título baseado nas estrelas
                val title = when (stars) {
                    3 -> "🌟 PERFEITO! 🌟"
                    2 -> "⭐ MUITO BEM! ⭐"
                    1 -> "👍 BOM TRABALHO!"
                    else -> "Continue praticando!"
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Estrelas
                Row {
                    repeat(3) { index ->
                        val isEarned = index < stars
                        Icon(
                            imageVector = if (isEarned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isEarned) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Pontuação
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PONTOS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Estatísticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStatItem(value = "$correctCount", label = "Acertos", color = Color(0xFF4CAF50))
                    ResultStatItem(value = "$wrongCount", label = "Erros", color = Color(0xFFE91E63))
                    ResultStatItem(value = "x$maxCombo", label = "Max Combo", color = Color(0xFFFF9800))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recompensas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 20.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("+$xpEarned XP", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 20.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("+$coinsEarned", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Níveis", color = Color.White)
                    }
                    
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Jogar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Enum de resultado de rodada
 */
enum class RoundResult {
    SUCCESS,
    FAIL
}
