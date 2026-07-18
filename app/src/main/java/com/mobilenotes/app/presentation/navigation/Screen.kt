package com.mobilenotes.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor?noteId={noteId}") {
        fun createRoute(noteId: String? = null): String =
            if (noteId != null) "editor?noteId=$noteId" else "editor"
    }
    data object Handwriting : Screen("handwriting?noteId={noteId}") {
        fun createRoute(noteId: String? = null): String =
            if (noteId != null) "handwriting?noteId=$noteId" else "handwriting"
    }
    data object Search : Screen("search")
    data object Settings : Screen("settings")
}
