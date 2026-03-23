package io.unmi.app.data.repository

import io.unmi.app.data.local.db.dao.DomainDao
import io.unmi.app.data.local.db.dao.RenewalRecordDao
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.local.db.entity.RenewalRecordEntity
import io.unmi.app.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val domainDao: DomainDao,
    private val renewalRecordDao: RenewalRecordDao,
    private val securityManager: SecurityManager
) {
    private fun accountId(): Long = securityManager.getSessionAccountId() ?: 0L

    fun observeAllDomains(): Flow<List<DomainEntity>> = domainDao.observeAll(accountId())
    fun observeDomainCount(): Flow<Int> = domainDao.observeCount(accountId())
    fun observeTotalRenewalCost(): Flow<Double?> = domainDao.observeTotalRenewalCost(accountId())
    fun observeTotalHoldCost(): Flow<Double?> = domainDao.observeTotalHoldCost(accountId())
    fun observeAllRegistrars(): Flow<List<String>> = domainDao.observeAllRegistrars(accountId())
    fun observeAllTlds(): Flow<List<String>> = domainDao.observeAllTlds(accountId())

    fun observeExpiringCount(days: Int): Flow<Int> {
        val date = LocalDate.now().plusDays(days.toLong()).toString()
        return domainDao.observeExpiringCount(accountId(), date)
    }

    fun observeRenewalCostInDays(days: Int): Flow<Double?> {
        val date = LocalDate.now().plusDays(days.toLong()).toString()
        return domainDao.observeRenewalCostBefore(accountId(), date)
    }

    fun observeAllRenewalRecords(): Flow<List<RenewalRecordEntity>> =
        renewalRecordDao.observeAll()

    fun observeTotalRenewalSpent(): Flow<Double?> =
        renewalRecordDao.observeTotalRenewalSpent()

    fun observeRenewalRecordsByDomain(domainId: Long): Flow<List<RenewalRecordEntity>> =
        renewalRecordDao.observeByDomainId(domainId)

    suspend fun insertRenewalRecord(record: RenewalRecordEntity): Long =
        renewalRecordDao.insert(record)

    suspend fun deleteRenewalRecord(record: RenewalRecordEntity) =
        renewalRecordDao.delete(record)
}
