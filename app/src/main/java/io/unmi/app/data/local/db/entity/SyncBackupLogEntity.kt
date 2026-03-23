package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(tableName = "sync_backup_logs")
data class SyncBackupLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncType: String,
    val syncStatus: String,
    val versionNo: String? = null,
    val syncTime: String,
    val remoteProvider: String? = null,
    val fileHash: String? = null,
    val remark: String? = null
)
