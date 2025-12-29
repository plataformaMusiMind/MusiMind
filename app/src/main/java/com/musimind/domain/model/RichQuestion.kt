package com.musimind.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tipos de perguntas suportados no Quiz/Duelo/Avaliação
 * 
 * Suporta desde perguntas simples de texto até
 * identificação de notas em partitura e ditado melódico.
 */
enum class QuestionType {
    MULTIPLE_CHOICE,       // Múltipla escolha texto simples
    NOTE_IDENTIFICATION,   // Identificar notas na partitura
    INTERVAL_RECOGNITION,  // Reconhecer intervalo (com áudio)
    CHORD_RECOGNITION,     // Reconhecer acorde (com áudio)
    RHYTHM_IDENTIFICATION, // Identificar padrão rítmico
    MELODY_DICTATION,      // Escrever melodia após ouvir
    PITCH_MATCHING,        // Cantar nota correta
    SOLFEGE_SINGING,       // Cantar solfejo
    THEORY_TEXT,           // Pergunta de teoria musical
    KEY_SIGNATURE,         // Identificar armadura de clave
    TIME_SIGNATURE,        // Identificar fórmula de compasso
    SYMBOL_IDENTIFICATION  // Identificar símbolo musical
}

/**
 * Pergunta rica com suporte a múltiplos formatos
 */
@Serializable
data class RichQuestion(
    val id: String,
    val type: QuestionType,
    val text: String,
    val options: List<QuestionOption> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val correctAnswerText: String? = null,
    val timeLimit: Int = 15,
    val points: Int = 10,
    
    // Para perguntas com áudio
    val audioData: AudioQuestionData? = null,
    
    // Para perguntas com partitura
    val scoreData: ScoreQuestionData? = null,
    
    // Para perguntas com imagem
    val imageUrl: String? = null,
    
    // Dica opcional
    val hint: String? = null,
    
    // Explicação após resposta
    val explanation: String? = null
)

/**
 * Opção de resposta
 */
@Serializable
data class QuestionOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean = false,
    // Para opções com áudio
    val audioData: AudioQuestionData? = null,
    // Para opções com notas
    val noteData: String? = null // "C4", "D4", etc
)

/**
 * Dados de áudio para reprodução
 */
@Serializable
data class AudioQuestionData(
    val type: AudioType,
    // Para notas/intervalos/acordes
    val notes: List<String> = emptyList(), // ["C4", "E4", "G4"]
    val duration: Int = 500, // ms por nota
    val tempo: Int = 80,
    // Para melodia
    val melody: List<MelodyNote> = emptyList(),
    // Para ritmo
    val rhythmPattern: String? = null
)

enum class AudioType {
    SINGLE_NOTE,
    INTERVAL,
    CHORD,
    MELODY,
    RHYTHM
}

@Serializable
data class MelodyNote(
    val note: String, // "C4"
    val duration: Float = 1f // em beats
)

/**
 * Dados para renderização de partitura
 */
@Serializable
data class ScoreQuestionData(
    val clef: String = "treble", // treble, bass, alto
    val keySignature: String = "C", // C, G, D, F, Bb, etc
    val timeSignature: String = "4/4",
    val notes: List<ScoreNote> = emptyList(),
    val showNoteNames: Boolean = false, // Esconder nomes para pergunta
    val highlightNoteIndex: Int? = null // Qual nota está sendo perguntada
)

@Serializable
data class ScoreNote(
    val pitch: String, // "C4", "D4", etc
    val duration: String = "quarter", // whole, half, quarter, eighth, sixteenth
    val accidental: String? = null, // sharp, flat, natural
    val isRest: Boolean = false
)

/**
 * Gerador de perguntas para Quiz/Duelo
 */
object QuestionGenerator {
    
    /**
     * Gera um conjunto de perguntas variadas para quiz
     */
    fun generateQuizQuestions(
        count: Int = 10,
        categories: List<QuestionType> = QuestionType.entries,
        difficulty: Int = 1 // 1=fácil, 2=médio, 3=difícil
    ): List<RichQuestion> {
        val questions = mutableListOf<RichQuestion>()
        val availableTypes = categories.shuffled()
        
        repeat(count) { index ->
            val type = availableTypes[index % availableTypes.size]
            questions.add(generateQuestion(type, difficulty, "q_$index"))
        }
        
        return questions.shuffled()
    }
    
    private fun generateQuestion(type: QuestionType, difficulty: Int, id: String): RichQuestion {
        return when (type) {
            QuestionType.MULTIPLE_CHOICE -> generateTheoryQuestion(id, difficulty)
            QuestionType.NOTE_IDENTIFICATION -> generateNoteIdentificationQuestion(id, difficulty)
            QuestionType.INTERVAL_RECOGNITION -> generateIntervalQuestion(id, difficulty)
            QuestionType.CHORD_RECOGNITION -> generateChordQuestion(id, difficulty)
            QuestionType.RHYTHM_IDENTIFICATION -> generateRhythmQuestion(id, difficulty)
            QuestionType.KEY_SIGNATURE -> generateKeySignatureQuestion(id, difficulty)
            QuestionType.TIME_SIGNATURE -> generateTimeSignatureQuestion(id, difficulty)
            QuestionType.SYMBOL_IDENTIFICATION -> generateSymbolQuestion(id, difficulty)
            else -> generateTheoryQuestion(id, difficulty)
        }
    }
    
    private fun generateTheoryQuestion(id: String, difficulty: Int): RichQuestion {
        val questions = listOf(
            RichQuestion(
                id = id,
                type = QuestionType.THEORY_TEXT,
                text = "Quantas notas tem uma escala maior?",
                options = listOf(
                    QuestionOption("a", "5 notas"),
                    QuestionOption("b", "6 notas"),
                    QuestionOption("c", "7 notas", isCorrect = true),
                    QuestionOption("d", "8 notas")
                ),
                correctAnswerIndex = 2,
                explanation = "A escala maior tem 7 notas: Dó, Ré, Mi, Fá, Sol, Lá, Si"
            ),
            RichQuestion(
                id = id,
                type = QuestionType.THEORY_TEXT,
                text = "O que indica um sustenido (#)?",
                options = listOf(
                    QuestionOption("a", "Abaixar a nota meio tom"),
                    QuestionOption("b", "Elevar a nota meio tom", isCorrect = true),
                    QuestionOption("c", "Dobrar a duração"),
                    QuestionOption("d", "Diminuir o volume")
                ),
                correctAnswerIndex = 1,
                explanation = "O sustenido eleva a nota em meio tom (1 semitom)"
            ),
            RichQuestion(
                id = id,
                type = QuestionType.THEORY_TEXT,
                text = "Qual é a relativa menor de Dó maior?",
                options = listOf(
                    QuestionOption("a", "Mi menor"),
                    QuestionOption("b", "Sol menor"),
                    QuestionOption("c", "Lá menor", isCorrect = true),
                    QuestionOption("d", "Ré menor")
                ),
                correctAnswerIndex = 2,
                explanation = "A relativa menor está uma terça menor abaixo (3 semitons)"
            ),
            RichQuestion(
                id = id,
                type = QuestionType.THEORY_TEXT,
                text = "O acorde maior é formado por:",
                options = listOf(
                    QuestionOption("a", "Tônica, 3ª menor, 5ª justa"),
                    QuestionOption("b", "Tônica, 3ª maior, 5ª justa", isCorrect = true),
                    QuestionOption("c", "Tônica, 3ª maior, 5ª diminuta"),
                    QuestionOption("d", "Tônica, 2ª maior, 5ª justa")
                ),
                correctAnswerIndex = 1,
                explanation = "O acorde maior = Fundamental + 3ª Maior (4 semitons) + 5ª Justa (7 semitons)"
            ),
            RichQuestion(
                id = id,
                type = QuestionType.THEORY_TEXT,
                text = "Quantas semicolcheias cabem em uma semínima?",
                options = listOf(
                    QuestionOption("a", "2"),
                    QuestionOption("b", "4", isCorrect = true),
                    QuestionOption("c", "8"),
                    QuestionOption("d", "16")
                ),
                correctAnswerIndex = 1,
                explanation = "Semínima (1 tempo) = 2 colcheias = 4 semicolcheias"
            )
        )
        return questions.random()
    }
    
    private fun generateNoteIdentificationQuestion(id: String, difficulty: Int): RichQuestion {
        val notes = when (difficulty) {
            1 -> listOf("C4", "D4", "E4", "F4", "G4") // Linhas e espaços básicos
            2 -> listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5")
            else -> listOf("C3", "D3", "E3", "F3", "G3", "A3", "B3", "C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5", "D5", "E5")
        }
        
        val targetNote = notes.random()
        val noteNames = mapOf(
            "C" to "Dó", "D" to "Ré", "E" to "Mi", "F" to "Fá",
            "G" to "Sol", "A" to "Lá", "B" to "Si"
        )
        val correctName = noteNames[targetNote.first().toString()] ?: "Dó"
        val wrongOptions = noteNames.values.filter { it != correctName }.shuffled().take(3)
        val allOptions = (listOf(correctName) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctName)
        
        return RichQuestion(
            id = id,
            type = QuestionType.NOTE_IDENTIFICATION,
            text = "Qual é o nome desta nota?",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(
                    id = index.toString(),
                    text = name,
                    isCorrect = name == correctName
                )
            },
            correctAnswerIndex = correctIndex,
            scoreData = ScoreQuestionData(
                clef = if (targetNote.last().digitToInt() < 4) "bass" else "treble",
                notes = listOf(ScoreNote(pitch = targetNote)),
                showNoteNames = false
            ),
            explanation = "Esta nota é $correctName"
        )
    }
    
    private fun generateIntervalQuestion(id: String, difficulty: Int): RichQuestion {
        val intervals = when (difficulty) {
            1 -> listOf(
                Triple("C4" to "E4", "3ª Maior", 4),
                Triple("C4" to "G4", "5ª Justa", 7),
                Triple("C4" to "C5", "8ª Justa", 12)
            )
            2 -> listOf(
                Triple("C4" to "D4", "2ª Maior", 2),
                Triple("C4" to "E4", "3ª Maior", 4),
                Triple("C4" to "F4", "4ª Justa", 5),
                Triple("C4" to "G4", "5ª Justa", 7),
                Triple("C4" to "A4", "6ª Maior", 9)
            )
            else -> listOf(
                Triple("C4" to "Db4", "2ª menor", 1),
                Triple("C4" to "D4", "2ª Maior", 2),
                Triple("C4" to "Eb4", "3ª menor", 3),
                Triple("C4" to "E4", "3ª Maior", 4),
                Triple("C4" to "F4", "4ª Justa", 5),
                Triple("C4" to "F#4", "4ª Aumentada", 6),
                Triple("C4" to "G4", "5ª Justa", 7),
                Triple("C4" to "Ab4", "6ª menor", 8),
                Triple("C4" to "A4", "6ª Maior", 9),
                Triple("C4" to "Bb4", "7ª menor", 10),
                Triple("C4" to "B4", "7ª Maior", 11)
            )
        }
        
        val (notesPair, correctInterval, _) = intervals.random()
        val wrongOptions = intervals.map { it.second }.filter { it != correctInterval }.shuffled().take(3)
        val allOptions = (listOf(correctInterval) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctInterval)
        
        return RichQuestion(
            id = id,
            type = QuestionType.INTERVAL_RECOGNITION,
            text = "🔊 Qual intervalo você ouviu?",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(index.toString(), name, name == correctInterval)
            },
            correctAnswerIndex = correctIndex,
            audioData = AudioQuestionData(
                type = AudioType.INTERVAL,
                notes = listOf(notesPair.first, notesPair.second),
                duration = 800
            ),
            scoreData = ScoreQuestionData(
                notes = listOf(
                    ScoreNote(pitch = notesPair.first),
                    ScoreNote(pitch = notesPair.second)
                )
            ),
            explanation = "Este é um intervalo de $correctInterval"
        )
    }
    
    private fun generateChordQuestion(id: String, difficulty: Int): RichQuestion {
        val chords = when (difficulty) {
            1 -> listOf(
                Triple("C Maior", listOf("C4", "E4", "G4"), "major"),
                Triple("C menor", listOf("C4", "Eb4", "G4"), "minor"),
                Triple("G Maior", listOf("G3", "B3", "D4"), "major")
            )
            else -> listOf(
                Triple("C Maior", listOf("C4", "E4", "G4"), "major"),
                Triple("C menor", listOf("C4", "Eb4", "G4"), "minor"),
                Triple("C Diminuto", listOf("C4", "Eb4", "Gb4"), "diminished"),
                Triple("C Aumentado", listOf("C4", "E4", "G#4"), "augmented"),
                Triple("C7", listOf("C4", "E4", "G4", "Bb4"), "dominant7")
            )
        }
        
        val (correctChord, notes, _) = chords.random()
        val wrongOptions = chords.map { it.first }.filter { it != correctChord }.shuffled().take(3)
        val allOptions = (listOf(correctChord) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctChord)
        
        return RichQuestion(
            id = id,
            type = QuestionType.CHORD_RECOGNITION,
            text = "🔊 Qual acorde você ouviu?",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(index.toString(), name, name == correctChord)
            },
            correctAnswerIndex = correctIndex,
            audioData = AudioQuestionData(
                type = AudioType.CHORD,
                notes = notes,
                duration = 1500
            ),
            explanation = "Este é um acorde de $correctChord"
        )
    }
    
    private fun generateRhythmQuestion(id: String, difficulty: Int): RichQuestion {
        val rhythms = listOf(
            "♩ ♩ ♩ ♩" to "4 semínimas",
            "♩ ♩ ♫" to "2 semínimas + 2 colcheias",
            "♫ ♫ ♩ ♩" to "4 colcheias + 2 semínimas",
            "𝅗𝅥 ♩ ♩" to "Mínima + 2 semínimas"
        )
        
        val (symbol, correctName) = rhythms.random()
        val wrongOptions = rhythms.map { it.second }.filter { it != correctName }.shuffled().take(3)
        val allOptions = (listOf(correctName) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctName)
        
        return RichQuestion(
            id = id,
            type = QuestionType.RHYTHM_IDENTIFICATION,
            text = "Qual é o padrão rítmico?\n\n$symbol",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(index.toString(), name, name == correctName)
            },
            correctAnswerIndex = correctIndex,
            explanation = "O padrão é: $correctName"
        )
    }
    
    private fun generateKeySignatureQuestion(id: String, difficulty: Int): RichQuestion {
        val keys = when (difficulty) {
            1 -> listOf(
                Triple("C", 0, "Dó Maior / Lá menor"),
                Triple("G", 1, "Sol Maior / Mi menor"),
                Triple("F", -1, "Fá Maior / Ré menor")
            )
            else -> listOf(
                Triple("C", 0, "Dó Maior / Lá menor"),
                Triple("G", 1, "Sol Maior / Mi menor"),
                Triple("D", 2, "Ré Maior / Si menor"),
                Triple("F", -1, "Fá Maior / Ré menor"),
                Triple("Bb", -2, "Sib Maior / Sol menor")
            )
        }
        
        val (key, accidentals, correctName) = keys.random()
        val accidentalText = when {
            accidentals == 0 -> "sem acidentes"
            accidentals > 0 -> "$accidentals sustenido(s)"
            else -> "${-accidentals} bemol(s)"
        }
        
        val wrongOptions = keys.map { it.third }.filter { it != correctName }.shuffled().take(3)
        val allOptions = (listOf(correctName) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctName)
        
        return RichQuestion(
            id = id,
            type = QuestionType.KEY_SIGNATURE,
            text = "Qual tonalidade tem $accidentalText?",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(index.toString(), name, name == correctName)
            },
            correctAnswerIndex = correctIndex,
            scoreData = ScoreQuestionData(keySignature = key),
            explanation = "Esta armadura representa $correctName"
        )
    }
    
    private fun generateTimeSignatureQuestion(id: String, difficulty: Int): RichQuestion {
        val signatures = listOf(
            "4/4" to "Quaternário simples (4 tempos)",
            "3/4" to "Ternário simples (3 tempos)",
            "2/4" to "Binário simples (2 tempos)",
            "6/8" to "Binário composto (2 tempos subdivididos em 3)"
        )
        
        val (signature, correctName) = signatures.random()
        val wrongOptions = signatures.map { it.second }.filter { it != correctName }.shuffled().take(3)
        val allOptions = (listOf(correctName) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctName)
        
        return RichQuestion(
            id = id,
            type = QuestionType.TIME_SIGNATURE,
            text = "O que significa o compasso $signature?",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(index.toString(), name, name == correctName)
            },
            correctAnswerIndex = correctIndex,
            scoreData = ScoreQuestionData(timeSignature = signature),
            explanation = "$signature = $correctName"
        )
    }
    
    private fun generateSymbolQuestion(id: String, difficulty: Int): RichQuestion {
        val symbols = listOf(
            "𝄞" to "Clave de Sol",
            "𝄢" to "Clave de Fá",
            "♯" to "Sustenido",
            "♭" to "Bemol",
            "♮" to "Bequadro",
            "𝄐" to "Fermata",
            "𝆏" to "Piano (suave)",
            "𝆑" to "Forte",
            "𝄎" to "Ligadura",
            "𝄻" to "Pausa de semínima"
        )
        
        val (symbol, correctName) = symbols.random()
        val wrongOptions = symbols.map { it.second }.filter { it != correctName }.shuffled().take(3)
        val allOptions = (listOf(correctName) + wrongOptions).shuffled()
        val correctIndex = allOptions.indexOf(correctName)
        
        return RichQuestion(
            id = id,
            type = QuestionType.SYMBOL_IDENTIFICATION,
            text = "O que significa este símbolo?\n\n$symbol",
            options = allOptions.mapIndexed { index, name ->
                QuestionOption(index.toString(), name, name == correctName)
            },
            correctAnswerIndex = correctIndex,
            explanation = "Este símbolo é: $correctName"
        )
    }
}
