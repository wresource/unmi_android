package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(
    tableName = "renewal_records",
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["domainId"]),
        Index(value = ["renewalDate"])
    ]
)
data class RenewalRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domainId: Long,
    val renewalDate: String,
    val renewalYears: Int = 1,
    val renewalPrice: Double,
    val currency: String = "CNY",
    val note: String? = null,
    val createdAt: String
)
