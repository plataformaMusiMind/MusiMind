package com.musimind.domain.locale

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider for music-specific terminology
 * Uses pedagogically correct terms for each tradition
 * 
 * NOT literal translations - culturally appropriate terms
 */
@Singleton
class MusicTerminologyProvider @Inject constructor(
    private val localeManager: LocaleManager
) {
    
    /**
     * Get note name by MIDI-style index (0-11)
     */
    fun getNoteName(noteIndex: Int, withOctave: Int? = null): String {
        val notes = localeManager.getChromaticNotes()
        val noteName = notes[noteIndex % 12]
        return if (withOctave != null) "$noteName$withOctave" else noteName
    }
    
    /**
     * Get solfege syllable (Do, Re, Mi or C, D, E)
     */
    fun getSolfegeSyllable(scaleDegree: Int): String {
        val notes = localeManager.getNoteNames()
        return notes[(scaleDegree - 1) % 7]
    }
    
    /**
     * Get interval name
     */
    fun getIntervalName(semitones: Int): String {
        val lang = localeManager.getCurrentLanguageSync()
        return when (lang) {
            AppLanguage.PORTUGUESE_BR -> getIntervalPtBr(semitones)
            AppLanguage.ENGLISH_US -> getIntervalEnUs(semitones)
            AppLanguage.SPANISH -> getIntervalEsEs(semitones)
            AppLanguage.GERMAN -> getIntervalDeDe(semitones)
            AppLanguage.FRENCH -> getIntervalFrFr(semitones)
            AppLanguage.CHINESE_SIMPLIFIED -> getIntervalZhCn(semitones)
        }
    }
    
    private fun getIntervalPtBr(semitones: Int): String = when (semitones) {
        0 -> "Uníssono"
        1 -> "Segunda menor"
        2 -> "Segunda maior"
        3 -> "Terça menor"
        4 -> "Terça maior"
        5 -> "Quarta justa"
        6 -> "Trítono"
        7 -> "Quinta justa"
        8 -> "Sexta menor"
        9 -> "Sexta maior"
        10 -> "Sétima menor"
        11 -> "Sétima maior"
        12 -> "Oitava"
        else -> "Intervalo composto"
    }
    
    private fun getIntervalEnUs(semitones: Int): String = when (semitones) {
        0 -> "Unison"
        1 -> "Minor 2nd"
        2 -> "Major 2nd"
        3 -> "Minor 3rd"
        4 -> "Major 3rd"
        5 -> "Perfect 4th"
        6 -> "Tritone"
        7 -> "Perfect 5th"
        8 -> "Minor 6th"
        9 -> "Major 6th"
        10 -> "Minor 7th"
        11 -> "Major 7th"
        12 -> "Octave"
        else -> "Compound interval"
    }
    
    private fun getIntervalEsEs(semitones: Int): String = when (semitones) {
        0 -> "Unísono"
        1 -> "Segunda menor"
        2 -> "Segunda mayor"
        3 -> "Tercera menor"
        4 -> "Tercera mayor"
        5 -> "Cuarta justa"
        6 -> "Tritono"
        7 -> "Quinta justa"
        8 -> "Sexta menor"
        9 -> "Sexta mayor"
        10 -> "Séptima menor"
        11 -> "Séptima mayor"
        12 -> "Octava"
        else -> "Intervalo compuesto"
    }
    
    private fun getIntervalDeDe(semitones: Int): String = when (semitones) {
        0 -> "Prime"
        1 -> "Kleine Sekunde"
        2 -> "Große Sekunde"
        3 -> "Kleine Terz"
        4 -> "Große Terz"
        5 -> "Reine Quarte"
        6 -> "Tritonus"
        7 -> "Reine Quinte"
        8 -> "Kleine Sexte"
        9 -> "Große Sexte"
        10 -> "Kleine Septime"
        11 -> "Große Septime"
        12 -> "Oktave"
        else -> "Zusammengesetztes Intervall"
    }
    
    private fun getIntervalFrFr(semitones: Int): String = when (semitones) {
        0 -> "Unisson"
        1 -> "Seconde mineure"
        2 -> "Seconde majeure"
        3 -> "Tierce mineure"
        4 -> "Tierce majeure"
        5 -> "Quarte juste"
        6 -> "Triton"
        7 -> "Quinte juste"
        8 -> "Sixte mineure"
        9 -> "Sixte majeure"
        10 -> "Septième mineure"
        11 -> "Septième majeure"
        12 -> "Octave"
        else -> "Intervalle composé"
    }
    
    private fun getIntervalZhCn(semitones: Int): String = when (semitones) {
        0 -> "同度"
        1 -> "小二度"
        2 -> "大二度"
        3 -> "小三度"
        4 -> "大三度"
        5 -> "纯四度"
        6 -> "增四度"
        7 -> "纯五度"
        8 -> "小六度"
        9 -> "大六度"
        10 -> "小七度"
        11 -> "大七度"
        12 -> "八度"
        else -> "复合音程"
    }
    
    /**
     * Get rhythm/note duration name
     */
    fun getRhythmName(duration: RhythmDuration): String {
        val lang = localeManager.getCurrentLanguageSync()
        return when (lang) {
            AppLanguage.PORTUGUESE_BR -> getRhythmPtBr(duration)
            AppLanguage.ENGLISH_US -> getRhythmEnUs(duration)
            AppLanguage.SPANISH -> getRhythmEsEs(duration)
            AppLanguage.GERMAN -> getRhythmDeDe(duration)
            AppLanguage.FRENCH -> getRhythmFrFr(duration)
            AppLanguage.CHINESE_SIMPLIFIED -> getRhythmZhCn(duration)
        }
    }
    
    private fun getRhythmPtBr(duration: RhythmDuration): String = when (duration) {
        RhythmDuration.WHOLE -> "Semibreve"
        RhythmDuration.HALF -> "Mínima"
        RhythmDuration.QUARTER -> "Semínima"
        RhythmDuration.EIGHTH -> "Colcheia"
        RhythmDuration.SIXTEENTH -> "Semicolcheia"
        RhythmDuration.THIRTY_SECOND -> "Fusa"
        RhythmDuration.SIXTY_FOURTH -> "Semifusa"
        RhythmDuration.DOTTED_HALF -> "Mínima pontuada"
        RhythmDuration.DOTTED_QUARTER -> "Semínima pontuada"
        RhythmDuration.DOTTED_EIGHTH -> "Colcheia pontuada"
    }
    
    private fun getRhythmEnUs(duration: RhythmDuration): String = when (duration) {
        RhythmDuration.WHOLE -> "Whole note"
        RhythmDuration.HALF -> "Half note"
        RhythmDuration.QUARTER -> "Quarter note"
        RhythmDuration.EIGHTH -> "Eighth note"
        RhythmDuration.SIXTEENTH -> "Sixteenth note"
        RhythmDuration.THIRTY_SECOND -> "Thirty-second note"
        RhythmDuration.SIXTY_FOURTH -> "Sixty-fourth note"
        RhythmDuration.DOTTED_HALF -> "Dotted half note"
        RhythmDuration.DOTTED_QUARTER -> "Dotted quarter note"
        RhythmDuration.DOTTED_EIGHTH -> "Dotted eighth note"
    }
    
    private fun getRhythmEsEs(duration: RhythmDuration): String = when (duration) {
        RhythmDuration.WHOLE -> "Redonda"
        RhythmDuration.HALF -> "Blanca"
        RhythmDuration.QUARTER -> "Negra"
        RhythmDuration.EIGHTH -> "Corchea"
        RhythmDuration.SIXTEENTH -> "Semicorchea"
        RhythmDuration.THIRTY_SECOND -> "Fusa"
        RhythmDuration.SIXTY_FOURTH -> "Semifusa"
        RhythmDuration.DOTTED_HALF -> "Blanca con puntillo"
        RhythmDuration.DOTTED_QUARTER -> "Negra con puntillo"
        RhythmDuration.DOTTED_EIGHTH -> "Corchea con puntillo"
    }
    
    private fun getRhythmDeDe(duration: RhythmDuration): String = when (duration) {
        RhythmDuration.WHOLE -> "Ganze Note"
        RhythmDuration.HALF -> "Halbe Note"
        RhythmDuration.QUARTER -> "Viertelnote"
        RhythmDuration.EIGHTH -> "Achtelnote"
        RhythmDuration.SIXTEENTH -> "Sechzehntelnote"
        RhythmDuration.THIRTY_SECOND -> "Zweiunddreißigstelnote"
        RhythmDuration.SIXTY_FOURTH -> "Vierundsechzigstelnote"
        RhythmDuration.DOTTED_HALF -> "Punktierte halbe Note"
        RhythmDuration.DOTTED_QUARTER -> "Punktierte Viertelnote"
        RhythmDuration.DOTTED_EIGHTH -> "Punktierte Achtelnote"
    }
    
    private fun getRhythmFrFr(duration: RhythmDuration): String = when (duration) {
        RhythmDuration.WHOLE -> "Ronde"
        RhythmDuration.HALF -> "Blanche"
        RhythmDuration.QUARTER -> "Noire"
        RhythmDuration.EIGHTH -> "Croche"
        RhythmDuration.SIXTEENTH -> "Double croche"
        RhythmDuration.THIRTY_SECOND -> "Triple croche"
        RhythmDuration.SIXTY_FOURTH -> "Quadruple croche"
        RhythmDuration.DOTTED_HALF -> "Blanche pointée"
        RhythmDuration.DOTTED_QUARTER -> "Noire pointée"
        RhythmDuration.DOTTED_EIGHTH -> "Croche pointée"
    }
    
    private fun getRhythmZhCn(duration: RhythmDuration): String = when (duration) {
        RhythmDuration.WHOLE -> "全音符"
        RhythmDuration.HALF -> "二分音符"
        RhythmDuration.QUARTER -> "四分音符"
        RhythmDuration.EIGHTH -> "八分音符"
        RhythmDuration.SIXTEENTH -> "十六分音符"
        RhythmDuration.THIRTY_SECOND -> "三十二分音符"
        RhythmDuration.SIXTY_FOURTH -> "六十四分音符"
        RhythmDuration.DOTTED_HALF -> "附点二分音符"
        RhythmDuration.DOTTED_QUARTER -> "附点四分音符"
        RhythmDuration.DOTTED_EIGHTH -> "附点八分音符"
    }
    
    /**
     * Get chord type name
     */
    fun getChordTypeName(chordType: ChordType): String {
        val lang = localeManager.getCurrentLanguageSync()
        return when (lang) {
            AppLanguage.PORTUGUESE_BR -> getChordPtBr(chordType)
            AppLanguage.ENGLISH_US -> getChordEnUs(chordType)
            AppLanguage.SPANISH -> getChordEsEs(chordType)
            AppLanguage.GERMAN -> getChordDeDe(chordType)
            AppLanguage.FRENCH -> getChordFrFr(chordType)
            AppLanguage.CHINESE_SIMPLIFIED -> getChordZhCn(chordType)
        }
    }
    
    private fun getChordPtBr(type: ChordType): String = when (type) {
        ChordType.MAJOR -> "Maior"
        ChordType.MINOR -> "Menor"
        ChordType.DIMINISHED -> "Diminuto"
        ChordType.AUGMENTED -> "Aumentado"
        ChordType.MAJOR_SEVENTH -> "Maior com sétima maior"
        ChordType.MINOR_SEVENTH -> "Menor com sétima"
        ChordType.DOMINANT_SEVENTH -> "Dominante"
        ChordType.SUSPENDED_SECOND -> "Suspenso na segunda"
        ChordType.SUSPENDED_FOURTH -> "Suspenso na quarta"
    }
    
    private fun getChordEnUs(type: ChordType): String = when (type) {
        ChordType.MAJOR -> "Major"
        ChordType.MINOR -> "Minor"
        ChordType.DIMINISHED -> "Diminished"
        ChordType.AUGMENTED -> "Augmented"
        ChordType.MAJOR_SEVENTH -> "Major 7th"
        ChordType.MINOR_SEVENTH -> "Minor 7th"
        ChordType.DOMINANT_SEVENTH -> "Dominant 7th"
        ChordType.SUSPENDED_SECOND -> "Sus2"
        ChordType.SUSPENDED_FOURTH -> "Sus4"
    }
    
    private fun getChordEsEs(type: ChordType): String = when (type) {
        ChordType.MAJOR -> "Mayor"
        ChordType.MINOR -> "Menor"
        ChordType.DIMINISHED -> "Disminuido"
        ChordType.AUGMENTED -> "Aumentado"
        ChordType.MAJOR_SEVENTH -> "Mayor séptima mayor"
        ChordType.MINOR_SEVENTH -> "Menor séptima"
        ChordType.DOMINANT_SEVENTH -> "Dominante"
        ChordType.SUSPENDED_SECOND -> "Suspendido segunda"
        ChordType.SUSPENDED_FOURTH -> "Suspendido cuarta"
    }
    
    private fun getChordDeDe(type: ChordType): String = when (type) {
        ChordType.MAJOR -> "Dur"
        ChordType.MINOR -> "Moll"
        ChordType.DIMINISHED -> "Vermindert"
        ChordType.AUGMENTED -> "Übermäßig"
        ChordType.MAJOR_SEVENTH -> "Maj7"
        ChordType.MINOR_SEVENTH -> "Moll7"
        ChordType.DOMINANT_SEVENTH -> "Dominantseptakkord"
        ChordType.SUSPENDED_SECOND -> "Sus2"
        ChordType.SUSPENDED_FOURTH -> "Sus4"
    }
    
    private fun getChordFrFr(type: ChordType): String = when (type) {
        ChordType.MAJOR -> "Majeur"
        ChordType.MINOR -> "Mineur"
        ChordType.DIMINISHED -> "Diminué"
        ChordType.AUGMENTED -> "Augmenté"
        ChordType.MAJOR_SEVENTH -> "Majeur 7"
        ChordType.MINOR_SEVENTH -> "Mineur 7"
        ChordType.DOMINANT_SEVENTH -> "7ème de dominante"
        ChordType.SUSPENDED_SECOND -> "Suspendu 2"
        ChordType.SUSPENDED_FOURTH -> "Suspendu 4"
    }
    
    private fun getChordZhCn(type: ChordType): String = when (type) {
        ChordType.MAJOR -> "大三和弦"
        ChordType.MINOR -> "小三和弦"
        ChordType.DIMINISHED -> "减三和弦"
        ChordType.AUGMENTED -> "增三和弦"
        ChordType.MAJOR_SEVENTH -> "大七和弦"
        ChordType.MINOR_SEVENTH -> "小七和弦"
        ChordType.DOMINANT_SEVENTH -> "属七和弦"
        ChordType.SUSPENDED_SECOND -> "挂二和弦"
        ChordType.SUSPENDED_FOURTH -> "挂四和弦"
    }
    
    /**
     * Get scale name
     */
    fun getScaleName(scaleType: ScaleType): String {
        val lang = localeManager.getCurrentLanguageSync()
        return when (lang) {
            AppLanguage.PORTUGUESE_BR -> getScalePtBr(scaleType)
            AppLanguage.ENGLISH_US -> getScaleEnUs(scaleType)
            AppLanguage.SPANISH -> getScaleEsEs(scaleType)
            AppLanguage.GERMAN -> getScaleDeDe(scaleType)
            AppLanguage.FRENCH -> getScaleFrFr(scaleType)
            AppLanguage.CHINESE_SIMPLIFIED -> getScaleZhCn(scaleType)
        }
    }
    
    private fun getScalePtBr(type: ScaleType): String = when (type) {
        ScaleType.MAJOR -> "Escala maior"
        ScaleType.NATURAL_MINOR -> "Escala menor natural"
        ScaleType.HARMONIC_MINOR -> "Escala menor harmônica"
        ScaleType.MELODIC_MINOR -> "Escala menor melódica"
        ScaleType.PENTATONIC_MAJOR -> "Pentatônica maior"
        ScaleType.PENTATONIC_MINOR -> "Pentatônica menor"
        ScaleType.CHROMATIC -> "Escala cromática"
        ScaleType.BLUES -> "Escala blues"
    }
    
    private fun getScaleEnUs(type: ScaleType): String = when (type) {
        ScaleType.MAJOR -> "Major scale"
        ScaleType.NATURAL_MINOR -> "Natural minor scale"
        ScaleType.HARMONIC_MINOR -> "Harmonic minor scale"
        ScaleType.MELODIC_MINOR -> "Melodic minor scale"
        ScaleType.PENTATONIC_MAJOR -> "Major pentatonic"
        ScaleType.PENTATONIC_MINOR -> "Minor pentatonic"
        ScaleType.CHROMATIC -> "Chromatic scale"
        ScaleType.BLUES -> "Blues scale"
    }
    
    private fun getScaleEsEs(type: ScaleType): String = when (type) {
        ScaleType.MAJOR -> "Escala mayor"
        ScaleType.NATURAL_MINOR -> "Escala menor natural"
        ScaleType.HARMONIC_MINOR -> "Escala menor armónica"
        ScaleType.MELODIC_MINOR -> "Escala menor melódica"
        ScaleType.PENTATONIC_MAJOR -> "Pentatónica mayor"
        ScaleType.PENTATONIC_MINOR -> "Pentatónica menor"
        ScaleType.CHROMATIC -> "Escala cromática"
        ScaleType.BLUES -> "Escala de blues"
    }
    
    private fun getScaleDeDe(type: ScaleType): String = when (type) {
        ScaleType.MAJOR -> "Dur-Tonleiter"
        ScaleType.NATURAL_MINOR -> "Natürliche Moll-Tonleiter"
        ScaleType.HARMONIC_MINOR -> "Harmonische Moll-Tonleiter"
        ScaleType.MELODIC_MINOR -> "Melodische Moll-Tonleiter"
        ScaleType.PENTATONIC_MAJOR -> "Dur-Pentatonik"
        ScaleType.PENTATONIC_MINOR -> "Moll-Pentatonik"
        ScaleType.CHROMATIC -> "Chromatische Tonleiter"
        ScaleType.BLUES -> "Blues-Tonleiter"
    }
    
    private fun getScaleFrFr(type: ScaleType): String = when (type) {
        ScaleType.MAJOR -> "Gamme majeure"
        ScaleType.NATURAL_MINOR -> "Gamme mineure naturelle"
        ScaleType.HARMONIC_MINOR -> "Gamme mineure harmonique"
        ScaleType.MELODIC_MINOR -> "Gamme mineure mélodique"
        ScaleType.PENTATONIC_MAJOR -> "Pentatonique majeure"
        ScaleType.PENTATONIC_MINOR -> "Pentatonique mineure"
        ScaleType.CHROMATIC -> "Gamme chromatique"
        ScaleType.BLUES -> "Gamme blues"
    }
    
    private fun getScaleZhCn(type: ScaleType): String = when (type) {
        ScaleType.MAJOR -> "大调音阶"
        ScaleType.NATURAL_MINOR -> "自然小调音阶"
        ScaleType.HARMONIC_MINOR -> "和声小调音阶"
        ScaleType.MELODIC_MINOR -> "旋律小调音阶"
        ScaleType.PENTATONIC_MAJOR -> "大调五声音阶"
        ScaleType.PENTATONIC_MINOR -> "小调五声音阶"
        ScaleType.CHROMATIC -> "半音音阶"
        ScaleType.BLUES -> "布鲁斯音阶"
    }
    
    /**
     * Get dynamic marking name
     */
    fun getDynamicName(dynamic: Dynamic): String {
        // Dynamics are universal (Italian terms), but we provide translations
        val lang = localeManager.getCurrentLanguageSync()
        return when (lang) {
            AppLanguage.PORTUGUESE_BR, AppLanguage.SPANISH, AppLanguage.FRENCH -> 
                "${dynamic.italian} (${dynamic.italianFull})"
            AppLanguage.ENGLISH_US -> 
                "${dynamic.italian} (${getEnglishDynamic(dynamic)})"
            AppLanguage.GERMAN -> 
                "${dynamic.italian} (${getGermanDynamic(dynamic)})"
            AppLanguage.CHINESE_SIMPLIFIED -> 
                "${dynamic.italian} (${getChineseDynamic(dynamic)})"
        }
    }
    
    private fun getEnglishDynamic(d: Dynamic) = when (d) {
        Dynamic.PIANISSIMO -> "very soft"
        Dynamic.PIANO -> "soft"
        Dynamic.MEZZO_PIANO -> "moderately soft"
        Dynamic.MEZZO_FORTE -> "moderately loud"
        Dynamic.FORTE -> "loud"
        Dynamic.FORTISSIMO -> "very loud"
    }
    
    private fun getGermanDynamic(d: Dynamic) = when (d) {
        Dynamic.PIANISSIMO -> "sehr leise"
        Dynamic.PIANO -> "leise"
        Dynamic.MEZZO_PIANO -> "mittelleise"
        Dynamic.MEZZO_FORTE -> "mittellaut"
        Dynamic.FORTE -> "laut"
        Dynamic.FORTISSIMO -> "sehr laut"
    }
    
    private fun getChineseDynamic(d: Dynamic) = when (d) {
        Dynamic.PIANISSIMO -> "很弱"
        Dynamic.PIANO -> "弱"
        Dynamic.MEZZO_PIANO -> "中弱"
        Dynamic.MEZZO_FORTE -> "中强"
        Dynamic.FORTE -> "强"
        Dynamic.FORTISSIMO -> "很强"
    }
}

/**
 * Note duration types
 */
enum class RhythmDuration {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH,
    SIXTEENTH,
    THIRTY_SECOND,
    SIXTY_FOURTH,
    DOTTED_HALF,
    DOTTED_QUARTER,
    DOTTED_EIGHTH
}

/**
 * Chord types
 */
enum class ChordType {
    MAJOR,
    MINOR,
    DIMINISHED,
    AUGMENTED,
    MAJOR_SEVENTH,
    MINOR_SEVENTH,
    DOMINANT_SEVENTH,
    SUSPENDED_SECOND,
    SUSPENDED_FOURTH
}

/**
 * Scale types
 */
enum class ScaleType {
    MAJOR,
    NATURAL_MINOR,
    HARMONIC_MINOR,
    MELODIC_MINOR,
    PENTATONIC_MAJOR,
    PENTATONIC_MINOR,
    CHROMATIC,
    BLUES
}

/**
 * Dynamic markings
 */
enum class Dynamic(val italian: String, val italianFull: String) {
    PIANISSIMO("pp", "pianissimo"),
    PIANO("p", "piano"),
    MEZZO_PIANO("mp", "mezzo piano"),
    MEZZO_FORTE("mf", "mezzo forte"),
    FORTE("f", "forte"),
    FORTISSIMO("ff", "fortissimo")
}
