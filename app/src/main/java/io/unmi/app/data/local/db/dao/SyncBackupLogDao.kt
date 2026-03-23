package io.unmi.app.data.local.db.dao

import androidx.room.*
import io.unmi.app.data.local.db.entity.SyncBackupLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncBackupLogDao {

    @Query("SELECT * FROM sync_backup_logs ORDER BY syncTime DESC")
    fun observeAll(): Flow<List<SyncBackupLogEntity>>

    @Query("SELECT * FROM sync_backup_logs ORDER BY syncTime DESC LIMIT 1")
    suspend fun getLatest(): SyncBackupLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncBackupLogEntity): Long
}
