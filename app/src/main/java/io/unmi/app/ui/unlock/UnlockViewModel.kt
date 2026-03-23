package io.unmi.app.ui.unlock

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.R
import io.unmi.app.data.local.db.dao.AccountDao
import io.unmi.app.data.local.db.entity.AccountEntity
import io.unmi.app.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class UnlockUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUnlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val hasAccounts: Boolean = false,
    val showCreateForm: Boolean = false,
    val createDisplayName: String = "",
    val createPassword: String = "",
    val createConfirmPassword: String = ""
)

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val app: Application,
    private val accountDao: AccountDao,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val count = accountDao.getCount()
            _uiState.update { it.copy(hasAccounts = count > 0, showCreateForm = count == 0) }
        }
    }

    fun login(password: String) {
        if (password.isBlank()) {
            _uiState.update { it.copy(error = app.getString(R.string.auth_error_empty)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val accounts = accountDao.getAll()
            var matched: AccountEntity? = null

            for (account in accounts) {
                if (securityManager.verifyPassword(password, account.passwordSalt, account.passwordHash)) {
                    matched = account
                    break
                }
            }

            if (matched != null) {
                // Init session with password-derived encryption key
                securityManager.initSession(password, matched.passwordSalt, matched.id)

                // Update last login time
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                accountDao.update(matched.copy(lastLoginAt = now))

                _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
            } else {
                val attempts = _uiState.value.failedAttempts + 1
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (attempts >= 5) app.getString(R.string.auth_error_too_many) else app.getString(R.string.auth_error_wrong_password),
                        failedAttempts = attempts
                    )
                }
            }
        }
    }

    fun showCreateForm() {
        _uiState.update { it.copy(showCreateForm = true, error = null) }
    }

    fun hideCreateForm() {
        _uiState.update { it.copy(showCreateForm = false, error = null) }
    }

    fun updateCreateField(field: String, value: String) {
        _uiState.update { state ->
            when (field) {
                "displayName" -> state.copy(createDisplayName = value)
                "password" -> state.copy(createPassword = value)
                "confirmPassword" -> state.copy(createConfirmPassword = value)
                else -> state
            }
        }
    }

    fun createAccount() {
        val state = _uiState.value

        if (state.createPassword.length < 6) {
            _uiState.update { it.copy(error = app.getString(R.string.auth_error_short_password)) }
            return
        }
        if (state.createPassword != state.createConfirmPassword) {
            _uiState.update { it.copy(error = app.getString(R.string.auth_error_mismatch)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val salt = securityManager.generateSalt()
            val hash = securityManager.hashPassword(state.createPassword, salt)
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // Check if same password already exists
            val existing = accountDao.getAll()
            for (account in existing) {
                if (securityManager.verifyPassword(state.createPassword, account.passwordSalt, account.passwordHash)) {
                    _uiState.update { it.copy(isLoading = false, error = app.getString(R.string.auth_error_duplicate)) }
                    return@launch
                }
            }

            val accountId = accountDao.insert(
                AccountEntity(
                    displayName = state.createDisplayName.ifBlank { null },
                    passwordHash = hash,
                    passwordSalt = salt,
                    createdAt = now,
                    lastLoginAt = now
                )
            )

            securityManager.initSession(state.createPassword, salt, accountId)
            _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
        }
    }
}
