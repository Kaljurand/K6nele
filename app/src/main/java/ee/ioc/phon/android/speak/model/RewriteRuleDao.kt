package ee.ioc.phon.android.speak.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RewriteRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg rewriteRules: RewriteRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rewriteRules: List<RewriteRule>)

    @Delete
    suspend fun delete(rewriteRule: RewriteRule)

    @Query("DELETE FROM rewrite_rules")
    suspend fun deleteAll()

    @Query("SELECT * FROM rewrite_rules")
    fun getRewriteRules(): Flow<List<RewriteRule>>

    @Transaction
    @Query("SELECT * FROM rewrite_list")
    fun getRewriteListsWithRules(): Flow<List<RewriteListWithRules>>

    @Query("UPDATE rewrite_rules SET ownerId = ownerId + 1 WHERE id = :id")
    suspend fun incFreq(id: Int)
}