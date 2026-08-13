package com.mobilenotes.app.domain.model

/**
 * User-facing settings, backed by DataStore.
 *
 * @param isGridView show notes as a grid instead of a list
 * @param defaultPaperType default paper background for new handwriting notes (PaperType.name)
 * @param sortOrder 0 = updated desc, 1 = created desc, 2 = title asc, 3 = title desc
 * @param useDynamicColor use Material You dynamic color
 * @param appLockEnabled require biometric unlock to open locked notes / the app
 */
data class UserSettings(
    val isGridView: Boolean = false,
    val defaultPaperType: String = "GRID",
    val sortOrder: Int = 0,
    val useDynamicColor: Boolean = true,
    val appLockEnabled: Boolean = false
)
