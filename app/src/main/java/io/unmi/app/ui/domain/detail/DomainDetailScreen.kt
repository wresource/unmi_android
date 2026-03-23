package io.unmi.app.ui.domain.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.unmi.app.R
import io.unmi.app.ui.navigation.AppRoute
import io.unmi.app.ui.theme.DangerRed
import io.unmi.app.ui.theme.SafeGreen
import io.unmi.app.ui.theme.WarningOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainDetailScreen(
    domainId: Long,
    navController: NavController,
    viewModel: DomainDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(AppRoute.DomainEdit.createRoute(domainId))
                    }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                    IconButton(onClick = {
                        navController.navigate(AppRoute.WhoisQuery.createRoute(
                            uiState.domain?.domainName ?: ""
                        ))
                    }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.tools_whois))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.domain == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.detail_not_found))
            }
        } else {
            val domain = uiState.domain!!
            val daysRemaining = try {
                val expireDate = LocalDate.parse(domain.expireDate, DateTimeFormatter.ISO_LOCAL_DATE)
                ChronoUnit.DAYS.between(LocalDate.now(), expireDate).toInt()
            } catch (e: Exception) { -1 }

            val statusColor = when {
                daysRemaining < 0 -> DangerRed
                daysRemaining <= 7 -> DangerRed
                daysRemaining <= 30 -> WarningOrange
                else -> SafeGreen
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Summary card
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = statusColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = domain.domainName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = statusColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = domain.status,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Text(
                                    text = "${stringResource(R.string.whois_exp_date)}: ${domain.expireDate}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (daysRemaining >= 0) stringResource(R.string.detail_days_remaining, daysRemaining) else stringResource(R.string.detail_days_expired),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Basic info
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.detail_basic_info), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(stringResource(R.string.detail_registrar), domain.registrar ?: stringResource(R.string.detail_unknown))
                            DetailRow(stringResource(R.string.detail_register_date), domain.registerDate ?: stringResource(R.string.detail_unknown))
                            DetailRow(stringResource(R.string.detail_suffix), domain.tld)
                            DetailRow(stringResource(R.string.detail_auto_renew), if (domain.autoRenew) stringResource(R.string.detail_yes) else stringResource(R.string.detail_no))
                            DetailRow(stringResource(R.string.detail_dns_provider), domain.dnsProvider ?: stringResource(R.string.detail_unknown))
                            DetailRow(stringResource(R.string.detail_privacy), if (domain.privacyProtection) stringResource(R.string.detail_enabled) else stringResource(R.string.detail_disabled))
                            DetailRow(stringResource(R.string.detail_usage), domain.usageType ?: stringResource(R.string.detail_unknown))
                        }
                    }
                }

                // Cost info
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.detail_cost_info), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(stringResource(R.string.detail_purchase_price), "${domain.currency} ${String.format("%.2f", domain.purchasePrice)}")
                            DetailRow(stringResource(R.string.detail_renew_price), "${domain.currency} ${String.format("%.2f", domain.renewPrice)}")
                            DetailRow(stringResource(R.string.detail_hold_cost), "${domain.currency} ${String.format("%.2f", domain.holdCost)}")
                        }
                    }
                }

                // Valuation info
                if (domain.estimatedValue > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.detail_valuation), style = MaterialTheme.typography.titleMedium)
                                    domain.valueGrade?.let { grade ->
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                stringResource(R.string.detail_grade, grade),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "\$${String.format("%,.0f", domain.estimatedValue)} ${domain.valueCurrency}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                domain.valueConfidence?.let {
                                    Text(
                                        stringResource(R.string.detail_confidence, it),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Tags
                if (uiState.tags.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.detail_tags), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.tags.forEach { tag ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(tag.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Note
                if (!domain.note.isNullOrBlank()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.detail_note), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(domain.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Renewal records
                item {
                    Text(stringResource(R.string.detail_renewal_records), style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.renewalRecords.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.detail_no_records),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.renewalRecords) { record ->
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(record.renewalDate, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${record.renewalYears} ${stringResource(R.string.per_year).removePrefix("/")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${record.currency} ${String.format("%.2f", record.renewalPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDomain()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
