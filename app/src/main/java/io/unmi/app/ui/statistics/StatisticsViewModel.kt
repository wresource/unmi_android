package io.unmi.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.repository.DomainRepository
import io.unmi.app.data.repository.StatisticsRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class StatisticsUiState(
    val totalDomains: Int = 0,
    val expiring7: Int = 0,
    val expiring30: Int = 0,
    val expiring90: Int = 0,
    val expiring365: Int = 0,
    val totalRenewalCost: Double = 0.0,
    val totalHoldCost: Double = 0.0,
    val totalRenewalSpent: Double = 0.0,
    val totalEstimatedValue: Double = 0.0,
    val totalPurchaseCost: Double = 0.0,
    val cost7Days: Double = 0.0,
    val cost30Days: Double = 0.0,
    val cost90Days: Double = 0.0,
    val cost365Days: Double = 0.0,
    val allDomains: List<DomainEntity> = emptyList(),
    val registrars: List<String> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val domainRepository: DomainRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        statisticsRepository.observeDomainCount(),
        statisticsRepository.observeExpiringCount(7),
        statisticsRepository.observeExpiringCount(30),
        statisticsRepository.observeExpiringCount(90),
        statisticsRepository.observeExpiringCount(365),
        statisticsRepository.observeTotalRenewalCost(),
        statisticsRepository.observeTotalHoldCost(),
        statisticsRepository.observeTotalRenewalSpent(),
        statisticsRepository.observeAllDomains(),
        statisticsRepository.observeAllRegistrars()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allDomains = (values[8] as List<*>).filterIsInstance<DomainEntity>()

        // Calculate derived stats from domain list
        val totalEstimatedValue = allDomains.sumOf { it.estimatedValue }
        val totalPurchaseCost = allDomains.sumOf { it.purchasePrice }

        val today = LocalDate.now()
        val cost7Days = allDomains.filter {
            try {
                val exp = LocalDate.parse(it.expireDate)
                it.status == "active" && exp.isAfter(today) && !exp.isAfter(today.plusDays(7))
            } catch (_: Exception) { false }
        }.sumOf { it.renewPrice }

        val cost30Days = allDomains.filter {
            try {
                val exp = LocalDate.parse(it.expireDate)
                it.status == "active" && exp.isAfter(today) && !exp.isAfter(today.plusDays(30))
            } catch (_: Exception) { false }
        }.sumOf { it.renewPrice }

        val cost90Days = allDomains.filter {
            try {
                val exp = LocalDate.parse(it.expireDate)
                it.status == "active" && exp.isAfter(today) && !exp.isAfter(today.plusDays(90))
            } catch (_: Exception) { false }
        }.sumOf { it.renewPrice }

        val cost365Days = allDomains.filter {
            try {
                val exp = LocalDate.parse(it.expireDate)
                it.status == "active" && exp.isAfter(today) && !exp.isAfter(today.plusDays(365))
            } catch (_: Exception) { false }
        }.sumOf { it.renewPrice }

        StatisticsUiState(
            totalDomains = values[0] as Int,
            expiring7 = values[1] as Int,
            expiring30 = values[2] as Int,
            expiring90 = values[3] as Int,
            expiring365 = values[4] as Int,
            totalRenewalCost = (values[5] as? Double) ?: 0.0,
            totalHoldCost = (values[6] as? Double) ?: 0.0,
            totalRenewalSpent = (values[7] as? Double) ?: 0.0,
            totalEstimatedValue = totalEstimatedValue,
            totalPurchaseCost = totalPurchaseCost,
            cost7Days = cost7Days,
            cost30Days = cost30Days,
            cost90Days = cost90Days,
            cost365Days = cost365Days,
            allDomains = allDomains,
            registrars = (values[9] as List<*>).filterIsInstance<String>()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )
}
