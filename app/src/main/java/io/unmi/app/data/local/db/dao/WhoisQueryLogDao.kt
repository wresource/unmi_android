package io.unmi.app.data.local.db.dao

import androidx.room.*
import io.unmi.app.data.local.db.entity.WhoisQueryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhoisQueryLogDao {

    @Query("SELECT * FROM whois_query_logs WHERE domainName = :domainName ORDER BY queriedAt DESC LIMIT 1")
    suspend fun getLatestByDomain(domainName: String): WhoisQueryLogEntity?

    @Query("SELECT * FROM whois_query_logs ORDER BY queriedAt DESC")
    fun observeAll(): Flow<List<WhoisQueryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WhoisQueryLogEntity): Long

    @Query("DELETE FROM whois_query_logs WHERE domainName = :domainName")
    suspend fun deleteByDomain(domainName: String)
}
