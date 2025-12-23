# MusiMind Music Notation Engine

Motor de renderização de partituras profissional usando o padrão SMuFL (Standard Music Font Layout).

## Instalação da Fonte Bravura

A fonte **Bravura** é necessária para renderizar os símbolos musicais. Baixe de:

1. Acesse: https://github.com/steinbergmedia/bravura/tree/master/redist/otf
2. Baixe `Bravura.otf` e `BravuraText.otf`
3. Coloque em: `app/src/main/assets/fonts/`

Ou via terminal:
```bash
mkdir -p app/src/main/assets/fonts
curl -L -o app/src/main/assets/fonts/Bravura.otf \
  "https://raw.githubusercontent.com/steinbergmedia/bravura/master/redist/otf/Bravura.otf"
curl -L -o app/src/main/assets/fonts/BravuraText.otf \
  "https://raw.githubusercontent.com/steinbergmedia/bravura/master/redist/otf/BravuraText.otf"
```

## Arquitetura

```
music/notation/
├── smufl/
│   └── SMuFLGlyphs.kt       # Mapeamento Unicode SMuFL
├── model/
│   └── ScoreModels.kt       # Modelos de domínio
├── engine/
│   └── NotationEngine.kt    # Motor de renderização
├── layout/
│   └── ScoreLayoutEngine.kt # Cálculo de layout
├── parser/
│   └── ScoreParser.kt       # Parser JSON
├── ui/
│   ├── ScoreView.kt         # Composable principal
│   ├── MusicSymbols.kt      # Símbolos individuais
│   └── StaffViews.kt        # Visualizações de pauta
└── di/
    └── NotationModule.kt    # Injeção de dependência
```

## Uso

### Exibir uma partitura completa
```kotlin
@Composable
fun ExerciseScreen() {
    val score = remember { ScoreParser.parseScore(jsonString) }
    
    ScoreView(
        score = score,
        staffHeight = 64.dp,
        noteStates = mapOf("note_1" to NoteState.HIGHLIGHTED),
        onNoteClick = { noteId -> /* handle click */ }
    )
}
```

### Exibir símbolos individuais em lições
```kotlin
// Clave
ClefSymbol(clef = ClefType.TREBLE, size = 48.dp)

// Nota com haste
NoteSymbol(duration = Duration.QUARTER, size = 32.dp)

// Pausa
RestSymbol(duration = Duration.HALF, size = 32.dp)

// Acidente
AccidentalSymbol(accidental = AccidentalType.SHARP, size = 24.dp)

// Dinâmica
DynamicSymbol(dynamic = DynamicType.MF, size = 24.dp)

// Fórmula de compasso
TimeSignatureSymbol(numerator = 6, denominator = 8, size = 48.dp)
```

### Nota em pauta para exercícios
```kotlin
NoteOnStaff(
    staffPosition = 4, // Posição na pauta (0 = linha inferior)
    clef = ClefType.TREBLE,
    duration = 1f, // Semínima
    noteColor = MaterialTheme.colorScheme.primary
)
```

### Intervalo para percepção
```kotlin
IntervalOnStaff(
    lowerPosition = 0, // Mi4
    upperPosition = 4, // Si4 (quinta justa)
    clef = ClefType.TREBLE
)
```

## Formato JSON de Partituras

```json
{
  "id": "exercise_1",
  "title": "Exercício de Solfejo",
  "clef": "treble",
  "keySignature": "G",
  "timeSignature": "4/4",
  "tempo": 120,
  "measures": [
    {
      "elements": [
        {"type": "note", "duration": 1.0, "pitch": "C4"},
        {"type": "note", "duration": 0.5, "pitch": "D4"},
        {"type": "note", "duration": 0.5, "pitch": "E4"},
        {"type": "rest", "duration": 2.0}
      ],
      "barline": "single"
    }
  ]
}
```

### Notação de Pitch
- `C4` = Dó central
- `F#5` = Fá sustenido na 5ª oitava
- `Bb3` = Si bemol na 3ª oitava
- `D##4` = Ré dobrado sustenido

### Durações
- `4.0` = Semibreve
- `2.0` = Mínima
- `1.0` = Semínima
- `0.5` = Colcheia
- `0.25` = Semicolcheia
- `0.125` = Fusa

## Símbolos SMuFL Disponíveis

O mapeamento completo está em `SMuFLGlyphs.kt`:
- Claves (Sol, Fá, Dó, Percussão)
- Figuras (semibreve a semifusa)
- Pausas
- Acidentes (bemol, sustenido, bequadro, dobrados)
- Fórmulas de compasso
- Articulações (staccato, accent, tenuto, etc.)
- Dinâmicas (pp a ff, sfz, fp, etc.)
- Ornamentos (trinado, mordente, grupeto)
- Barras de compasso
- E muito mais...
