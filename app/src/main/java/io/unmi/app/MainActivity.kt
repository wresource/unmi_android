package io.unmi.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.AndroidEntryPoint
import io.unmi.app.data.local.datastore.AppPreferences
import io.unmi.app.data.local.datastore.AppSettings
import io.unmi.app.data.local.db.dao.AccountDao
import io.unmi.app.security.SecurityManager
import io.unmi.app.ui.navigation.AppNavigation
import io.unmi.app.ui.navigation.AppRoute
import io.unmi.app.ui.theme.UnmiTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var securityManager: SecurityManager
    @Inject lateinit var accountDao: AccountDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read settings synchronously BEFORE setContent to avoid flicker
        val initialSettings = runBlocking { appPreferences.settingsFlow.first() }

        // Apply locale ONCE synchronously — this prevents the infinite recreation loop.
        // setApplicationLocales() recreates the Activity if locale differs from current.
        // By comparing with the current locale first, we only recreate when truly needed.
        applyLocaleIfChanged(initialSettings.language)

        val startRoute = runBlocking { resolveStartRoute() }

        enableEdgeToEdge()
        setContent {
            val settings by appPreferences.settingsFlow.collectAsState(initial = initialSettings)

            // Track which language we've already applied to avoid re-triggering
            var appliedLanguage by remember { mutableStateOf(initialSettings.language) }
            LaunchedEffect(settings.language) {
                if (settings.language != appliedLanguage) {
                    appliedLanguage = settings.language
                    // This will recreate Activity — no further code runs after this
                    applyLocaleIfChanged(settings.language)
                }
            }

            UnmiTheme(darkTheme = settings.darkMode) {
                AppNavigation(startDestination = startRoute)
            }
        }
    }

    /**
     * Only call setApplicationLocales if the desired locale is different from current.
     * This prevents the infinite Activity recreation loop.
     */
    private fun applyLocaleIfChanged(language: String) {
        val desired = when (language) {
            "zh" -> LocaleListCompat.forLanguageTags("zh")
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        val current = AppCompatDelegate.getApplicationLocales()
        if (current != desired) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
    }

    private suspend fun resolveStartRoute(): String {
        val session = appPreferences.sessionFlow.first() ?: return AppRoute.Unlock.route

        val elapsed = System.currentTimeMillis() - session.loginTime
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        if (elapsed >= thirtyDaysMs) {
            appPreferences.clearSession()
            return AppRoute.Unlock.route
        }

        val account = accountDao.getById(session.accountId) ?: run {
            appPreferences.clearSession()
            return AppRoute.Unlock.route
        }

        if (session.isGuest) {
            securityManager.initGuestSession(account.id)
        } else {
            securityManager.initGuestSession(account.id)
        }

        appPreferences.saveSession(account.id, session.isGuest)
        return AppRoute.Home.route
    }
}
