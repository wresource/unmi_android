package io.unmi.app.ui.init

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.R
import io.unmi.app.data.local.datastore.AppPreferences
import io.unmi.app.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InitUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class InitViewModel @Inject constructor(
    private val app: Application,
    private val appPreferences: AppPreferences,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InitUiState())
    val uiState: StateFlow<InitUiState> = _uiState.asStateFlow()

    fun initialize(password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (password.length < 6) {
                _uiState.update { it.copy(isLoading = false, error = app.getString(R.string.auth_error_short_password)) }
                return@launch
            }

            if (password != confirmPassword) {
                _uiState.update { it.copy(isLoading = false, error = app.getString(R.string.auth_error_mismatch)) }
                return@launch
            }

            val salt = securityManager.generateSalt()
            val hash = securityManager.hashPassword(password, salt)

            appPreferences.setPasswordCredentials(hash, salt)
            appPreferences.setFirstLaunch(false)

            _uiState.update { it.copy(isLoading = false, isComplete = true) }
        }
    }
}
