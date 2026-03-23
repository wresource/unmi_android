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
import io.unmi.app.security.SecurityManager
import io.unmi.app.ui.navigation.AppNavigation
import io.unmi.app.ui.navigation.AppRoute
import io.unmi.app.ui.theme.UnmiTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                AppNavigation(startDestination = AppRoute.Unlock.route)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        securityManager.clearSession()
    }
}
