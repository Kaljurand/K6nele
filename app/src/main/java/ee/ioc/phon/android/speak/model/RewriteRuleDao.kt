package ee.ioc.phon.android.speak.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RewriteRuleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg rewriteList: RewriteList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg rewriteRules: RewriteRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rewriteRules: List<RewriteRule>)

    @Delete
    suspend fun delete(rewriteRule: RewriteRule)

    @Delete
    suspend fun delete(rewriteRule: RewriteList)

    @Query("DELETE FROM rewrite_rules")
    suspend fun deleteAll()

    @Query("SELECT * FROM rewrite_rules")
    fun getRewriteRules(): Flow<List<RewriteRule>>

    @Query("SELECT * FROM rewrite_rules WHERE ownerId = :ownerId")
    fun getRewriteRulesByOwner(ownerId: Int): Flow<List<RewriteRule>>

    //@Transaction
    //@Query("SELECT * FROM rewrite_list WHERE name = :tableName")
    //fun getRewriteRulesByOwnerName(tableName: String): Flow<List<RewriteListWithRules>>

    // TODO: seems to work but is maybe not idiomatic
    @Transaction
    @Query("SELECT * FROM rewrite_rules, rewrite_list WHERE (:tableName = '' or rewrite_list.name = :tableName and rewrite_list.rewriteListId = rewrite_rules.ownerId)")
    fun getRewriteRulesByOwnerName(tableName: String): Flow<List<RewriteRule>>

    @Transaction
    @Query("SELECT * FROM rewrite_list")
    fun getRewriteListsWithRules(): Flow<List<RewriteListWithRules>>

    @Query("SELECT * FROM rewrite_list ORDER BY name")
    fun getRewriteLists(): Flow<List<RewriteList>>

    @Query("UPDATE rewrite_rules SET ownerId = ownerId + 1 WHERE id = :id")
    suspend fun incFreq(id: Int)

    @Query("UPDATE rewrite_list SET name = :name WHERE rewriteListId = :rewriteListId")
    suspend fun rename(rewriteListId: Int, name: String)

    @Query("SELECT name FROM rewrite_list WHERE rewriteListId = :rewriteListId")
    suspend fun getName(rewriteListId: Long): String

    // TODO: if name does not exist then create it (transaction?)
    @Query("SELECT rewriteListId FROM rewrite_list WHERE name = :name")
    suspend fun getId(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RewriteList): Long
}