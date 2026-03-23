package io.unmi.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "unmi_settings")

data class AppSettings(
    val isFirstLaunch: Boolean = true,
    val autoLockMinutes: Int = 5,
    val biometricEnabled: Boolean = false,
    val darkMode: Boolean = false,
    val defaultCurrency: String = "CNY",
    val dateFormat: String = "yyyy-MM-dd",
    val duplicateStrategy: String = "skip",
    val githubUrl: String = "https://github.com",
    val projectUrl: String = "",
    val domainListUrl: String = "",
    val cloudSyncEnabled: Boolean = false,
    val lastSyncTime: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val language: String = "system"
)

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val DUPLICATE_STRATEGY = stringPreferencesKey("duplicate_strategy")
        val GITHUB_URL = stringPreferencesKey("github_url")
        val PROJECT_URL = stringPreferencesKey("project_url")
        val DOMAIN_LIST_URL = stringPreferencesKey("domain_list_url")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val PASSWORD_SALT = stringPreferencesKey("password_salt")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            isFirstLaunch = prefs[IS_FIRST_LAUNCH] ?: true,
            autoLockMinutes = prefs[AUTO_LOCK_MINUTES] ?: 5,
            biometricEnabled = prefs[BIOMETRIC_ENABLED] ?: false,
            darkMode = prefs[DARK_MODE] ?: false,
            defaultCurrency = prefs[DEFAULT_CURRENCY] ?: "CNY",
            dateFormat = prefs[DATE_FORMAT] ?: "yyyy-MM-dd",
            duplicateStrategy = prefs[DUPLICATE_STRATEGY] ?: "skip",
            githubUrl = prefs[GITHUB_URL] ?: "https://github.com",
            projectUrl = prefs[PROJECT_URL] ?: "",
            domainListUrl = prefs[DOMAIN_LIST_URL] ?: "",
            cloudSyncEnabled = prefs[CLOUD_SYNC_ENABLED] ?: false,
            lastSyncTime = prefs[LAST_SYNC_TIME] ?: "",
            passwordHash = prefs[PASSWORD_HASH] ?: "",
            passwordSalt = prefs[PASSWORD_SALT] ?: "",
            language = prefs[LANGUAGE] ?: "system"
        )
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[IS_FIRST_LAUNCH] ?: true }
    val passwordHash: Flow<String> = dataStore.data.map { it[PASSWORD_HASH] ?: "" }
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "system" }

    suspend fun setFirstLaunch(value: Boolean) {
        dataStore.edit { it[IS_FIRST_LAUNCH] = value }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        dataStore.edit { it[AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setDefaultCurrency(currency: String) {
        dataStore.edit { it[DEFAULT_CURRENCY] = currency }
    }

    suspend fun setDateFormat(format: String) {
        dataStore.edit { it[DATE_FORMAT] = format }
    }

    suspend fun setDuplicateStrategy(strategy: String) {
        dataStore.edit { it[DUPLICATE_STRATEGY] = strategy }
    }

    suspend fun setGithubUrl(url: String) {
        dataStore.edit { it[GITHUB_URL] = url }
    }

    suspend fun setProjectUrl(url: String) {
        dataStore.edit { it[PROJECT_URL] = url }
    }

    suspend fun setDomainListUrl(url: String) {
        dataStore.edit { it[DOMAIN_LIST_URL] = url }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[CLOUD_SYNC_ENABLED] = enabled }
    }

    suspend fun setLastSyncTime(time: String) {
        dataStore.edit { it[LAST_SYNC_TIME] = time }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[LANGUAGE] = language }
    }

    suspend fun setPasswordCredentials(hash: String, salt: String) {
        dataStore.edit {
            it[PASSWORD_HASH] = hash
            it[PASSWORD_SALT] = salt
        }
    }
}
