package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(tableName = "whois_query_logs")
data class WhoisQueryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domainName: String,
    val queryStatus: String,
    val provider: String? = null,
    val rawText: String? = null,
    val parsedResultJson: String? = null,
    val queriedAt: String,
    val errorMessage: String? = null
)
