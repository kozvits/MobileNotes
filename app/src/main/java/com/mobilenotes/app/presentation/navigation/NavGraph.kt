package com.mobilenotes.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobilenotes.app.presentation.editor.EditorScreen
import com.mobilenotes.app.presentation.home.HomeScreen
import com.mobilenotes.app.presentation.drawing.HandwritingNoteScreen
import com.mobilenotes.app.presentation.search.SearchScreen
import com.mobilenotes.app.presentation.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToEditor = { noteId -> navController.navigate(Screen.Editor.createRoute(noteId)) },
                onNavigateToHandwriting = { noteId -> navController.navigate(Screen.Handwriting.createRoute(noteId)) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            EditorScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Handwriting.route,
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            HandwritingNoteScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNote = { noteId ->
                    // The editor opens the note by id; it detects handwriting vs text
                    // from the stored content prefix on load.
                    navController.navigate(Screen.Editor.createRoute(noteId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
