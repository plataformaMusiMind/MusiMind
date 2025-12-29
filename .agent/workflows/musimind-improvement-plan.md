---
description: Plano de melhorias do MusiMind - Projeto de educação musical profissional
---

# 🚀 PLANO DE MELHORIAS MUSIMIND - SPRINT PARA PRODUÇÃO

## VISÃO GERAL DO PROJETO

**MusiMind** é uma aplicação Android de educação musical gamificada, construída com:
- Kotlin + Jetpack Compose
- Supabase (Auth, Database, Storage)
- Hilt para injeção de dependências
- Arquitetura MVVM

---

## 📌 SPRINT 1: CORREÇÕES CRÍTICAS (OBRIGATÓRIAS)

### 1.1 Implementar Login com Google
// turbo
```bash
# Passo 1: Verificar configuração atual do Supabase
# Acessar: https://supabase.com/dashboard/project/qspzqkyiemjtrlupfzuq/auth/providers
```

**Arquivos a modificar:**
- `AuthViewModel.kt` - Implementar `signInWithGoogle()`
- `build.gradle.kts` - Adicionar dependências Google Sign-In
- `MainActivity.kt` - Configurar ActivityResultLauncher

**Código necessário:**
```kotlin
// Em AuthViewModel.kt
fun signInWithGoogle(activityContext: android.content.Context) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            auth.signInWith(auth.providers.google) 
            // Processar resultado
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
        }
    }
}
```

### 1.2 Implementar Reset de Senha
**Arquivo:** `AuthViewModel.kt`

```kotlin
fun forgotPassword(email: String) {
    viewModelScope.launch {
        try {
            auth.resetPasswordForEmail(email)
            _uiState.update { it.copy(errorMessage = "Email de recuperação enviado!") }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Erro: ${e.message}") }
        }
    }
}
```

### 1.3 Corrigir userId em Todos os Mini-Games
**Arquivo:** `NavGraph.kt`

Criar um helper para obter userId:
```kotlin
// Injetar no NavGraph
@Composable
fun rememberCurrentUserId(auth: Auth): String {
    return remember { auth.currentSessionOrNull()?.user?.id ?: "" }
}
```

Substituir todos os `userId = ""` por chamada ao helper.

### 1.4 Popular Tabela node_requirements
// turbo
```bash
# Executar no Supabase SQL Editor:
# INSERT INTO node_requirements (node_id, requirement_type, requirement_value)
# VALUES ('b0000001-0000-0000-0000-000000000001', 'none', 0);
```

---

## 📌 SPRINT 2: SISTEMA DE ÁUDIO

### 2.1 Criar AudioManager Unificado
**Novo arquivo:** `com/musimind/music/audio/GameAudioManager.kt`

```kotlin
@Singleton
class GameAudioManager @Inject constructor(
    private val midiEngine: MidiEngine,
    private val soundPool: SoundPool
) {
    fun playNote(note: String, durationMs: Long) { }
    fun playChord(notes: List<String>) { }
    fun playMetronome(bpm: Int) { }
    fun playSuccessSound() { }
    fun playErrorSound() { }
}
```

### 2.2 Integrar Áudio nos ViewModels
Adicionar injeção do AudioManager em:
- IntervalHeroViewModel
- ProgressionQuestViewModel  
- ChordBuilderViewModel
- ScalePuzzleViewModel
- RhythmTapViewModel
- MelodyMemoryViewModel
- ChordMatchViewModel
- TempoRunViewModel
- SolfegeSingViewModel

---

## 📌 SPRINT 3: QUIZ MULTIPLAYER

### 3.1 Criar Tela de Quiz Multiplayer
**Novo arquivo:** `presentation/games/QuizMultiplayerScreen.kt`

Funcionalidades:
- Criar sala
- Entrar com código de 6 dígitos
- Exibir QR Code
- Lista de participantes
- Contador regressivo
- Perguntas sincronizadas

### 3.2 Implementar Geração de QR Code
Adicionar dependência:
```kotlin
implementation("com.google.zxing:core:3.5.1")
```

Criar componente:
```kotlin
@Composable
fun QRCodeDisplay(data: String) {
    val bitmap = remember(data) { generateQRCode(data) }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code")
}
```

### 3.3 Sistema de Salas em Tempo Real
Usar Supabase Realtime:
```kotlin
val channel = realtime.channel("quiz_${roomCode}")
channel.on<RealtimeChannel.Presence>("presence") { }
channel.on<PostgresAction>("postgres_changes") { }
```

---

## 📌 SPRINT 4: PERFIL E UPLOAD

### 4.1 Conectar Dados Reais ao ProfileScreen
Criar `ProfileViewModel`:
```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {
    // Carregar dados reais do usuário
}
```

### 4.2 Implementar Upload de Avatar
**Arquivos a modificar:**
- `ProfileScreen.kt` - Adicionar botão de câmera/galeria
- `UserRepository.kt` - Função uploadAvatar()

```kotlin
suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String {
    val bucket = storage.from("avatars")
    val path = "users/$userId/avatar_${System.currentTimeMillis()}.jpg"
    bucket.upload(path, imageBytes)
    return bucket.publicUrl(path)
}
```

---

## 📌 SPRINT 5: OTIMIZAÇÕES

### 5.1 Criar Índices no Banco
```sql
-- Executar no Supabase
CREATE INDEX IF NOT EXISTS idx_user_progress_user ON user_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_exercises_category ON exercises(category);
CREATE INDEX IF NOT EXISTS idx_learning_nodes_path ON learning_nodes(path_id);
CREATE INDEX IF NOT EXISTS idx_game_sessions_user ON game_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_duels_challenger ON duels(challenger_id);
CREATE INDEX IF NOT EXISTS idx_duels_opponent ON duels(opponent_id);
```

### 5.2 Configurar RLS
```sql
-- Executar o script 099_optimization_rls.sql no Supabase
```

### 5.3 Implementar Cache Local
Usar Room Database para cache offline:
```kotlin
@Entity
data class CachedExercise(
    @PrimaryKey val id: String,
    val data: String,
    val cachedAt: Long
)
```

---

## 📊 CHECKLIST DE VERIFICAÇÃO FINAL

### Autenticação
- [ ] Login com email funciona
- [ ] Logout funciona
- [ ] Login com Google funciona
- [ ] Reset de senha funciona
- [ ] Registro de usuário funciona

### Onboarding
- [ ] Tutorial exibe todas as páginas
- [ ] Seleção de tipo de usuário salva
- [ ] Seleção de plano salva
- [ ] Seleção de avatar salva
- [ ] Placement test funciona

### Learning Path
- [ ] Nós carregam corretamente
- [ ] Primeiro nó tem dados reais
- [ ] Progresso é salvo
- [ ] XP é atualizado

### Exercícios
- [ ] Solfejo funciona com áudio
- [ ] Ritmo funciona com áudio
- [ ] Intervalos funcionam com áudio
- [ ] Percepção melódica funciona

### Mini-Games
- [ ] Todos os jogos carregam
- [ ] Pontuação é salva
- [ ] userId está correto
- [ ] High scores funcionam

### Quiz Multiplayer
- [ ] Criar sala funciona
- [ ] QR Code é gerado
- [ ] Código de 6 dígitos funciona
- [ ] Sincronização em tempo real
- [ ] Ranking final exibe

### Duelo
- [ ] Criar desafio funciona
- [ ] Aceitar desafio funciona
- [ ] Sincronização em tempo real
- [ ] Resultado é exibido
- [ ] XP é atribuído ao vencedor

### Perfil
- [ ] Dados reais são exibidos
- [ ] Upload de avatar funciona
- [ ] Estatísticas são dinâmicas
- [ ] Conquistas funcionam

### Configurações
- [ ] Modo dark/light funciona
- [ ] Tema de cores funciona
- [ ] Notificações funcionam

### Banco de Dados
- [ ] RLS está configurado
- [ ] Índices estão criados
- [ ] Dados de seed estão populados

---

## 🔧 COMANDOS ÚTEIS

// turbo
```bash
# Build do projeto
./gradlew assembleDebug
```

// turbo  
```bash
# Rodar testes
./gradlew test
```

// turbo
```bash
# Verificar lint
./gradlew lint
```

---

*Workflow criado em 28/12/2025 - MusiMind v1.0*
