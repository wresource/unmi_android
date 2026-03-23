package io.unmi.app.data.local.db.dao

import androidx.room.*
import io.unmi.app.data.local.db.entity.RenewalRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RenewalRecordDao {

    @Query("SELECT * FROM renewal_records WHERE domainId = :domainId ORDER BY renewalDate DESC")
    fun observeByDomainId(domainId: Long): Flow<List<RenewalRecordEntity>>

    @Query("SELECT * FROM renewal_records ORDER BY renewalDate DESC")
    fun observeAll(): Flow<List<RenewalRecordEntity>>

    @Query("SELECT SUM(renewalPrice) FROM renewal_records")
    fun observeTotalRenewalSpent(): Flow<Double?>

    @Query("SELECT SUM(renewalPrice) FROM renewal_records WHERE renewalDate LIKE :yearMonth || '%'")
    fun observeMonthlySpent(yearMonth: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RenewalRecordEntity): Long

    @Delete
    suspend fun delete(entity: RenewalRecordEntity)
}
