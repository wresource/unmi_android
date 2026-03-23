package io.unmi.app.data.repository

import io.unmi.app.data.local.db.dao.DomainDao
import io.unmi.app.data.local.db.dao.TagDao
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.local.db.entity.DomainTagCrossRef
import io.unmi.app.data.local.db.entity.TagEntity
import io.unmi.app.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainRepository @Inject constructor(
    private val domainDao: DomainDao,
    private val tagDao: TagDao,
    private val securityManager: SecurityManager
) {
    private fun accountId(): Long = securityManager.getSessionAccountId() ?: 0L

    fun observeAll(): Flow<List<DomainEntity>> = domainDao.observeAll(accountId())

    fun searchByName(keyword: String): Flow<List<DomainEntity>> =
        domainDao.searchByName(accountId(), keyword)

    fun observeByStatus(status: String): Flow<List<DomainEntity>> =
        domainDao.observeByStatus(accountId(), status)

    fun observeByRegistrar(registrar: String): Flow<List<DomainEntity>> =
        domainDao.observeByRegistrar(accountId(), registrar)

    fun observeExpiringSoon(daysFromNow: Int): Flow<List<DomainEntity>> {
        val date = LocalDate.now().plusDays(daysFromNow.toLong()).toString()
        return domainDao.observeExpiringSoon(accountId(), date)
    }

    fun observeExpired(): Flow<List<DomainEntity>> =
        domainDao.observeExpired(accountId(), LocalDate.now().toString())

    fun observeCount(): Flow<Int> = domainDao.observeCount(accountId())

    fun observeExpiringCount(daysFromNow: Int): Flow<Int> {
        val date = LocalDate.now().plusDays(daysFromNow.toLong()).toString()
        return domainDao.observeExpiringCount(accountId(), date)
    }

    fun observeRenewalCostBefore(daysFromNow: Int): Flow<Double?> {
        val date = LocalDate.now().plusDays(daysFromNow.toLong()).toString()
        return domainDao.observeRenewalCostBefore(accountId(), date)
    }

    fun observeTotalRenewalCost(): Flow<Double?> = domainDao.observeTotalRenewalCost(accountId())
    fun observeTotalHoldCost(): Flow<Double?> = domainDao.observeTotalHoldCost(accountId())
    fun observeAllRegistrars(): Flow<List<String>> = domainDao.observeAllRegistrars(accountId())
    fun observeAllTlds(): Flow<List<String>> = domainDao.observeAllTlds(accountId())

    suspend fun getById(id: Long): DomainEntity? = domainDao.getById(id)

    suspend fun insert(entity: DomainEntity): Long {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        // Encrypt sensitive fields into encryptedPayload
        val encrypted = try {
            securityManager.encryptDomainData(
                entity.domainName, entity.registrar, entity.note, entity.nameservers
            )
        } catch (_: Exception) { null }

        return domainDao.insert(
            entity.copy(
                accountId = accountId(),
                encryptedPayload = encrypted,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun update(entity: DomainEntity) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val encrypted = try {
            securityManager.encryptDomainData(
                entity.domainName, entity.registrar, entity.note, entity.nameservers
            )
        } catch (_: Exception) { null }

        domainDao.update(
            entity.copy(
                accountId = accountId(),
                encryptedPayload = encrypted,
                updatedAt = now
            )
        )
    }

    suspend fun delete(entity: DomainEntity) = domainDao.delete(entity)
    suspend fun deleteByIds(ids: List<Long>) = domainDao.deleteByIds(ids)

    fun observeTagsForDomain(domainId: Long): Flow<List<TagEntity>> =
        tagDao.observeTagsForDomain(domainId)

    suspend fun setTagsForDomain(domainId: Long, tagIds: List<Long>) {
        tagDao.deleteCrossRefsForDomain(domainId)
        tagIds.forEach { tagId ->
            tagDao.insertCrossRef(DomainTagCrossRef(domainId, tagId))
        }
    }

    suspend fun insertOrReplace(entity: DomainEntity): Long {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return domainDao.insertOrReplace(entity.copy(accountId = accountId(), createdAt = now, updatedAt = now))
    }
}
