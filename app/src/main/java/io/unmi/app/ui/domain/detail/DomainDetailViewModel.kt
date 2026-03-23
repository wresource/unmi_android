package io.unmi.app.ui.domain.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.local.db.entity.RenewalRecordEntity
import io.unmi.app.data.local.db.entity.TagEntity
import io.unmi.app.data.repository.DomainRepository
import io.unmi.app.data.repository.StatisticsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DomainDetailUiState(
    val domain: DomainEntity? = null,
    val tags: List<TagEntity> = emptyList(),
    val renewalRecords: List<RenewalRecordEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class DomainDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domainRepository: DomainRepository,
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val domainId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow(DomainDetailUiState())
    val uiState: StateFlow<DomainDetailUiState> = _uiState.asStateFlow()

    init {
        loadDomain()
    }

    private fun loadDomain() {
        viewModelScope.launch {
            val domain = domainRepository.getById(domainId)
            _uiState.update { it.copy(domain = domain, isLoading = false) }
        }

        viewModelScope.launch {
            domainRepository.observeTagsForDomain(domainId).collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }

        viewModelScope.launch {
            statisticsRepository.observeRenewalRecordsByDomain(domainId).collect { records ->
                _uiState.update { it.copy(renewalRecords = records) }
            }
        }
    }

    fun deleteDomain() {
        viewModelScope.launch {
            _uiState.value.domain?.let {
                domainRepository.delete(it)
                _uiState.update { state -> state.copy(isDeleted = true) }
            }
        }
    }
}
