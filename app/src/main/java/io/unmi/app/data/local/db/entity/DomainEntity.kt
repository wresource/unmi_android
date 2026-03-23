package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(
    tableName = "domains",
    indices = [
        Index(value = ["domainName", "accountId"], unique = true),
        Index(value = ["accountId"]),
        Index(value = ["expireDate"]),
        Index(value = ["registrar"]),
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long = 0,
    val domainName: String,
    val tld: String,
    val registrar: String? = null,
    val registerDate: String? = null,
    val expireDate: String,
    val autoRenew: Boolean = false,
    val status: String = "active",
    val purchasePrice: Double = 0.0,
    val renewPrice: Double = 0.0,
    val holdCost: Double = 0.0,
    val currency: String = "CNY",
    val dnsProvider: String? = null,
    val privacyProtection: Boolean = false,
    val usageType: String? = null,
    val note: String? = null,
    val encryptedPayload: String? = null,
    val estimatedValue: Double = 0.0,
    val valueCurrency: String = "USD",
    val valueGrade: String? = null,
    val valueConfidence: String? = null,
    val nameservers: String? = null,
    val createdAt: String,
    val updatedAt: String
)
