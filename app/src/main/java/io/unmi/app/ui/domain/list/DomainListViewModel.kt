package io.unmi.app.ui.domain.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.R
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.repository.DomainRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DomainListUiState(
    val domains: List<DomainEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortBy: SortOption = SortOption.UPDATED_DESC,
    val filterRegistrar: String? = null,
    val filterStatus: String? = null,
    val registrars: List<String> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

enum class SortOption(val labelResId: Int) {
    UPDATED_DESC(R.string.sort_updated),
    EXPIRE_ASC(R.string.sort_expire),
    NAME_ASC(R.string.sort_name),
    PRICE_DESC(R.string.sort_price)
}

@HiltViewModel
class DomainListViewModel @Inject constructor(
    private val domainRepository: DomainRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(SortOption.UPDATED_DESC)
    private val _filterRegistrar = MutableStateFlow<String?>(null)
    private val _filterStatus = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DomainListUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) domainRepository.observeAll()
            else domainRepository.searchByName(query)
        },
        _sortBy,
        _filterRegistrar,
        _filterStatus,
        domainRepository.observeAllRegistrars(),
        _selectedIds,
        _isSelectionMode
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allDomains = values[0] as List<DomainEntity>
        val sortBy = values[1] as SortOption
        val filterRegistrar = values[2] as String?
        val filterStatus = values[3] as String?
        val registrars = values[4] as List<String>
        val selectedIds = values[5] as Set<Long>
        val isSelectionMode = values[6] as Boolean

        val filtered = allDomains
            .filter { d -> filterRegistrar == null || d.registrar == filterRegistrar }
            .filter { d -> filterStatus == null || d.status == filterStatus }

        val sorted = when (sortBy) {
            SortOption.UPDATED_DESC -> filtered.sortedByDescending { it.updatedAt }
            SortOption.EXPIRE_ASC -> filtered.sortedBy { it.expireDate }
            SortOption.NAME_ASC -> filtered.sortedBy { it.domainName }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.renewPrice }
        }

        DomainListUiState(
            domains = sorted,
            isLoading = false,
            searchQuery = _searchQuery.value,
            sortBy = sortBy,
            filterRegistrar = filterRegistrar,
            filterStatus = filterStatus,
            registrars = registrars,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DomainListUiState()
    )

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSort(sort: SortOption) { _sortBy.value = sort }
    fun updateFilterRegistrar(registrar: String?) { _filterRegistrar.value = registrar }
    fun updateFilterStatus(status: String?) { _filterStatus.value = status }

    fun toggleSelection(id: Long) {
        _selectedIds.update { ids ->
            if (ids.contains(id)) ids - id else ids + id
        }
        if (_selectedIds.value.isEmpty()) _isSelectionMode.value = false
    }

    fun enterSelectionMode(id: Long) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            domainRepository.deleteByIds(_selectedIds.value.toList())
            exitSelectionMode()
        }
    }
}
