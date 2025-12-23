package com.musimind.music.notation.smufl

/**
 * SMuFL (Standard Music Font Layout) Glyph Mappings
 * 
 * Complete mapping of Unicode codepoints for SMuFL-compliant fonts like Bravura.
 * Reference: https://w3c.github.io/smufl/latest/
 * 
 * These codepoints are in the Private Use Area (PUA) starting at U+E000
 */
object SMuFLGlyphs {

    // ============================================
    // CLEFS (U+E050 - U+E07F)
    // ============================================
    object Clefs {
        const val TREBLE = '\uE050'           // Clave de Sol
        const val TREBLE_8VB = '\uE052'       // Clave de Sol 8vb
        const val TREBLE_8VA = '\uE053'       // Clave de Sol 8va
        const val TREBLE_15MB = '\uE051'      // Clave de Sol 15mb
        const val TREBLE_15MA = '\uE054'      // Clave de Sol 15ma
        
        const val BASS = '\uE062'             // Clave de Fá
        const val BASS_8VB = '\uE064'         // Clave de Fá 8vb
        const val BASS_8VA = '\uE065'         // Clave de Fá 8va
        const val BASS_15MB = '\uE063'        // Clave de Fá 15mb
        const val BASS_15MA = '\uE066'        // Clave de Fá 15ma
        
        const val ALTO = '\uE05C'             // Clave de Dó (Alto)
        const val TENOR = '\uE05C'            // Clave de Dó (Tenor - same glyph, different position)
        
        const val PERCUSSION = '\uE069'       // Clave de Percussão
        const val TAB = '\uE06D'              // Clave de Tablatura
    }

    // ============================================
    // NOTEHEADS (U+E0A0 - U+E0FF)
    // ============================================
    object Noteheads {
        // Standard noteheads
        const val WHOLE = '\uE0A2'            // Semibreve
        const val HALF = '\uE0A3'             // Mínima
        const val BLACK = '\uE0A4'            // Semínima e menores
        const val DOUBLE_WHOLE = '\uE0A0'     // Breve
        
        // Parenthesized noteheads
        const val WHOLE_PAREN = '\uE0A5'
        const val HALF_PAREN = '\uE0A6'
        const val BLACK_PAREN = '\uE0A7'
        
        // X noteheads (for percussion)
        const val X_WHOLE = '\uE0B0'
        const val X_HALF = '\uE0B1'
        const val X_BLACK = '\uE0B2'
        
        // Diamond noteheads (for harmonics)
        const val DIAMOND_WHOLE = '\uE0D8'
        const val DIAMOND_HALF = '\uE0D9'
        const val DIAMOND_BLACK = '\uE0DA'
        
        // Triangle noteheads
        const val TRIANGLE_UP_WHOLE = '\uE0BC'
        const val TRIANGLE_UP_HALF = '\uE0BD'
        const val TRIANGLE_UP_BLACK = '\uE0BE'
        const val TRIANGLE_DOWN_BLACK = '\uE0C7'
        
        // Slash noteheads (for rhythm notation)
        const val SLASH_WHOLE = '\uE100'
        const val SLASH_HALF = '\uE101'
        const val SLASH_BLACK = '\uE102'
    }

    // ============================================
    // FLAGS (U+E240 - U+E25F)
    // ============================================
    object Flags {
        // Stem up flags
        const val EIGHTH_UP = '\uE240'        // Colcheia
        const val SIXTEENTH_UP = '\uE242'     // Semicolcheia
        const val THIRTY_SECOND_UP = '\uE244' // Fusa
        const val SIXTY_FOURTH_UP = '\uE246'  // Semifusa
        const val ONE_TWENTY_EIGHTH_UP = '\uE248'
        const val TWO_FIFTY_SIXTH_UP = '\uE24A'
        
        // Stem down flags
        const val EIGHTH_DOWN = '\uE241'
        const val SIXTEENTH_DOWN = '\uE243'
        const val THIRTY_SECOND_DOWN = '\uE245'
        const val SIXTY_FOURTH_DOWN = '\uE247'
        const val ONE_TWENTY_EIGHTH_DOWN = '\uE249'
        const val TWO_FIFTY_SIXTH_DOWN = '\uE24B'
        
        // Internal flags (for beamed notes)
        const val INTERNAL_UP = '\uE250'
        const val INTERNAL_DOWN = '\uE251'
    }

    // ============================================
    // RESTS (U+E4E0 - U+E4FF)
    // ============================================
    object Rests {
        const val MAXIMA = '\uE4E0'
        const val LONGA = '\uE4E1'
        const val DOUBLE_WHOLE = '\uE4E2'     // Pausa de breve
        const val WHOLE = '\uE4E3'            // Pausa de semibreve
        const val HALF = '\uE4E4'             // Pausa de mínima
        const val QUARTER = '\uE4E5'          // Pausa de semínima
        const val EIGHTH = '\uE4E6'           // Pausa de colcheia
        const val SIXTEENTH = '\uE4E7'        // Pausa de semicolcheia
        const val THIRTY_SECOND = '\uE4E8'    // Pausa de fusa
        const val SIXTY_FOURTH = '\uE4E9'     // Pausa de semifusa
        const val ONE_TWENTY_EIGHTH = '\uE4EA'
        const val TWO_FIFTY_SIXTH = '\uE4EB'
    }

    // ============================================
    // ACCIDENTALS (U+E260 - U+E2FF)
    // ============================================
    object Accidentals {
        const val FLAT = '\uE260'             // Bemol
        const val NATURAL = '\uE261'          // Bequadro
        const val SHARP = '\uE262'            // Sustenido
        const val DOUBLE_SHARP = '\uE263'     // Dobrado sustenido
        const val DOUBLE_FLAT = '\uE264'      // Dobrado bemol
        const val TRIPLE_SHARP = '\uE265'
        const val TRIPLE_FLAT = '\uE266'
        
        // Quarter-tone accidentals
        const val QUARTER_FLAT = '\uE280'
        const val QUARTER_SHARP = '\uE282'
        const val THREE_QUARTER_FLAT = '\uE281'
        const val THREE_QUARTER_SHARP = '\uE283'
        
        // Parenthesized accidentals
        const val FLAT_PAREN = '\uE26A'
        const val NATURAL_PAREN = '\uE26B'
        const val SHARP_PAREN = '\uE26C'
        
        // Bracketed accidentals
        const val FLAT_BRACKET = '\uE26D'
        const val NATURAL_BRACKET = '\uE26E'
        const val SHARP_BRACKET = '\uE26F'
    }

    // ============================================
    // TIME SIGNATURES (U+E080 - U+E09F)
    // ============================================
    object TimeSignatures {
        const val COMMON = '\uE08A'           // Compasso C (4/4)
        const val CUT = '\uE08B'              // Compasso C cortado (2/2)
        
        // Time signature numerals
        const val NUM_0 = '\uE080'
        const val NUM_1 = '\uE081'
        const val NUM_2 = '\uE082'
        const val NUM_3 = '\uE083'
        const val NUM_4 = '\uE084'
        const val NUM_5 = '\uE085'
        const val NUM_6 = '\uE086'
        const val NUM_7 = '\uE087'
        const val NUM_8 = '\uE088'
        const val NUM_9 = '\uE089'
        
        fun getNumeral(digit: Int): Char = when (digit) {
            0 -> NUM_0
            1 -> NUM_1
            2 -> NUM_2
            3 -> NUM_3
            4 -> NUM_4
            5 -> NUM_5
            6 -> NUM_6
            7 -> NUM_7
            8 -> NUM_8
            9 -> NUM_9
            else -> NUM_0
        }
    }

    // ============================================
    // ARTICULATIONS (U+E4A0 - U+E4BF)
    // ============================================
    object Articulations {
        const val ACCENT = '\uE4A0'           // Acento
        const val STACCATO = '\uE4A2'         // Staccato
        const val STACCATISSIMO = '\uE4A6'    // Staccatissimo
        const val TENUTO = '\uE4A4'           // Tenuto
        const val MARCATO = '\uE4AC'          // Marcato
        const val ACCENT_STACCATO = '\uE4B0'
        const val TENUTO_STACCATO = '\uE4B2'
        const val MARCATO_STACCATO = '\uE4AE'
        
        // Bowing marks
        const val UP_BOW = '\uE612'
        const val DOWN_BOW = '\uE610'
        
        // Strings
        const val HARMONIC = '\uE614'
        const val OPEN_STRING = '\uE5F2'
        const val STOPPED = '\uE5F3'
        const val SNAP_PIZZICATO = '\uE631'
    }

    // ============================================
    // DYNAMICS (U+E520 - U+E54F)
    // ============================================
    object Dynamics {
        const val PIANO = '\uE520'            // p
        const val MEZZO = '\uE521'            // m
        const val FORTE = '\uE522'            // f
        const val RINFORZANDO = '\uE523'      // r
        const val SFORZANDO = '\uE524'        // s
        const val Z = '\uE525'                // z
        const val NIENTE = '\uE526'           // n
        
        const val PPPPPP = '\uE527'
        const val PPPPP = '\uE528'
        const val PPPP = '\uE529'
        const val PPP = '\uE52A'
        const val PP = '\uE52B'
        const val P = '\uE520'
        const val MP = '\uE52C'
        const val MF = '\uE52D'
        const val F = '\uE522'
        const val FF = '\uE52F'
        const val FFF = '\uE530'
        const val FFFF = '\uE531'
        const val FFFFF = '\uE532'
        const val FFFFFF = '\uE533'
        
        const val FP = '\uE534'
        const val FZ = '\uE535'
        const val SF = '\uE536'
        const val SFP = '\uE537'
        const val SFPP = '\uE538'
        const val SFZ = '\uE539'
        const val SFFZ = '\uE53B'
        const val SFZP = '\uE53A'
        const val RF = '\uE53C'
        const val RFZ = '\uE53D'
        
        // Hairpins
        const val CRESCENDO = '\uE53E'
        const val DECRESCENDO = '\uE53F'
    }

    // ============================================
    // ORNAMENTS (U+E560 - U+E5FF)
    // ============================================
    object Ornaments {
        const val TRILL = '\uE566'            // Trilo
        const val TURN = '\uE567'             // Grupeto
        const val INVERTED_TURN = '\uE568'    // Grupeto invertido
        const val MORDENT = '\uE56C'          // Mordente
        const val INVERTED_MORDENT = '\uE56D' // Mordente invertido
        const val TRILL_SHARP = '\uE569'
        const val TRILL_FLAT = '\uE56A'
        const val TRILL_NATURAL = '\uE56B'
        
        const val ARPEGGIO_UP = '\uE634'
        const val ARPEGGIO_DOWN = '\uE635'
        
        const val TREMOLO_1 = '\uE220'
        const val TREMOLO_2 = '\uE221'
        const val TREMOLO_3 = '\uE222'
        const val TREMOLO_4 = '\uE223'
        const val TREMOLO_5 = '\uE224'
    }

    // ============================================
    // HOLDS AND PAUSES (U+E4C0 - U+E4DF)
    // ============================================
    object HoldsAndPauses {
        const val FERMATA = '\uE4C0'          // Fermata (corona)
        const val FERMATA_SHORT = '\uE4C4'
        const val FERMATA_LONG = '\uE4C6'
        const val FERMATA_VERY_LONG = '\uE4C8'
        const val FERMATA_BELOW = '\uE4C1'
        
        const val BREATH_MARK = '\uE4CE'
        const val CAESURA = '\uE4D1'
        const val CAESURA_THICK = '\uE4D2'
        const val CAESURA_SHORT = '\uE4D3'
        const val CAESURA_CURVED = '\uE4D4'
    }

    // ============================================
    // REPEATS (U+E040 - U+E04F)
    // ============================================
    object Repeats {
        const val REPEAT_LEFT = '\uE040'
        const val REPEAT_RIGHT = '\uE041'
        const val REPEAT_BOTH = '\uE042'
        const val REPEAT_DOT = '\uE044'
        
        const val SEGNO = '\uE047'
        const val CODA = '\uE048'
        
        const val DA_CAPO = '\uE046'
        const val DAL_SEGNO = '\uE045'
    }

    // ============================================
    // BARLINES (U+E030 - U+E03F)
    // ============================================
    object Barlines {
        const val SINGLE = '\uE030'           // Barra simples
        const val DOUBLE = '\uE031'           // Barra dupla
        const val FINAL = '\uE032'            // Barra final
        const val DASHED = '\uE036'           // Barra tracejada
        const val DOTTED = '\uE037'           // Barra pontilhada
        const val HEAVY = '\uE034'            // Barra grossa
        const val HEAVY_HEAVY = '\uE035'
    }

    // ============================================
    // OCTAVE LINES (U+E510 - U+E51F)
    // ============================================
    object OctaveLines {
        const val OTTAVA = '\uE510'           // 8va
        const val OTTAVA_BASSA = '\uE511'     // 8vb
        const val QUINDICESIMA = '\uE514'     // 15ma
        const val QUINDICESIMA_BASSA = '\uE515' // 15mb
        const val VENTIDUESIMA = '\uE518'     // 22ma
        const val VENTIDUESIMA_BASSA = '\uE519' // 22mb
        const val LOCO = '\uE51C'
    }

    // ============================================
    // AUGMENTATION DOTS
    // ============================================
    object AugmentationDots {
        const val DOT = '\uE1E7'              // Ponto de aumento
    }

    // ============================================
    // BEAMS
    // ============================================
    object Beams {
        const val BEGIN = '\uE8F0'
        const val END = '\uE8F1'
        const val CONTINUE = '\uE8F2'
    }

    // ============================================
    // TIES AND SLURS
    // ============================================
    object TiesAndSlurs {
        const val TIE = '\uE1FD'
        const val SLUR = '\uE1FE'
    }

    // ============================================
    // STAFF BRACKETS
    // ============================================
    object Brackets {
        const val BRACE = '\uE000'
        const val BRACKET = '\uE002'
        const val SQUARE_BRACKET = '\uE003'
    }

    // ============================================
    // FIGURED BASS (for harmony)
    // ============================================
    object FiguredBass {
        const val NUM_0 = '\uEA50'
        const val NUM_1 = '\uEA51'
        const val NUM_2 = '\uEA52'
        const val NUM_3 = '\uEA53'
        const val NUM_4 = '\uEA54'
        const val NUM_5 = '\uEA55'
        const val NUM_6 = '\uEA56'
        const val NUM_7 = '\uEA57'
        const val NUM_8 = '\uEA58'
        const val NUM_9 = '\uEA59'
        const val SHARP = '\uEA5C'
        const val FLAT = '\uEA5D'
        const val NATURAL = '\uEA5E'
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================

    /**
     * Get notehead glyph based on duration
     */
    fun getNoteheadForDuration(beats: Float): Char = when {
        beats >= 4f -> Noteheads.WHOLE
        beats >= 2f -> Noteheads.HALF
        else -> Noteheads.BLACK
    }

    /**
     * Get rest glyph based on duration
     */
    fun getRestForDuration(beats: Float): Char = when {
        beats >= 8f -> Rests.DOUBLE_WHOLE
        beats >= 4f -> Rests.WHOLE
        beats >= 2f -> Rests.HALF
        beats >= 1f -> Rests.QUARTER
        beats >= 0.5f -> Rests.EIGHTH
        beats >= 0.25f -> Rests.SIXTEENTH
        beats >= 0.125f -> Rests.THIRTY_SECOND
        else -> Rests.SIXTY_FOURTH
    }

    /**
     * Get flag glyph based on duration and stem direction
     */
    fun getFlagForDuration(beats: Float, stemUp: Boolean): Char? = when {
        beats >= 0.5f -> null // No flag for quarter and longer
        beats >= 0.25f -> if (stemUp) Flags.EIGHTH_UP else Flags.EIGHTH_DOWN
        beats >= 0.125f -> if (stemUp) Flags.SIXTEENTH_UP else Flags.SIXTEENTH_DOWN
        beats >= 0.0625f -> if (stemUp) Flags.THIRTY_SECOND_UP else Flags.THIRTY_SECOND_DOWN
        else -> if (stemUp) Flags.SIXTY_FOURTH_UP else Flags.SIXTY_FOURTH_DOWN
    }
}
