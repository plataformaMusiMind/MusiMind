package com.musimind.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.musimind.presentation.auth.LoginScreen
import com.musimind.presentation.auth.RegisterScreen
import com.musimind.presentation.onboarding.AvatarSelectionScreen
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
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
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
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
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
                    navController.navigate(Screen.Home.route) {
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

        // Onboarding Screens
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
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
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
                    navController.navigate(Screen.Duel.createRoute(challengeId))
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
                }
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
    }
}
