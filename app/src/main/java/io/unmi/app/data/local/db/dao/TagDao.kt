package io.unmi.app.data.local.db.dao

import androidx.room.*
import io.unmi.app.data.local.db.entity.TagEntity
import io.unmi.app.data.local.db.entity.DomainTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN domain_tag_cross_ref ref ON t.id = ref.tagId
        WHERE ref.domainId = :domainId
    """)
    fun observeTagsForDomain(domainId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: DomainTagCrossRef)

    @Query("DELETE FROM domain_tag_cross_ref WHERE domainId = :domainId")
    suspend fun deleteCrossRefsForDomain(domainId: Long)

    @Delete
    suspend fun delete(entity: TagEntity)
}
