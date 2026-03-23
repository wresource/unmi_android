package io.unmi.app.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.unmi.app.R
import io.unmi.app.ui.components.StatCard
import io.unmi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
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
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(stringResource(R.string.stats_overview), style = MaterialTheme.typography.titleMedium)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_total_domains),
                        value = "${uiState.totalDomains}",
                        icon = Icons.Outlined.Language,
                        color = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stats_total_renew_cost),
                        value = String.format("%.0f", uiState.totalRenewalCost),
                        icon = Icons.Outlined.Payments,
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_total_hold_cost),
                        value = String.format("%.0f", uiState.totalHoldCost),
                        icon = Icons.Outlined.AccountBalance,
                        color = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stats_total_spent),
                        value = String.format("%.0f", uiState.totalRenewalSpent),
                        icon = Icons.Outlined.Receipt,
                        color = GradeS,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Valuation row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_total_value),
                        value = String.format("$%,.0f", uiState.totalEstimatedValue),
                        icon = Icons.Outlined.Diamond,
                        color = GradeS,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stats_total_purchase),
                        value = String.format("%.0f", uiState.totalPurchaseCost),
                        icon = Icons.Outlined.ShoppingCart,
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_avg_renew),
                        value = if (uiState.totalDomains > 0)
                            String.format("%.1f", uiState.totalRenewalCost / uiState.totalDomains)
                        else "0",
                        icon = Icons.Outlined.PriceChange,
                        color = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stats_expiring_30d),
                        value = "${uiState.expiring30}",
                        icon = Icons.Outlined.Warning,
                        color = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Renewal budget section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.stats_renew_budget), style = MaterialTheme.typography.titleMedium)
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RenewalBudgetRow(stringResource(R.string.stats_renew_7d), uiState.expiring7, uiState.cost7Days, DangerRed)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RenewalBudgetRow(stringResource(R.string.stats_renew_30d), uiState.expiring30, uiState.cost30Days, WarningOrange)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RenewalBudgetRow(stringResource(R.string.stats_renew_90d), uiState.expiring90, uiState.cost90Days, InfoBlue)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RenewalBudgetRow(stringResource(R.string.stats_renew_365d), uiState.expiring365, uiState.cost365Days, SafeGreen)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.stats_expiry_preview), style = MaterialTheme.typography.titleMedium)
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ExpiryRow(stringResource(R.string.stats_expire_7d), uiState.expiring7, DangerRed)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ExpiryRow(stringResource(R.string.stats_expire_30d), uiState.expiring30, WarningOrange)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ExpiryRow(stringResource(R.string.stats_expire_90d), uiState.expiring90, InfoBlue)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ExpiryRow(stringResource(R.string.stats_expire_365d), uiState.expiring365, SafeGreen)
                    }
                }
            }

            if (uiState.registrars.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.stats_registrar_dist), style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.registrars.forEach { registrar ->
                                val count = uiState.allDomains.count { it.registrar == registrar }
                                val cost = uiState.allDomains
                                    .filter { it.registrar == registrar }
                                    .sumOf { it.renewPrice }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(registrar, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            stringResource(R.string.stats_domains_count, count),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        String.format("%.0f", cost),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (registrar != uiState.registrars.last()) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            // TLD distribution
            if (uiState.allDomains.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.stats_tld_dist), style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val tldGroups = uiState.allDomains.groupBy { it.tld }
                                .entries.sortedByDescending { it.value.size }
                            tldGroups.forEach { (tld, domains) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(tld, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        stringResource(R.string.stats_count, domains.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (tld != tldGroups.last().key) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ExpiryRow(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.stats_count, count),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RenewalBudgetRow(label: String, count: Int, cost: Double, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.stats_domains_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            String.format("%.0f", cost),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (cost > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
