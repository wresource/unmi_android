package io.unmi.app.ui.unlock

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.R
import io.unmi.app.data.local.datastore.AppPreferences
import io.unmi.app.data.local.db.dao.AccountDao
import io.unmi.app.data.local.db.dao.DomainDao
import io.unmi.app.data.local.db.entity.AccountEntity
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class UnlockUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isUnlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val hasAccounts: Boolean = false,
    val createDisplayName: String = "",
    val createPassword: String = "",
    val createConfirmPassword: String = ""
)

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val app: Application,
    private val accountDao: AccountDao,
    private val domainDao: DomainDao,
    private val securityManager: SecurityManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    companion object {
        private const val REVIEW_PASSWORD = "unmi@review2026"
        private const val REVIEW_DISPLAY_NAME = "Reviewer"
        private const val SESSION_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureReviewAccount()
            val count = accountDao.getCount()
            _uiState.update { it.copy(isLoading = false, hasAccounts = count > 0) }
        }
    }

    /**
     * Create built-in review account on first launch for Google Play review.
     */
    private suspend fun ensureReviewAccount() {
        val accounts = accountDao.getAll()
        val reviewExists = accounts.any { account ->
            !account.isGuest && account.displayName == REVIEW_DISPLAY_NAME &&
            securityManager.verifyPassword(REVIEW_PASSWORD, account.passwordSalt, account.passwordHash)
        }
        if (!reviewExists && accounts.isEmpty()) {
            val salt = securityManager.generateSalt()
            val hash = securityManager.hashPassword(REVIEW_PASSWORD, salt)
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val accountId = accountDao.insert(
                AccountEntity(
                    displayName = REVIEW_DISPLAY_NAME,
                    passwordHash = hash,
                    passwordSalt = salt,
                    isGuest = false,
                    createdAt = now
                )
            )
            // Insert demo domains for the review account
            insertDemoDomains(accountId)
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
                if (account.passwordHash.isNotBlank() &&
                    securityManager.verifyPassword(password, account.passwordSalt, account.passwordHash)) {
                    matched = account
                    break
                }
            }

            if (matched != null) {
                securityManager.initSession(password, matched.passwordSalt, matched.id)
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                accountDao.update(matched.copy(lastLoginAt = now))
                appPreferences.saveSession(matched.id, false)
                _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
            } else {
                val attempts = _uiState.value.failedAttempts + 1
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (attempts >= 5) app.getString(R.string.auth_error_too_many)
                                else app.getString(R.string.auth_error_wrong_password),
                        failedAttempts = attempts
                    )
                }
            }
        }
    }

    /**
     * Guest login - no password, no encryption.
     */
    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Find or create guest account
            var guest = accountDao.getGuestAccount()
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            if (guest == null) {
                val guestId = accountDao.insert(
                    AccountEntity(
                        displayName = app.getString(R.string.settings_guest_label),
                        passwordHash = "",
                        passwordSalt = "",
                        isGuest = true,
                        createdAt = now,
                        lastLoginAt = now
                    )
                )
                guest = accountDao.getById(guestId)
                // Insert demo domains
                insertDemoDomains(guestId)
            } else {
                accountDao.update(guest.copy(lastLoginAt = now))
            }

            securityManager.initGuestSession(guest!!.id)
            appPreferences.saveSession(guest.id, true)
            _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
        }
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

            // Check for duplicate password
            val existing = accountDao.getAll()
            for (account in existing) {
                if (account.passwordHash.isNotBlank() &&
                    securityManager.verifyPassword(state.createPassword, account.passwordSalt, account.passwordHash)) {
                    _uiState.update { it.copy(isLoading = false, error = app.getString(R.string.auth_error_duplicate)) }
                    return@launch
                }
            }

            val accountId = accountDao.insert(
                AccountEntity(
                    displayName = state.createDisplayName.ifBlank { null },
                    passwordHash = hash,
                    passwordSalt = salt,
                    isGuest = false,
                    createdAt = now,
                    lastLoginAt = now
                )
            )

            securityManager.initSession(state.createPassword, salt, accountId)
            appPreferences.saveSession(accountId, false)
            _uiState.update { it.copy(isLoading = false, isUnlocked = true) }
        }
    }

    /**
     * Insert demo domains for new accounts.
     */
    private suspend fun insertDemoDomains(accountId: Long) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val today = LocalDate.now()

        val demos = listOf(
            DomainEntity(
                accountId = accountId,
                domainName = "example.com",
                tld = ".com",
                registrar = "Cloudflare",
                registerDate = "2020-01-15",
                expireDate = today.plusDays(45).toString(),
                autoRenew = true,
                status = "active",
                purchasePrice = 8.99,
                renewPrice = 10.44,
                currency = "USD",
                dnsProvider = "Cloudflare",
                estimatedValue = 5000.0,
                valueCurrency = "USD",
                valueGrade = "B",
                createdAt = now,
                updatedAt = now
            ),
            DomainEntity(
                accountId = accountId,
                domainName = "myapp.io",
                tld = ".io",
                registrar = "Namecheap",
                registerDate = "2022-06-01",
                expireDate = today.plusDays(120).toString(),
                status = "active",
                purchasePrice = 34.98,
                renewPrice = 45.0,
                currency = "USD",
                dnsProvider = "Cloudflare",
                estimatedValue = 800.0,
                valueCurrency = "USD",
                valueGrade = "C",
                createdAt = now,
                updatedAt = now
            ),
            DomainEntity(
                accountId = accountId,
                domainName = "shop.cn",
                tld = ".cn",
                registrar = "阿里云",
                registerDate = "2023-03-10",
                expireDate = today.plusDays(8).toString(),
                status = "active",
                purchasePrice = 29.0,
                renewPrice = 39.0,
                currency = "CNY",
                estimatedValue = 3000.0,
                valueCurrency = "USD",
                valueGrade = "B",
                createdAt = now,
                updatedAt = now
            )
        )

        for (domain in demos) {
            try { domainDao.insert(domain) } catch (_: Exception) { }
        }
    }
}
