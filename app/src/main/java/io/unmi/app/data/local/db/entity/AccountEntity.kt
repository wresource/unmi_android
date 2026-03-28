package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["passwordHash"], unique = false)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String? = null,
    val passwordHash: String,
    val passwordSalt: String,
    val isGuest: Boolean = false,
    val createdAt: String,
    val lastLoginAt: String? = null
)
