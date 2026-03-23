package io.unmi.app.ui.whois

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.unmi.app.R
import io.unmi.app.ui.theme.DangerRed
import io.unmi.app.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisScreen(
    navController: NavController,
    viewModel: WhoisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRawJson by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whois_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    label = { Text(stringResource(R.string.whois_input)) },
                    placeholder = { Text(stringResource(R.string.whois_input_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.queryWhois() }
                    )
                )
            }

            item {
                Button(
                    onClick = { viewModel.queryWhois() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.query.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.whois_querying))
                    } else {
                        Text(stringResource(R.string.whois_query))
                    }
                }
            }

            if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Structured RDAP results
            uiState.rdapResult?.let { result ->
                item {
                    Text(stringResource(R.string.whois_result), style = MaterialTheme.typography.titleMedium)
                }

                // Summary card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                result.domainName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.whois_source),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Registration info
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.whois_reg_info), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(stringResource(R.string.whois_registrar), result.registrar ?: stringResource(R.string.whois_unknown))
                            InfoRow(stringResource(R.string.whois_reg_date), result.registrationDate ?: stringResource(R.string.whois_unknown))
                            InfoRow(stringResource(R.string.whois_exp_date), result.expirationDate ?: stringResource(R.string.whois_unknown))
                            InfoRow(stringResource(R.string.whois_last_changed), result.lastChangedDate ?: stringResource(R.string.whois_unknown))
                            InfoRow(stringResource(R.string.whois_dnssec), if (result.dnssecSigned) stringResource(R.string.whois_dnssec_signed) else stringResource(R.string.whois_dnssec_unsigned))
                        }
                    }
                }

                // Status
                if (result.status.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.whois_status), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                result.status.forEach { status ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = SafeGreen
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(status, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Nameservers
                if (result.nameservers.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.whois_ns), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                result.nameservers.forEach { ns ->
                                    Text(ns, style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // Raw JSON toggle
                item {
                    TextButton(onClick = { showRawJson = !showRawJson }) {
                        Text(if (showRawJson) stringResource(R.string.whois_hide_raw) else stringResource(R.string.whois_show_raw))
                    }
                }

                if (showRawJson && uiState.rawResult != null) {
                    item {
                        Card {
                            SelectionContainer {
                                Text(
                                    text = uiState.rawResult!!,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Recent queries
            if (uiState.recentQueries.isNotEmpty() && uiState.rdapResult == null) {
                item {
                    Text(stringResource(R.string.whois_recent), style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.recentQueries.take(10)) { log ->
                    Card(
                        onClick = {
                            viewModel.updateQuery(log.domainName)
                            viewModel.queryWhois(log.domainName)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(log.domainName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${log.provider ?: "RDAP"} - ${log.queriedAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (log.queryStatus == "success")
                                    SafeGreen.copy(alpha = 0.1f)
                                else DangerRed.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = if (log.queryStatus == "success") stringResource(R.string.whois_success) else stringResource(R.string.whois_failed),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.queryStatus == "success") SafeGreen else DangerRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
