package com.musimind.navigation

/**
 * Sealed class representing all navigation routes in MusiMind
 */
sealed class Screen(val route: String) {
    // Auth & Onboarding
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object UserType : Screen("user_type")
    data object PlanSelection : Screen("plan_selection")
    data object AvatarSelection : Screen("avatar_selection")
    data object OnboardingTutorial : Screen("onboarding_tutorial")
    data object PlacementTest : Screen("placement_test")
    
    // Main Navigation
    data object Home : Screen("home")
    data object Practice : Screen("practice")
    data object Challenges : Screen("challenges")
    data object Social : Screen("social")
    data object Profile : Screen("profile")
    
    // Practice Exercises
    data object Solfege : Screen("solfege/{exerciseId}") {
        fun createRoute(exerciseId: String) = "solfege/$exerciseId"
    }
    data object RhythmExercise : Screen("rhythm/{exerciseId}") {
        fun createRoute(exerciseId: String) = "rhythm/$exerciseId"
    }
    data object MelodyExercise : Screen("melody/{exerciseId}") {
        fun createRoute(exerciseId: String) = "melody/$exerciseId"
    }
    data object IntervalExercise : Screen("interval/{exerciseId}") {
        fun createRoute(exerciseId: String) = "interval/$exerciseId"
    }
    data object HarmonyExercise : Screen("harmony/{exerciseId}") {
        fun createRoute(exerciseId: String) = "harmony/$exerciseId"
    }
    
    // Theory Lessons
    data object TheoryLesson : Screen("theory/{lessonId}") {
        fun createRoute(lessonId: String) = "theory/$lessonId"
    }
    
    // Challenges & Multiplayer
    data object Duel : Screen("duel/{duelId}") {
        fun createRoute(duelId: String) = "duel/$duelId"
    }
    data object QuizMultiplayer : Screen("quiz/{quizId}") {
        fun createRoute(quizId: String) = "quiz/$quizId"
    }
    data object Leaderboard : Screen("leaderboard")
    
    // Social
    data object Friends : Screen("friends")
    data object FriendProfile : Screen("friend/{userId}") {
        fun createRoute(userId: String) = "friend/$userId"
    }
    
    // Teacher/School Dashboard
    data object TeacherDashboard : Screen("teacher_dashboard")
    data object SchoolDashboard : Screen("school_dashboard")
    data object StudentProgress : Screen("student/{studentId}") {
        fun createRoute(studentId: String) = "student/$studentId"
    }
    
    // Mini-Games
    data object GamesHub : Screen("games_hub")
    data object NoteCatcher : Screen("game/note_catcher")
    data object RhythmTap : Screen("game/rhythm_tap")
    data object MelodyMemory : Screen("game/melody_memory")
    data object IntervalHero : Screen("game/interval_hero")
    data object ScalePuzzle : Screen("game/scale_puzzle")
    data object ChordMatch : Screen("game/chord_match")
    data object KeyShooter : Screen("game/key_shooter")
    data object TempoRun : Screen("game/tempo_run")
    data object SolfegeSing : Screen("game/solfege_sing")
    data object ChordBuilder : Screen("game/chord_builder")
    data object ProgressionQuest : Screen("game/progression_quest")
    data object DailyChallenge : Screen("game/daily_challenge")
    
    // Settings
    data object Settings : Screen("settings")
    data object EditProfile : Screen("edit_profile")
    data object Notifications : Screen("notifications_settings")
}

/**
 * Bottom navigation items
 */
enum class BottomNavItem(
    val screen: Screen,
    val iconName: String,
    val label: String
) {
    HOME(Screen.Home, "home", "Trilha"),
    PRACTICE(Screen.Practice, "music_note", "Prática"),
    CHALLENGES(Screen.Challenges, "emoji_events", "Desafios"),
    SOCIAL(Screen.Social, "people", "Social"),
    PROFILE(Screen.Profile, "person", "Perfil")
}
