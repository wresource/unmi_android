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
        enableEdgeToEdge()

        // Hilt injects AFTER super.onCreate(), so now it's safe to use
        val startRoute = runBlocking { resolveStartRoute() }

        setContent {
            val settings by appPreferences.settingsFlow.collectAsState(initial = AppSettings())

            LaunchedEffect(settings.language) {
                val locales = when (settings.language) {
                    "zh" -> LocaleListCompat.forLanguageTags("zh")
                    "en" -> LocaleListCompat.forLanguageTags("en")
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(locales)
            }

            UnmiTheme(darkTheme = settings.darkMode) {
                AppNavigation(startDestination = startRoute)
            }
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

        // Restore session
        if (session.isGuest) {
            securityManager.initGuestSession(account.id)
        } else {
            securityManager.initGuestSession(account.id)
        }

        appPreferences.saveSession(account.id, session.isGuest)
        return AppRoute.Home.route
    }
}
