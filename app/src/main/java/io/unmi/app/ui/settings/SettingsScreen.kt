package io.unmi.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.unmi.app.R
import io.unmi.app.ui.navigation.AppRoute
import io.unmi.app.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val passwordChangeResult by viewModel.passwordChangeResult.collectAsStateWithLifecycle()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Security section
            item { SectionHeader(stringResource(R.string.settings_security)) }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.settings_change_password),
                    onClick = { showPasswordDialog = true }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(R.string.settings_biometric),
                    subtitle = stringResource(R.string.settings_biometric_desc),
                    checked = settings.biometricEnabled,
                    onCheckedChange = { viewModel.toggleBiometric(it) }
                )
            }

            // Display section
            item {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant)
                SectionHeader(stringResource(R.string.settings_display))
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    checked = settings.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = when (settings.language) {
                        "zh" -> stringResource(R.string.settings_language_zh)
                        "en" -> stringResource(R.string.settings_language_en)
                        else -> stringResource(R.string.settings_language_system)
                    },
                    onClick = { showLanguageDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.AttachMoney,
                    title = stringResource(R.string.settings_currency),
                    subtitle = settings.defaultCurrency
                )
            }

            // Other section
            item {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant)
                SectionHeader(stringResource(R.string.settings_other))
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_about),
                    onClick = { navController.navigate(AppRoute.About.route) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Language dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    listOf(
                        "system" to stringResource(R.string.settings_language_system),
                        "zh" to stringResource(R.string.settings_language_zh),
                        "en" to stringResource(R.string.settings_language_en)
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(value)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.language == value,
                                onClick = {
                                    viewModel.setLanguage(value)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Password dialog
    if (showPasswordDialog) {
        var oldPwd by remember { mutableStateOf("") }
        var newPwd by remember { mutableStateOf("") }
        var confirmPwd by remember { mutableStateOf("") }
        val successMsg = stringResource(R.string.settings_password_success)

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; viewModel.clearPasswordChangeResult() },
            title = { Text(stringResource(R.string.settings_change_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPwd, onValueChange = { oldPwd = it },
                        label = { Text(stringResource(R.string.settings_password_old)) },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = newPwd, onValueChange = { newPwd = it },
                        label = { Text(stringResource(R.string.settings_password_new)) },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = confirmPwd, onValueChange = { confirmPwd = it },
                        label = { Text(stringResource(R.string.settings_password_confirm)) },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium
                    )
                    if (passwordChangeResult != null) {
                        Text(
                            passwordChangeResult!!,
                            color = if (passwordChangeResult == successMsg) SafeGreen
                                    else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.changePassword(oldPwd, newPwd, confirmPwd) }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; viewModel.clearPasswordChangeResult() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Consistent settings item with icon, title, optional subtitle, clickable */
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = if (onClick != null) {
            { Icon(Icons.Default.KeyboardArrowRight, null,
                   tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        } else null,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    )
}

/** Toggle item with switch */
@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
