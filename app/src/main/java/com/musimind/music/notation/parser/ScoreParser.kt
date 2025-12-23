package com.musimind.music.notation.parser

import com.musimind.music.notation.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON parser for musical scores
 * 
 * This allows defining musical scores in JSON format that can be stored in Firebase,
 * loaded from files, or defined inline in code.
 */
object ScoreParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    
    /**
     * Parse a JSON string into a Score object
     */
    fun parseScore(jsonString: String): Score {
        val jsonScore = json.decodeFromString<JsonScore>(jsonString)
        return jsonScore.toScore()
    }
    
    /**
     * Convert a Score to JSON string
     */
    fun scoreToJson(score: Score): String {
        val jsonScore = JsonScore.fromScore(score)
        return json.encodeToString(JsonScore.serializer(), jsonScore)
    }
}

/**
 * JSON representation of a score
 */
@Serializable
data class JsonScore(
    val id: String,
    val title: String,
    val composer: String? = null,
    val clef: String = "treble",
    val keySignature: String = "C",
    val timeSignature: String = "4/4",
    val tempo: Int? = null,
    val measures: List<JsonMeasure>
) {
    fun toScore(): Score {
        return Score(
            id = id,
            title = title,
            composer = composer,
            clef = parseClef(clef),
            keySignature = parseKeySignature(keySignature),
            timeSignature = parseTimeSignature(timeSignature),
            tempo = tempo,
            measures = measures.mapIndexed { index, m -> m.toMeasure(index + 1) }
        )
    }
    
    companion object {
        fun fromScore(score: Score): JsonScore {
            return JsonScore(
                id = score.id,
                title = score.title,
                composer = score.composer,
                clef = score.clef.name.lowercase(),
                keySignature = score.keySignature.name.replace("_", " "),
                timeSignature = "${score.timeSignature.numerator}/${score.timeSignature.denominator}",
                tempo = score.tempo,
                measures = score.measures.map { JsonMeasure.fromMeasure(it) }
            )
        }
    }
}

@Serializable
data class JsonMeasure(
    val elements: List<JsonElement>,
    val barline: String = "single"
) {
    fun toMeasure(number: Int): Measure {
        return Measure(
            number = number,
            elements = elements.map { it.toMusicElement() },
            barlineType = parseBarline(barline)
        )
    }
    
    companion object {
        fun fromMeasure(measure: Measure): JsonMeasure {
            return JsonMeasure(
                elements = measure.elements.map { JsonElement.fromMusicElement(it) },
                barline = measure.barlineType.name.lowercase()
            )
        }
    }
}

@Serializable
data class JsonElement(
    val type: String, // "note", "chord", "rest"
    val duration: Float,
    
    // For notes and chords
    val pitch: String? = null, // "C4", "F#5", "Bb3"
    val pitches: List<String>? = null, // For chords
    
    // Modifiers
    val dotted: Boolean = false,
    val doubleDotted: Boolean = false,
    val tied: Boolean = false,
    val slurred: Boolean = false,
    val beamGroup: Int? = null,
    
    // Articulations and ornaments
    val articulations: List<String>? = null,
    val dynamic: String? = null,
    val ornament: String? = null,
    
    // For feedback
    val state: String = "normal"
) {
    fun toMusicElement(): MusicElement {
        return when (type.lowercase()) {
            "note" -> Note(
                id = generateId(),
                durationBeats = duration,
                pitch = parsePitch(pitch ?: "C4"),
                accidental = parseAccidentalFromPitch(pitch ?: "C4"),
                dotted = dotted,
                doubleDotted = doubleDotted,
                tied = tied,
                slurred = slurred,
                beamGroup = beamGroup,
                articulations = articulations?.mapNotNull { parseArticulation(it) } ?: emptyList(),
                dynamic = dynamic?.let { parseDynamic(it) },
                ornament = ornament?.let { parseOrnament(it) },
                state = parseNoteState(state)
            )
            "chord" -> Chord(
                id = generateId(),
                durationBeats = duration,
                notes = pitches?.map { pitchStr ->
                    Note(
                        id = generateId(),
                        durationBeats = duration,
                        pitch = parsePitch(pitchStr),
                        accidental = parseAccidentalFromPitch(pitchStr),
                        state = parseNoteState(state)
                    )
                } ?: emptyList()
            )
            "rest" -> Rest(
                id = generateId(),
                durationBeats = duration,
                isWholeMeasure = duration >= 4f
            )
            else -> Rest(id = generateId(), durationBeats = duration)
        }
    }
    
    companion object {
        fun fromMusicElement(element: MusicElement): JsonElement {
            return when (element) {
                is Note -> JsonElement(
                    type = "note",
                    duration = element.durationBeats,
                    pitch = pitchToString(element.pitch),
                    dotted = element.dotted,
                    doubleDotted = element.doubleDotted,
                    tied = element.tied,
                    slurred = element.slurred,
                    beamGroup = element.beamGroup,
                    articulations = element.articulations.map { it.name.lowercase() },
                    dynamic = element.dynamic?.name?.lowercase(),
                    ornament = element.ornament?.name?.lowercase(),
                    state = element.state.name.lowercase()
                )
                is Chord -> JsonElement(
                    type = "chord",
                    duration = element.durationBeats,
                    pitches = element.notes.map { pitchToString(it.pitch) }
                )
                is Rest -> JsonElement(
                    type = "rest",
                    duration = element.durationBeats
                )
            }
        }
        
        private fun pitchToString(pitch: Pitch): String {
            val accidental = when (pitch.alteration) {
                1 -> "#"
                -1 -> "b"
                2 -> "##"
                -2 -> "bb"
                else -> ""
            }
            return "${pitch.note}$accidental${pitch.octave}"
        }
    }
}

// ============================================
// PARSING UTILITIES
// ============================================

private var idCounter = 0

private fun generateId(): String {
    return "elem_${idCounter++}_${System.currentTimeMillis()}"
}

/**
 * Parse pitch string like "C4", "F#5", "Bb3"
 */
private fun parsePitch(pitchStr: String): Pitch {
    val regex = Regex("([A-Ga-g])(##?|bb?)?([0-9])")
    val match = regex.find(pitchStr) ?: return Pitch(NoteName.C, 4)
    
    val (noteLetter, accidental, octave) = match.destructured
    
    val noteName = when (noteLetter.uppercase()) {
        "C" -> NoteName.C
        "D" -> NoteName.D
        "E" -> NoteName.E
        "F" -> NoteName.F
        "G" -> NoteName.G
        "A" -> NoteName.A
        "B" -> NoteName.B
        else -> NoteName.C
    }
    
    val alteration = when (accidental) {
        "#" -> 1
        "##" -> 2
        "b" -> -1
        "bb" -> -2
        else -> 0
    }
    
    return Pitch(
        note = noteName,
        octave = octave.toIntOrNull() ?: 4,
        alteration = alteration
    )
}

private fun parseAccidentalFromPitch(pitchStr: String): AccidentalType? {
    return when {
        pitchStr.contains("##") -> AccidentalType.DOUBLE_SHARP
        pitchStr.contains("#") -> AccidentalType.SHARP
        pitchStr.contains("bb") -> AccidentalType.DOUBLE_FLAT
        pitchStr.contains("b") && !pitchStr.startsWith("B") -> AccidentalType.FLAT
        else -> null
    }
}

private fun parseClef(clef: String): ClefType {
    return when (clef.lowercase()) {
        "treble", "g" -> ClefType.TREBLE
        "bass", "f" -> ClefType.BASS
        "alto", "c" -> ClefType.ALTO
        "tenor" -> ClefType.TENOR
        "percussion" -> ClefType.PERCUSSION
        else -> ClefType.TREBLE
    }
}

private fun parseKeySignature(key: String): KeySignatureType {
    return when (key.uppercase().replace(" ", "_")) {
        "C", "C_MAJOR", "A_MINOR" -> KeySignatureType.C_MAJOR
        "G", "G_MAJOR", "E_MINOR" -> KeySignatureType.G_MAJOR
        "D", "D_MAJOR", "B_MINOR" -> KeySignatureType.D_MAJOR
        "A", "A_MAJOR", "F#_MINOR" -> KeySignatureType.A_MAJOR
        "E", "E_MAJOR", "C#_MINOR" -> KeySignatureType.E_MAJOR
        "B", "B_MAJOR", "G#_MINOR" -> KeySignatureType.B_MAJOR
        "F#", "F#_MAJOR" -> KeySignatureType.F_SHARP_MAJOR
        "C#", "C#_MAJOR" -> KeySignatureType.C_SHARP_MAJOR
        "F", "F_MAJOR", "D_MINOR" -> KeySignatureType.F_MAJOR
        "BB", "BB_MAJOR", "G_MINOR" -> KeySignatureType.B_FLAT_MAJOR
        "EB", "EB_MAJOR", "C_MINOR" -> KeySignatureType.E_FLAT_MAJOR
        "AB", "AB_MAJOR", "F_MINOR" -> KeySignatureType.A_FLAT_MAJOR
        "DB", "DB_MAJOR", "BB_MINOR" -> KeySignatureType.D_FLAT_MAJOR
        "GB", "GB_MAJOR", "EB_MINOR" -> KeySignatureType.G_FLAT_MAJOR
        "CB", "CB_MAJOR", "AB_MINOR" -> KeySignatureType.C_FLAT_MAJOR
        else -> KeySignatureType.C_MAJOR
    }
}

private fun parseTimeSignature(ts: String): TimeSignatureModel {
    val parts = ts.split("/")
    return if (parts.size == 2) {
        TimeSignatureModel(
            numerator = parts[0].toIntOrNull() ?: 4,
            denominator = parts[1].toIntOrNull() ?: 4
        )
    } else {
        TimeSignatureModel(4, 4)
    }
}

private fun parseBarline(barline: String): BarlineType {
    return when (barline.lowercase()) {
        "single" -> BarlineType.SINGLE
        "double" -> BarlineType.DOUBLE
        "final" -> BarlineType.FINAL
        "repeat_left" -> BarlineType.REPEAT_LEFT
        "repeat_right" -> BarlineType.REPEAT_RIGHT
        "repeat_both" -> BarlineType.REPEAT_BOTH
        else -> BarlineType.SINGLE
    }
}

private fun parseArticulation(art: String): ArticulationType? {
    return when (art.lowercase()) {
        "staccato" -> ArticulationType.STACCATO
        "staccatissimo" -> ArticulationType.STACCATISSIMO
        "accent" -> ArticulationType.ACCENT
        "tenuto" -> ArticulationType.TENUTO
        "marcato" -> ArticulationType.MARCATO
        "fermata" -> ArticulationType.FERMATA
        else -> null
    }
}

private fun parseDynamic(dyn: String): DynamicType? {
    return when (dyn.lowercase()) {
        "pp" -> DynamicType.PP
        "p" -> DynamicType.P
        "mp" -> DynamicType.MP
        "mf" -> DynamicType.MF
        "f" -> DynamicType.F
        "ff" -> DynamicType.FF
        "sf" -> DynamicType.SF
        "fp" -> DynamicType.FP
        else -> null
    }
}

private fun parseOrnament(orn: String): OrnamentType? {
    return when (orn.lowercase()) {
        "trill" -> OrnamentType.TRILL
        "mordent" -> OrnamentType.MORDENT
        "inverted_mordent" -> OrnamentType.INVERTED_MORDENT
        "turn" -> OrnamentType.TURN
        "inverted_turn" -> OrnamentType.INVERTED_TURN
        else -> null
    }
}

private fun parseNoteState(state: String): NoteState {
    return when (state.lowercase()) {
        "normal" -> NoteState.NORMAL
        "highlighted" -> NoteState.HIGHLIGHTED
        "correct" -> NoteState.CORRECT
        "incorrect" -> NoteState.INCORRECT
        "upcoming" -> NoteState.UPCOMING
        "passed" -> NoteState.PASSED
        else -> NoteState.NORMAL
    }
}
