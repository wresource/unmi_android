package io.unmi.app.data.local.db.dao

import androidx.room.*
import io.unmi.app.data.local.db.entity.DomainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainDao {

    // All queries filter by accountId for multi-account isolation

    @Query("SELECT * FROM domains WHERE accountId = :accountId ORDER BY updatedAt DESC")
    fun observeAll(accountId: Long): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DomainEntity?

    @Query("SELECT * FROM domains WHERE accountId = :accountId AND domainName LIKE '%' || :keyword || '%' ORDER BY updatedAt DESC")
    fun searchByName(accountId: Long, keyword: String): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE accountId = :accountId AND status = :status ORDER BY expireDate ASC")
    fun observeByStatus(accountId: Long, status: String): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE accountId = :accountId AND registrar = :registrar ORDER BY expireDate ASC")
    fun observeByRegistrar(accountId: Long, registrar: String): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE accountId = :accountId AND expireDate <= :date AND status = 'active' ORDER BY expireDate ASC")
    fun observeExpiringSoon(accountId: Long, date: String): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE accountId = :accountId AND expireDate < :today AND status = 'active' ORDER BY expireDate ASC")
    fun observeExpired(accountId: Long, today: String): Flow<List<DomainEntity>>

    @Query("SELECT COUNT(*) FROM domains WHERE accountId = :accountId")
    fun observeCount(accountId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM domains WHERE accountId = :accountId AND expireDate <= :date AND status = 'active'")
    fun observeExpiringCount(accountId: Long, date: String): Flow<Int>

    @Query("SELECT SUM(renewPrice) FROM domains WHERE accountId = :accountId AND expireDate <= :date AND status = 'active'")
    fun observeRenewalCostBefore(accountId: Long, date: String): Flow<Double?>

    @Query("SELECT SUM(renewPrice) FROM domains WHERE accountId = :accountId AND status = 'active'")
    fun observeTotalRenewalCost(accountId: Long): Flow<Double?>

    @Query("SELECT SUM(holdCost) FROM domains WHERE accountId = :accountId")
    fun observeTotalHoldCost(accountId: Long): Flow<Double?>

    @Query("SELECT DISTINCT registrar FROM domains WHERE accountId = :accountId AND registrar IS NOT NULL ORDER BY registrar")
    fun observeAllRegistrars(accountId: Long): Flow<List<String>>

    @Query("SELECT DISTINCT tld FROM domains WHERE accountId = :accountId ORDER BY tld")
    fun observeAllTlds(accountId: Long): Flow<List<String>>

    @Query("SELECT SUM(estimatedValue) FROM domains WHERE accountId = :accountId")
    fun observeTotalEstimatedValue(accountId: Long): Flow<Double?>

    @Query("SELECT SUM(purchasePrice) FROM domains WHERE accountId = :accountId")
    fun observeTotalPurchaseCost(accountId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DomainEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: DomainEntity): Long

    @Update
    suspend fun update(entity: DomainEntity)

    @Delete
    suspend fun delete(entity: DomainEntity)

    @Query("DELETE FROM domains WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM domains WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)
}
