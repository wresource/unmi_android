package io.unmi.app.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.repository.DomainRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeUiState(
    val totalDomains: Int = 0,
    val expiring7Days: Int = 0,
    val expiring30Days: Int = 0,
    val monthlyBudget: Double = 0.0,
    val yearlyBudget: Double = 0.0,
    val recentDomains: List<DomainEntity> = emptyList(),
    val expiredDomains: List<DomainEntity> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val domainRepository: DomainRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        domainRepository.observeCount(),
        domainRepository.observeExpiringCount(7),
        domainRepository.observeExpiringCount(30),
        domainRepository.observeRenewalCostBefore(30),
        domainRepository.observeRenewalCostBefore(365),
        domainRepository.observeAll(),
        domainRepository.observeExpired()
    ) { values ->
        HomeUiState(
            totalDomains = values[0] as Int,
            expiring7Days = values[1] as Int,
            expiring30Days = values[2] as Int,
            monthlyBudget = (values[3] as? Double) ?: 0.0,
            yearlyBudget = (values[4] as? Double) ?: 0.0,
            recentDomains = (values[5] as List<*>).filterIsInstance<DomainEntity>().take(5),
            expiredDomains = (values[6] as List<*>).filterIsInstance<DomainEntity>().take(5)
        )
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
