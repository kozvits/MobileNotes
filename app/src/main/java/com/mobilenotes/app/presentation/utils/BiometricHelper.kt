package com.mobilenotes.app.presentation.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mobilenotes.app.R

/**
 * Shows a biometric authentication prompt. On success [onSuccess] is invoked.
 * If biometrics are unavailable or the user cancels, [onError] is invoked.
 */
fun FragmentActivity.authenticateWithBiometrics(
    title: String = "Unlock note",
    subtitle: String = "Confirm your identity to open this note",
    onSuccess: () -> Unit,
    onError: () -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(this)
    val prompt = BiometricPrompt(
        this,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError()
            }
        }
    )
    val canAuthenticate = BiometricManager.from(this)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        // No biometrics enrolled — fall back to opening directly.
        onSuccess()
        return
    }
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Cancel")
        .build()
    prompt.authenticate(info)
}
