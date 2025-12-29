package com.musimind.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.musimind.domain.auth.AuthManager
import com.musimind.presentation.auth.LoginScreen
import com.musimind.presentation.auth.RegisterScreen
import com.musimind.presentation.onboarding.AvatarSelectionScreen
import com.musimind.presentation.onboarding.LanguageSelectionScreen
import com.musimind.presentation.onboarding.OnboardingTutorialScreen
import com.musimind.presentation.onboarding.PlanSelectionScreen
import com.musimind.presentation.onboarding.UserTypeScreen
import com.musimind.presentation.splash.SplashScreen
import com.musimind.presentation.home.HomeScreen
import com.musimind.presentation.practice.PracticeScreen
import com.musimind.presentation.challenges.ChallengesScreen
import com.musimind.presentation.social.SocialScreen
import com.musimind.presentation.profile.ProfileScreen

/**
 * Main navigation graph for MusiMind
 */
@Composable
fun MusiMindNavGraph(
    navController: NavHostController,
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    // Get current user ID for use in all screens
    val currentUserId = remember { authManager.currentUserId }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // Language Selection Screen - First screen for new users
        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                onLanguageSelected = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Splash Screen - Entry point that determines where to navigate
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLanguageSelection = {
                    navController.navigate(Screen.LanguageSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToUserType = {
                    navController.navigate(Screen.UserType.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToPlanSelection = {
                    navController.navigate(Screen.PlanSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAvatarSelection = {
                    navController.navigate(Screen.AvatarSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToTutorial = {
                    navController.navigate(Screen.OnboardingTutorial.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Auth Screens
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    // After login, go to splash to check onboarding status
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToUserType = {
                    navController.navigate(Screen.UserType.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.UserType.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Screens - Must complete in order
        composable(Screen.UserType.route) {
            UserTypeScreen(
                onUserTypeSelected = {
                    navController.navigate(Screen.PlanSelection.route)
                }
            )
        }

        composable(Screen.PlanSelection.route) {
            PlanSelectionScreen(
                onPlanSelected = {
                    navController.navigate(Screen.AvatarSelection.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AvatarSelection.route) {
            AvatarSelectionScreen(
                onComplete = {
                    navController.navigate(Screen.OnboardingTutorial.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnboardingTutorial.route) {
            OnboardingTutorialScreen(
                onComplete = {
                    // After tutorial, go to placement test
                    navController.navigate(Screen.PlacementTest.route)
                }
            )
        }
        
        // Placement Test - Adaptive assessment to determine starting level
        composable(Screen.PlacementTest.route) {
            com.musimind.presentation.onboarding.PlacementTestScreen(
                onComplete = { level ->
                    // Save level and navigate to home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onSkip = {
                    // Skip test and go to home with default level
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Main App Screens (Bottom Navigation)
        composable(Screen.Home.route) {
            HomeScreen(
                onNodeClick = { nodeId ->
                    // Navigate to appropriate exercise/theory based on node type
                    navController.navigate(Screen.TheoryLesson.createRoute(nodeId))
                }
            )
        }

        composable(Screen.Practice.route) {
            PracticeScreen(
                onExerciseClick = { category, exerciseId ->
                    // Navigate based on category
                    val route = when (category) {
                        com.musimind.domain.model.MusicCategory.SOLFEGE -> 
                            Screen.Solfege.createRoute(exerciseId)
                        com.musimind.domain.model.MusicCategory.RHYTHMIC_PERCEPTION -> 
                            Screen.RhythmExercise.createRoute(exerciseId)
                        com.musimind.domain.model.MusicCategory.INTERVAL_PERCEPTION -> 
                            Screen.IntervalExercise.createRoute(exerciseId)
                        com.musimind.domain.model.MusicCategory.MELODIC_PERCEPTION -> 
                            Screen.MelodyExercise.createRoute(exerciseId)
                        com.musimind.domain.model.MusicCategory.HARMONIC_PROGRESSIONS -> 
                            Screen.HarmonyExercise.createRoute(exerciseId)
                        else -> Screen.Solfege.createRoute(exerciseId)
                    }
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.Challenges.route) {
            ChallengesScreen(
                onChallengeClick = { challengeId ->
                    when (challengeId) {
                        "quiz_multiplayer" -> navController.navigate("quiz_multiplayer")
                        "duel_random" -> navController.navigate(Screen.Duel.createRoute("random"))
                        "group_room" -> navController.navigate("quiz_multiplayer") // Reuse multiplayer screen
                        "speed_challenge" -> navController.navigate(Screen.DailyChallenge.route)
                        else -> navController.navigate(Screen.Duel.createRoute(challengeId))
                    }
                }
            )
        }

        composable(Screen.Social.route) {
            SocialScreen(
                onFriendClick = { friendId ->
                    navController.navigate(Screen.FriendProfile.createRoute(friendId))
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onUpgrade = {
                    navController.navigate(Screen.Paywall.createRoute("maestro"))
                },
                onManageSubscription = {
                    navController.navigate(Screen.ManageSubscription.route)
                }
            )
        }
        
        // Theory Lesson Screen
        composable(
            route = Screen.TheoryLesson.route,
            arguments = listOf(
                androidx.navigation.navArgument("lessonId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            com.musimind.presentation.theory.TheoryLessonScreen(
                lessonId = lessonId,
                onBack = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }
        
        // Exercise Screens
        composable(
            route = Screen.Solfege.route,
            arguments = listOf(
                androidx.navigation.navArgument("exerciseId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            com.musimind.presentation.exercise.SolfegeExerciseScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onComplete = { score, total ->
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.RhythmExercise.route,
            arguments = listOf(
                androidx.navigation.navArgument("exerciseId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            com.musimind.presentation.exercise.RhythmExerciseScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onComplete = { score, total ->
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.IntervalExercise.route,
            arguments = listOf(
                androidx.navigation.navArgument("exerciseId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            com.musimind.presentation.exercise.IntervalExerciseScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onComplete = { score, total ->
                    navController.popBackStack()
                }
            )
        }
        
        // Melodic Perception Exercise
        composable(
            route = Screen.MelodyExercise.route,
            arguments = listOf(
                androidx.navigation.navArgument("exerciseId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            com.musimind.presentation.exercise.MelodicPerceptionScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onComplete = { score, total ->
                    navController.popBackStack()
                }
            )
        }
        
        // =====================================================
        // MINI-GAMES
        // =====================================================
        
        // Games Hub - Lista de todos os jogos
        composable(Screen.GamesHub.route) {
            com.musimind.presentation.games.GamesHubScreen(
                userId = currentUserId,
                onGameSelect = { gameName ->
                    when (gameName) {
                        "note_catcher" -> navController.navigate(Screen.NoteCatcher.route)
                        "rhythm_tap" -> navController.navigate(Screen.RhythmTap.route)
                        "melody_memory" -> navController.navigate(Screen.MelodyMemory.route)
                        "interval_hero" -> navController.navigate(Screen.IntervalHero.route)
                        "scale_puzzle" -> navController.navigate(Screen.ScalePuzzle.route)
                        "chord_match" -> navController.navigate(Screen.ChordMatch.route)
                        "key_shooter" -> navController.navigate(Screen.KeyShooter.route)
                        "tempo_run" -> navController.navigate(Screen.TempoRun.route)
                        "solfege_sing" -> navController.navigate(Screen.SolfegeSing.route)
                        "chord_builder" -> navController.navigate(Screen.ChordBuilder.route)
                        "progression_quest" -> navController.navigate(Screen.ProgressionQuest.route)
                        "daily_challenge" -> navController.navigate(Screen.DailyChallenge.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Note Catcher Game
        composable(Screen.NoteCatcher.route) {
            com.musimind.presentation.games.NoteCatcherScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Rhythm Tap Game
        composable(Screen.RhythmTap.route) {
            com.musimind.presentation.games.RhythmTapScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Melody Memory Game
        composable(Screen.MelodyMemory.route) {
            com.musimind.presentation.games.MelodyMemoryScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Interval Hero Game
        composable(Screen.IntervalHero.route) {
            com.musimind.presentation.games.IntervalHeroScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Scale Puzzle Game
        composable(Screen.ScalePuzzle.route) {
            com.musimind.presentation.games.ScalePuzzleScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Solfege Sing Game
        composable(Screen.SolfegeSing.route) {
            com.musimind.presentation.games.SolfegeSingScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Chord Match Game
        composable(Screen.ChordMatch.route) {
            com.musimind.presentation.games.ChordMatchScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Key Shooter Game
        composable(Screen.KeyShooter.route) {
            com.musimind.presentation.games.KeyShooterScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Tempo Run Game
        composable(Screen.TempoRun.route) {
            com.musimind.presentation.games.TempoRunScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Chord Builder Game
        composable(Screen.ChordBuilder.route) {
            com.musimind.presentation.games.ChordBuilderScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Progression Quest Game
        composable(Screen.ProgressionQuest.route) {
            com.musimind.presentation.games.ProgressionQuestScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Daily Challenge
        composable(Screen.DailyChallenge.route) {
            com.musimind.presentation.games.DailyChallengeScreen(
                userId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // =====================================================
        // QUIZ MULTIPLAYER
        // =====================================================
        
        // Quiz Multiplayer - Create/Join rooms
        composable("quiz_multiplayer") {
            com.musimind.presentation.games.multiplayer.QuizMultiplayerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // =====================================================
        // ASSESSMENTS (Teacher/School)
        // =====================================================
        
        // Create Assessment
        composable(Screen.CreateAssessment.route) {
            com.musimind.presentation.teacher.CreateAssessmentScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Take Assessment
        composable(Screen.TakeAssessment.route) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: return@composable
            com.musimind.presentation.assessment.TakeAssessmentScreen(
                assessmentId = assessmentId,
                onBack = { navController.popBackStack() },
                onComplete = { score, total, passed ->
                    navController.popBackStack()
                }
            )
        }
        
        // =====================================================
        // SUBSCRIPTION / PAYWALL
        // =====================================================
        
        // Paywall Screen
        composable(Screen.Paywall.route) { backStackEntry ->
            val tierArg = backStackEntry.arguments?.getString("tier") ?: "maestro"
            val highlightTier = when (tierArg) {
                "spalla" -> com.musimind.domain.model.SubscriptionTier.SPALLA
                else -> com.musimind.domain.model.SubscriptionTier.MAESTRO
            }
            com.musimind.presentation.subscription.PaywallScreen(
                onBack = { navController.popBackStack() },
                onSubscriptionComplete = {
                    navController.popBackStack()
                },
                highlightTier = highlightTier
            )
        }
        
        // Manage Subscription
        composable(Screen.ManageSubscription.route) {
            // TODO: Implement ManageSubscriptionScreen
            // For now, redirect to paywall
            com.musimind.presentation.subscription.PaywallScreen(
                onBack = { navController.popBackStack() },
                onSubscriptionComplete = {
                    navController.popBackStack()
                }
            )
        }
    }
}
