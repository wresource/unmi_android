package io.unmi.app.ui.domain.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.unmi.app.R
import io.unmi.app.ui.components.DomainListItem
import io.unmi.app.ui.navigation.AppRoute

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DomainListScreen(
    navController: NavController,
    viewModel: DomainListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.domain_list_selected, uiState.selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.domain_list_title)) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { navController.navigate(AppRoute.DomainCreate.route) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.domain_list_add))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.domain_list_search)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, stringResource(R.string.domain_list_clear))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.filterRegistrar != null || uiState.filterStatus != null,
                    onClick = { showFilterSheet = true },
                    label = { Text(stringResource(R.string.domain_list_filter)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.FilterList, null, Modifier.size(18.dp))
                    }
                )

                Box {
                    FilterChip(
                        selected = false,
                        onClick = { showSortMenu = true },
                        label = { Text(stringResource(uiState.sortBy.labelResId)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp))
                        }
                    )
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelResId)) },
                                onClick = { viewModel.updateSort(option); showSortMenu = false },
                                leadingIcon = if (uiState.sortBy == option) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Active filter chips
                uiState.filterRegistrar?.let { reg ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.updateFilterRegistrar(null) },
                        label = { Text(reg) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
                uiState.filterStatus?.let { status ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.updateFilterStatus(null) },
                        label = { Text(status) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
            }

            // Content
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.domains.isEmpty()) {
                // Empty state
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (uiState.searchQuery.isNotEmpty()) Icons.Outlined.SearchOff
                                    else Icons.Outlined.Dns,
                                    null,
                                    Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (uiState.searchQuery.isNotEmpty()) stringResource(R.string.domain_list_no_match) else stringResource(R.string.domain_list_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.domains, key = { it.id }) { domain ->
                        val isSelected = uiState.selectedIds.contains(domain.id)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (uiState.isSelectionMode) viewModel.toggleSelection(domain.id)
                                        else navController.navigate(AppRoute.DomainDetail.createRoute(domain.id))
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) viewModel.enterSelectionMode(domain.id)
                                    }
                                ),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedVisibility(visible = uiState.isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleSelection(domain.id) },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                DomainListItem(domain = domain, onClick = {})
                            }
                        }
                    }

                    // Bottom spacing for FAB
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(stringResource(R.string.domain_list_filter_title), style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.domain_list_filter_registrar), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = uiState.filterRegistrar == null,
                        onClick = { viewModel.updateFilterRegistrar(null); showFilterSheet = false },
                        label = { Text(stringResource(R.string.domain_list_all)) }
                    )
                }
                uiState.registrars.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { registrar ->
                            FilterChip(
                                selected = uiState.filterRegistrar == registrar,
                                onClick = { viewModel.updateFilterRegistrar(registrar); showFilterSheet = false },
                                label = { Text(registrar) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.domain_list_filter_status), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to stringResource(R.string.domain_list_all), "active" to stringResource(R.string.domain_list_active), "expired" to stringResource(R.string.domain_list_expired), "transferred" to stringResource(R.string.domain_list_transferred)).forEach { (s, l) ->
                        FilterChip(
                            selected = uiState.filterStatus == s,
                            onClick = { viewModel.updateFilterStatus(s); showFilterSheet = false },
                            label = { Text(l) }
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
