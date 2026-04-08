package io.unmi.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps Android BiometricPrompt API for fingerprint / face unlock.
 */
object BiometricHelper {

    /**
     * Check if the device supports biometric authentication.
     */
    fun isAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if device has biometric hardware (even if not enrolled).
     */
    fun hasHardware(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        return result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
               result != BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
    }

    /**
     * Show biometric authentication prompt.
     * @param activity FragmentActivity required by BiometricPrompt
     * @param title dialog title
     * @param subtitle dialog subtitle
     * @param negativeButtonText cancel button text
     * @param onSuccess called on successful auth
     * @param onError called on error/cancel
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User cancelled or lockout — don't show error for user-initiated cancel
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Single attempt failed, prompt stays open for retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }
}
