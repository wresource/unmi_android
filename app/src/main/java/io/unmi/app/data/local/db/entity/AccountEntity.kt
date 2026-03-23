package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["passwordHash"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String? = null,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: String,
    val lastLoginAt: String? = null
)
