package io.unmi.app.data.local.db.entity

import androidx.room.*

@Entity(
    tableName = "domain_tag_cross_ref",
    primaryKeys = ["domainId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("domainId"), Index("tagId")]
)
data class DomainTagCrossRef(
    val domainId: Long,
    val tagId: Long
)
