package io.unmi.app.ui.unlock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.unmi.app.R
import io.unmi.app.security.BiometricHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = if (uiState.hasAccounts) 0 else 1,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    // Auto-trigger biometric prompt
    LaunchedEffect(uiState.showBiometricPrompt) {
        if (uiState.showBiometricPrompt) {
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            BiometricHelper.authenticate(
                activity = activity,
                title = context.getString(R.string.biometric_title),
                subtitle = context.getString(R.string.biometric_subtitle),
                negativeButtonText = context.getString(R.string.biometric_cancel),
                onSuccess = { viewModel.onBiometricSuccess() },
                onError = { viewModel.dismissBiometricPrompt() }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo - compact
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Shield, null,
                        Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Tab row: Login / Register
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.padding(horizontal = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.auth_login)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.auth_create_account)) }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Pager content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> LoginPage(uiState, viewModel)
                        1 -> RegisterPage(uiState, viewModel)
                    }
                }
            }

            // Guest mode button
            OutlinedButton(
                onClick = { viewModel.loginAsGuest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Outlined.PersonOutline, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.auth_guest))
            }

            // Footer hint
            Text(
                stringResource(R.string.auth_guest_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun LoginPage(uiState: UnlockUiState, viewModel: UnlockViewModel) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (password.isNotBlank()) viewModel.login(password)
            }),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.error != null,
            shape = MaterialTheme.shapes.medium
        )

        if (uiState.error != null) {
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.login(password) },
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = password.isNotBlank() && !uiState.isLoading && uiState.failedAttempts < 5,
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.auth_login))
                }
            }

            // Biometric button
            if (uiState.biometricEnabled) {
                val context = LocalContext.current
                FilledTonalIconButton(
                    onClick = {
                        val activity = context as? FragmentActivity ?: return@FilledTonalIconButton
                        BiometricHelper.authenticate(
                            activity = activity,
                            title = context.getString(R.string.biometric_title),
                            subtitle = context.getString(R.string.biometric_subtitle),
                            negativeButtonText = context.getString(R.string.biometric_cancel),
                            onSuccess = { viewModel.onBiometricSuccess() },
                            onError = { }
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun RegisterPage(uiState: UnlockUiState, viewModel: UnlockViewModel) {
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = uiState.createDisplayName,
            onValueChange = { viewModel.updateCreateField("displayName", it) },
            label = { Text(stringResource(R.string.auth_username_optional)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = uiState.createPassword,
            onValueChange = { viewModel.updateCreateField("password", it) },
            label = { Text(stringResource(R.string.auth_set_password)) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = uiState.createConfirmPassword,
            onValueChange = { viewModel.updateCreateField("confirmPassword", it) },
            label = { Text(stringResource(R.string.auth_confirm_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.error != null,
            shape = MaterialTheme.shapes.medium
        )

        if (uiState.error != null) {
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { viewModel.createAccount() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !uiState.isLoading && uiState.createPassword.isNotBlank(),
            shape = MaterialTheme.shapes.medium
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.auth_create_account))
            }
        }

        Text(
            stringResource(R.string.auth_password_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
