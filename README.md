# 🎵 MusiMind

**Plataforma gamificada de aprendizagem musical para Android**

Inspirado no Duolingo, o MusiMind oferece uma experiência envolvente de aprendizado de teoria musical, percepção auditiva e prática instrumental.

## ✨ Funcionalidades

### 🎯 Exercícios Interativos
- **Solfejo** - Cante notas com detecção de pitch em tempo real
- **Percepção Rítmica** - Marque padrões rítmicos com o metrônomo
- **Reconhecimento de Intervalos** - Identifique intervalos musicais
- **Progressões Harmônicas** - Reconheça acordes e progressões

### 🎮 Gamificação
- Sistema de XP e níveis
- 20+ conquistas desbloqueáveis
- Desafios diários
- Leaderboards (semanal/mensal/global)
- Sistema de vidas e streak

### ⚔️ Multiplayer
- Duelos em tempo real
- Quiz musical competitivo

### 👨‍🏫 Dashboard para Professores
- Gestão de turmas
- Acompanhamento de alunos
- Código de convite
- Relatórios de desempenho

### 🎼 Motor de Notação Musical
- Renderização SMuFL profissional com fonte Bravura
- Exibição dinâmica de partituras
- Símbolos musicais completos

## 🛠️ Tecnologias

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Supabase** (Auth, Postgrest, Realtime, Storage)
- **Hilt** para injeção de dependência
- **Room** para banco de dados local
- **Detecção de pitch nativa** (algoritmo YIN)

## 📱 Requisitos

- Android 8.0+ (API 26)
- Permissão de microfone para exercícios de solfejo

## 🚀 Configuração

1. Clone o repositório
2. Configure as credenciais do Supabase em `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"sua-url\"")
   buildConfigField("String", "SUPABASE_ANON_KEY", "\"sua-key\"")
   ```
3. Execute o script SQL em `database/001_initial_schema.sql` no Supabase SQL Editor
4. Baixe as fontes Bravura para `app/src/main/assets/fonts/`
5. Sincronize e compile no Android Studio

## 🗄️ Estrutura do Banco de Dados

Os scripts SQL estão em `/database`:
- `001_initial_schema.sql` - Tabelas, RLS e triggers

## 📄 Licença

Projeto proprietário - © 2024 MusiMind
