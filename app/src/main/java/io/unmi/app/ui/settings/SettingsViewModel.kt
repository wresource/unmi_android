package io.unmi.app.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.R
import io.unmi.app.data.local.datastore.AppPreferences
import io.unmi.app.data.local.datastore.AppSettings
import io.unmi.app.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val appPreferences: AppPreferences,
    private val securityManager: SecurityManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = appPreferences.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    private val _passwordChangeResult = MutableStateFlow<String?>(null)
    val passwordChangeResult: StateFlow<String?> = _passwordChangeResult.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDarkMode(enabled) }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setBiometricEnabled(enabled) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch { appPreferences.setAutoLockMinutes(minutes) }
    }

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch { appPreferences.setDefaultCurrency(currency) }
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (!securityManager.verifyPassword(oldPassword, currentSettings.passwordSalt, currentSettings.passwordHash)) {
                _passwordChangeResult.value = app.getString(R.string.settings_password_wrong)
                return@launch
            }
            if (newPassword.length < 6) {
                _passwordChangeResult.value = app.getString(R.string.settings_password_short)
                return@launch
            }
            if (newPassword != confirmPassword) {
                _passwordChangeResult.value = app.getString(R.string.settings_password_mismatch)
                return@launch
            }
            val salt = securityManager.generateSalt()
            val hash = securityManager.hashPassword(newPassword, salt)
            appPreferences.setPasswordCredentials(hash, salt)
            _passwordChangeResult.value = app.getString(R.string.settings_password_success)
        }
    }

    fun clearPasswordChangeResult() {
        _passwordChangeResult.value = null
    }

    fun setGithubUrl(url: String) {
        viewModelScope.launch { appPreferences.setGithubUrl(url) }
    }

    fun setProjectUrl(url: String) {
        viewModelScope.launch { appPreferences.setProjectUrl(url) }
    }

    fun setDomainListUrl(url: String) {
        viewModelScope.launch { appPreferences.setDomainListUrl(url) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { appPreferences.setLanguage(language) }
    }
}
