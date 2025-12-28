package com.musimind.music.notation.smufl

/**
 * SMuFL (Standard Music Font Layout) Glyph Mappings
 * 
 * Complete mapping of Unicode codepoints for SMuFL-compliant fonts like Bravura.
 * Reference: https://w3c.github.io/smufl/latest/
 * 
 * These codepoints are in the Private Use Area (PUA) starting at U+E000
 */
/**
 * SMuFL (Standard Music Font Layout) Glyph Mappings - COMPLETE EDITION
 * 
 * Comprehensive mapping of Unicode codepoints for SMuFL-compliant fonts like Bravura.
 * Reference: https://w3c.github.io/smufl/latest/
 * 
 * These codepoints are in the Private Use Area (PUA) starting at U+E000
 * 
 * This file contains ALL major SMuFL categories for professional music notation:
 * - Clefs, Noteheads, Flags, Rests
 * - Accidentals (including microtonal)
 * - Time Signatures
 * - Articulations, Dynamics, Ornaments
 * - Holds, Pauses, Fermatas
 * - Repeats, Barlines
 * - Octave Lines, Tuplets
 * - Pedaling, String Techniques
 * - Beams, Ties, Slurs
 * - Staff Brackets
 * - Figured Bass, Chord Symbols
 * - Expression Marks
 * - And much more...
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
        const val SOPRANO = '\uE05C'          // Clave de Dó (Soprano)
        const val MEZZO_SOPRANO = '\uE05C'    // Clave de Dó (Mezzo-soprano)
        const val BARITONE = '\uE05C'         // Clave de Dó (Baritone)
        
        const val PERCUSSION = '\uE069'       // Clave de Percussão
        const val PERCUSSION_2 = '\uE06A'     // Clave de Percussão 2
        const val SEMIPITCHED_PERCUSSION = '\uE06B' // Semitonal percussion
        
        const val TAB = '\uE06D'              // Clave de Tablatura
        const val TAB_4_STRING = '\uE06E'     // Tablatura 4 cordas
        const val TAB_6_STRING = '\uE06F'     // Tablatura 6 cordas
        
        // Clef change versions (smaller)
        const val TREBLE_CHANGE = '\uE07A'
        const val BASS_CHANGE = '\uE07B'
        const val ALTO_CHANGE = '\uE07C'
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
    // TUPLETS (U+E1F0 - U+E1FF)
    // ============================================
    object Tuplets {
        const val COLON = '\uE88A'              // Tuplet colon
        const val NUM_0 = '\uE880'
        const val NUM_1 = '\uE881'
        const val NUM_2 = '\uE882'
        const val NUM_3 = '\uE883'
        const val NUM_4 = '\uE884'
        const val NUM_5 = '\uE885'
        const val NUM_6 = '\uE886'
        const val NUM_7 = '\uE887'
        const val NUM_8 = '\uE888'
        const val NUM_9 = '\uE889'
        
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
    // PEDALING (U+E650 - U+E67F)
    // ============================================
    object Pedaling {
        const val PEDAL_MARK = '\uE650'         // Ped.
        const val PEDAL_P = '\uE651'            // P mark
        const val PEDAL_E = '\uE652'            // E mark
        const val PEDAL_D = '\uE653'            // D mark
        const val PEDAL_DOT = '\uE654'          // Pedal dot
        const val PEDAL_UP = '\uE655'           // Pedal up (release)
        const val PEDAL_SOSTENUTO = '\uE659'    // Sostenuto pedal
        const val HALF_PEDAL = '\uE656'         // Half pedal
        const val PEDAL_HEEL_1 = '\uE661'       // Heel 1
        const val PEDAL_HEEL_2 = '\uE662'       // Heel 2
        const val PEDAL_TOE_1 = '\uE663'        // Toe 1
        const val PEDAL_TOE_2 = '\uE664'        // Toe 2
        
        // Harp pedals
        const val HARP_PEDAL_RAISED = '\uE680'
        const val HARP_PEDAL_CENTER = '\uE681'
        const val HARP_PEDAL_LOWERED = '\uE682'
        const val HARP_PEDAL_DIVIDER = '\uE683'
    }

    // ============================================
    // STRING TECHNIQUES (U+E610 - U+E62F)
    // ============================================
    object StringTechniques {
        const val DOWN_BOW = '\uE610'           // Arcada para baixo
        const val UP_BOW = '\uE612'             // Arcada para cima
        const val HARMONIC = '\uE614'           // Harmônico
        const val HALF_HARMONIC = '\uE615'
        
        // Fingering
        const val THUMB = '\uE624'              // Polegar (violoncelo)
        const val OPEN_STRING = '\uE5F2'        // Corda solta
        const val MUTE_ON = '\uE638'            // Con sordino
        const val MUTE_OFF = '\uE639'           // Senza sordino
        
        // Pizzicato
        const val SNAP_PIZZ = '\uE631'          // Pizzicato à la Bartók
        const val PIZZICATO = '\uE630'          // Pizzicato
        const val LEFT_HAND_PIZZ = '\uE633'     // Pizz. mão esquerda
        
        // Tremolo
        const val TREMOLO_DIVISI = '\uE621'
        const val BOWING_FLAUTANDO = '\uE620'   // Sul tasto / flautando
        const val BOWING_PONTICELLO = '\uE622' // Sul ponticello
        
        // Guitar/String effects
        const val ARPEGGIO_UP = '\uE634'
        const val ARPEGGIO_DOWN = '\uE635'
        const val STRUM_UP = '\uE636'
        const val STRUM_DOWN = '\uE637'
    }

    // ============================================
    // VOLTA BRACKETS / ENDINGS (U+E040)
    // ============================================
    object VoltaBrackets {
        // These are typically drawn with numbers, not glyphs
        const val BRACKET_TOP = '\uE500'
        const val BRACKET_BOTTOM = '\uE501'
        
        // Numbers for volta brackets (use regular time sig numbers)
        const val NUM_1 = '\uE081'
        const val NUM_2 = '\uE082'
        const val NUM_3 = '\uE083'
    }

    // ============================================
    // CHORD SYMBOLS (U+E870 - U+E87F)
    // ============================================
    object ChordSymbols {
        const val DIMINISHED = '\uE870'         // ° (diminuto)
        const val HALF_DIMINISHED = '\uE871'    // ø (meio-diminuto)
        const val AUGMENTED = '\uE872'          // + (aumentado)
        const val MAJOR_SEVENTH = '\uE873'      // Δ (sétima maior)
        const val MINOR = '\uE874'              // - (menor)
        
        // Extensions
        const val PARENS_LEFT = '\uE876'
        const val PARENS_RIGHT = '\uE877'
        const val BRACKET_LEFT = '\uE878'
        const val BRACKET_RIGHT = '\uE879'
        
        // Bass
        const val BASS_SLASH = '\uE87A'         // Slash for bass notes
    }

    // ============================================
    // EXPRESSION TEXT (U+E540 - U+E55F)
    // Standard music expression words
    // ============================================
    object ExpressionText {
        // Note: These are text-based, typically use BravuraText font
        // These glyphs are for special symbols that accompany text
        const val NIENTE_CIRCLE = '\uE526'      // n (niente)
        const val SUBITO = '\uE53C'             // s (subito)
    }

    // ============================================
    // ARROWS AND ARROWHEADS (U+EB60 - U+EB8F)
    // ============================================
    object Arrows {
        const val ARROW_BLACK_UP = '\uEB60'
        const val ARROW_BLACK_DOWN = '\uEB62'
        const val ARROW_BLACK_LEFT = '\uEB64'
        const val ARROW_BLACK_RIGHT = '\uEB66'
        const val ARROW_BLACK_UP_LEFT = '\uEB68'
        const val ARROW_BLACK_UP_RIGHT = '\uEB6A'
        const val ARROW_BLACK_DOWN_LEFT = '\uEB6C'
        const val ARROW_BLACK_DOWN_RIGHT = '\uEB6E'
        
        const val ARROW_WHITE_UP = '\uEB61'
        const val ARROW_WHITE_DOWN = '\uEB63'
        const val ARROW_WHITE_LEFT = '\uEB65'
        const val ARROW_WHITE_RIGHT = '\uEB67'
        
        // Open arrowheads
        const val ARROWHEAD_OPEN_UP = '\uEB70'
        const val ARROWHEAD_OPEN_DOWN = '\uEB71'
        const val ARROWHEAD_OPEN_LEFT = '\uEB72'
        const val ARROWHEAD_OPEN_RIGHT = '\uEB73'
    }

    // ============================================
    // KEYBOARD TECHNIQUES (U+E6A0 - U+E6BF)
    // ============================================
    object KeyboardTechniques {
        // Pluck inside piano
        const val PLUCK_INSIDE = '\uE6A0'
        
        // Hand positions
        const val LEFT_HAND = '\uE6B0'          // L.H.
        const val RIGHT_HAND = '\uE6B1'         // R.H.
        
        // Pedal directions
        const val PEDAL_HEEL = '\uE6B2'
        const val PEDAL_TOE = '\uE6B3'
        const val PEDAL_HEEL_OR_TOE = '\uE6B4'
    }

    // ============================================
    // WIND/BRASS TECHNIQUES (U+E5D0 - U+E5EF)
    // ============================================
    object WindTechniques {
        const val DOUBLE_TONGUE = '\uE5D0'      // Double tonguing
        const val TRIPLE_TONGUE = '\uE5D1'      // Triple tonguing
        const val STOPPED = '\uE5E4'            // Stopped (+ horn)
        const val OPEN = '\uE5E5'               // Open (o horn)
        const val MUTE_CLOSED = '\uE5E6'        // Mute closed
        const val MUTE_HALF_OPEN = '\uE5E7'     // Mute half-open
        const val MUTE_OPEN = '\uE5E8'          // Mute open
        const val FLIP = '\uE5E9'               // Flip
        const val SMEAR = '\uE5EA'              // Smear/bend
        const val LIFT = '\uE5EB'               // Lift
        const val DOIT = '\uE5EC'               // Doit
        const val FALL = '\uE5ED'               // Fall
        const val BEND = '\uE5EE'               // Bend
        const val MULTIPHONIC = '\uE5F0'        // Multiphonic
        const val HARMONIC_MUTE = '\uE5F1'      // Harmonic mute
    }

    // ============================================
    // ANALYTICS (Music Analysis Symbols)
    // ============================================
    object Analytics {
        const val HAUPTSTIMME = '\uE860'        // Principal voice
        const val NEBENSTIMME = '\uE861'        // Secondary voice
        const val HAUPTSTIMME_END = '\uE862'
        const val NEBENSTIMME_END = '\uE863'
        
        // Function theory (Roman numerals are typically text)
        const val CHOICE_BRACKET_LEFT = '\uE864'
        const val CHOICE_BRACKET_RIGHT = '\uE865'
    }

    // ============================================
    // PICTOGRAMS - Percussion (U+E710 - U+E7FF)
    // ============================================
    object PercussionPictograms {
        // Beaters
        const val SOFT_MALLET = '\uE770'
        const val MEDIUM_MALLET = '\uE771'
        const val HARD_MALLET = '\uE772'
        const val WOODEN_MALLET = '\uE773'
        const val BASS_DRUM_MALLET = '\uE774'
        const val TIMPANI_MALLET = '\uE775'
        
        // Sticks
        const val DRUM_STICK = '\uE780'
        const val SNARE_STICK = '\uE781'
        const val BRUSHES = '\uE782'
        const val RODS = '\uE783'
        
        // Other beaters
        const val HANDS = '\uE784'
        const val FINGERS = '\uE785'
        const val FINGERNAILS = '\uE786'
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
     * 
     * Duration mapping:
     * - >= 1.0 beats (quarter and longer): no flag
     * - >= 0.5 beats (eighth/colcheia): 1 flag
     * - >= 0.25 beats (sixteenth/semicolcheia): 2 flags
     * - >= 0.125 beats (thirty-second/fusa): 3 flags
     * - < 0.125 beats (sixty-fourth/semifusa): 4 flags
     */
    fun getFlagForDuration(beats: Float, stemUp: Boolean): Char? = when {
        beats >= 1f -> null // No flag for quarter note and longer
        beats >= 0.5f -> if (stemUp) Flags.EIGHTH_UP else Flags.EIGHTH_DOWN           // Colcheia = 1 flag
        beats >= 0.25f -> if (stemUp) Flags.SIXTEENTH_UP else Flags.SIXTEENTH_DOWN     // Semicolcheia = 2 flags
        beats >= 0.125f -> if (stemUp) Flags.THIRTY_SECOND_UP else Flags.THIRTY_SECOND_DOWN // Fusa = 3 flags
        else -> if (stemUp) Flags.SIXTY_FOURTH_UP else Flags.SIXTY_FOURTH_DOWN         // Semifusa = 4 flags
    }

    // ============================================
    // INDIVIDUAL NOTES (Metronome Marks)
    // ============================================
    object MetronomeMarks {
        const val WHOLE = '\uE1D2'
        const val HALF = '\uE1D3'
        const val QUARTER = '\uE1D5'
        const val EIGHTH = '\uE1D7'
        const val SIXTEENTH = '\uE1D9'
        const val THIRTY_SECOND = '\uE1DB'
        const val SIXTY_FOURTH = '\uE1DD'
    }

    // ============================================
    // STANDARD UNICODE MUSICAL SYMBOLS (U+1D100 - U+1D1FF)
    // https://symbl.cc/pt/unicode/blocks/musical-symbols/
    // ============================================
    object StandardMusicalSymbols {
        // Notes
        const val NOTE_WHOLE = "\uD834\uDD5D"       // 𝅝 (U+1D15D)
        const val NOTE_HALF = "\uD834\uDD5E"        // 𝅗𝅥 (U+1D15E)
        const val NOTE_QUARTER = "\uD834\uDD5F"     // 𝅘𝅥 (U+1D15F)
        const val NOTE_EIGHTH = "\uD834\uDD60"      // 𝅘𝅥𝅮 (U+1D160)
        const val NOTE_SIXTEENTH = "\uD834\uDD61"   // 𝅘𝅥𝅯 (U+1D161)
        const val NOTE_THIRTY_SECOND = "\uD834\uDD62" // 𝅘𝅥𝅰 (U+1D162)
        const val NOTE_SIXTY_FOURTH = "\uD834\uDD63" // 𝅘𝅥𝅱 (U+1D163)

        // Rests
        const val REST_WHOLE = "\uD834\uDD3B"       // 𝄻 (U+1D13B)
        const val REST_HALF = "\uD834\uDD3C"        // 𝄼 (U+1D13C)
        const val REST_QUARTER = "\uD834\uDD3D"     // 𝄽 (U+1D13D)
        const val REST_EIGHTH = "\uD834\uDD3E"      // 𝄾 (U+1D13E)
        const val REST_SIXTEENTH = "\uD834\uDD3F"   // 𝄿 (U+1D13F)
        const val REST_THIRTY_SECOND = "\uD834\uDD40" // 𝅀 (U+1D140)
        const val REST_SIXTY_FOURTH = "\uD834\uDD41"  // 𝅁 (U+1D141)

        // Accidentals
        const val FLAT = "\u266D"                   // ♭ (U+266D)
        const val NATURAL = "\u266E"                // ♮ (U+266E)
        const val SHARP = "\u266F"                  // ♯ (U+266F)
    }
}
