package io.unmi.app.ui.domain.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.unmi.app.R
import io.unmi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainEditScreen(
    domainId: Long?,
    navController: NavController,
    viewModel: DomainEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showStatusMenu by remember { mutableStateOf(false) }
    var showPriceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(if (uiState.isNew) stringResource(R.string.edit_add_title) else stringResource(R.string.edit_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isLoading
                    ) {
                        Text(stringResource(R.string.edit_save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // === Auto Identify Section ===
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = InfoBlue.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.edit_identify_title), style = MaterialTheme.typography.titleSmall, color = InfoBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.edit_identify_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val isAnyLoading = uiState.isQuerying || uiState.isFetchingPrice
                    Button(
                        onClick = { viewModel.autoIdentify() },
                        enabled = !isAnyLoading && uiState.domainName.contains("."),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnyLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.edit_identifying))
                        } else {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.edit_auto_identify))
                        }
                    }
                    uiState.queryMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = InfoBlue,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    uiState.priceMessage?.let {
                        if (it != uiState.queryMessage) {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = SafeGreen,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    // Show price list inline if available
                    if (uiState.priceList.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showPriceSheet = true }) {
                            Text(stringResource(R.string.edit_view_prices, uiState.priceList.size))
                        }
                    }
                }
            }

            // === Basic Info ===
            Text(stringResource(R.string.edit_basic_info), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.domainName,
                onValueChange = { viewModel.updateField("domainName", it) },
                label = { Text(stringResource(R.string.edit_domain_name)) },
                placeholder = { Text(stringResource(R.string.edit_domain_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState.isNew,
                shape = MaterialTheme.shapes.medium
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.tld,
                    onValueChange = { viewModel.updateField("tld", it) },
                    label = { Text(stringResource(R.string.edit_suffix)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = uiState.registrar,
                    onValueChange = { viewModel.updateField("registrar", it) },
                    label = { Text(stringResource(R.string.edit_registrar)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.status,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.edit_status)) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        listOf("active", "expired", "transferred", "pending").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = { viewModel.updateField("status", status); showStatusMenu = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.usageType,
                    onValueChange = { viewModel.updateField("usageType", it) },
                    label = { Text(stringResource(R.string.edit_usage)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // === Time ===
            Text(stringResource(R.string.edit_time_info), style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.registerDate,
                    onValueChange = { viewModel.updateField("registerDate", it) },
                    label = { Text(stringResource(R.string.edit_register_date)) },
                    placeholder = { Text(stringResource(R.string.edit_date_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = uiState.expireDate,
                    onValueChange = { viewModel.updateField("expireDate", it) },
                    label = { Text(stringResource(R.string.edit_expire_date)) },
                    placeholder = { Text(stringResource(R.string.edit_date_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.edit_auto_renew), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = uiState.autoRenew, onCheckedChange = { viewModel.updateBoolean("autoRenew", it) })
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // === Cost ===
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_cost_info), style = MaterialTheme.typography.titleMedium)
                if (!uiState.isNew) {
                    TextButton(onClick = {
                        viewModel.fetchPricing()
                        showPriceSheet = true
                    }) {
                        Text(stringResource(R.string.edit_check_price), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.purchasePrice,
                    onValueChange = { viewModel.updateField("purchasePrice", it) },
                    label = { Text(stringResource(R.string.edit_purchase_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = uiState.renewPrice,
                    onValueChange = { viewModel.updateField("renewPrice", it) },
                    label = { Text(stringResource(R.string.edit_renew_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.holdCost,
                    onValueChange = { viewModel.updateField("holdCost", it) },
                    label = { Text(stringResource(R.string.edit_hold_cost)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = uiState.currency,
                    onValueChange = { viewModel.updateField("currency", it) },
                    label = { Text(stringResource(R.string.edit_currency)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // === Valuation ===
            uiState.valuation?.let { val_ ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (val_.grade) {
                            "S", "A" -> SafeGreen.copy(alpha = 0.1f)
                            "B", "C" -> WarningOrange.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.detail_valuation), style = MaterialTheme.typography.titleSmall)
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (val_.grade) {
                                    "S" -> SafeGreen; "A" -> SafeGreen; "B" -> WarningOrange
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    stringResource(R.string.detail_grade, val_.grade),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "\$${String.format("%,.0f", val_.estimatedValue)} ${val_.currency}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.detail_confidence, val_.confidence),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        val_.factors.forEach { (factor, score) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(factor, style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.0f", score), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { viewModel.evaluateValue() }) {
                    Text(stringResource(R.string.edit_re_evaluate))
                }
            }

            if (uiState.valuation == null && uiState.domainName.contains(".")) {
                OutlinedButton(onClick = { viewModel.evaluateValue() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.edit_evaluate))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // === Extended ===
            Text(stringResource(R.string.edit_extended), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.dnsProvider,
                onValueChange = { viewModel.updateField("dnsProvider", it) },
                label = { Text(stringResource(R.string.edit_dns_provider)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = uiState.nameservers,
                onValueChange = { viewModel.updateField("nameservers", it) },
                label = { Text(stringResource(R.string.edit_nameservers)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.edit_privacy), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = uiState.privacyProtection, onCheckedChange = { viewModel.updateBoolean("privacyProtection", it) })
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.updateField("note", it) },
                label = { Text(stringResource(R.string.edit_note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Price comparison bottom sheet
    if (showPriceSheet) {
        ModalBottomSheet(onDismissRequest = { showPriceSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.edit_price_compare), style = MaterialTheme.typography.titleMedium)
                uiState.priceMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.edit_price_header), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                if (uiState.isFetchingPrice) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.priceList.isEmpty()) {
                    Text(stringResource(R.string.edit_no_price), modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.priceList) { price ->
                            Card(
                                onClick = {
                                    viewModel.applyPrice(price)
                                    showPriceSheet = false
                                }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(price.registrar, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(price.currency, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        PriceColumn(stringResource(R.string.edit_price_register), price.registerPrice)
                                        PriceColumn(stringResource(R.string.edit_price_renew), price.renewPrice)
                                        PriceColumn(stringResource(R.string.edit_price_transfer), price.transferPrice)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.edit_price_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PriceColumn(label: String, price: Double) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(String.format("%.2f", price), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
