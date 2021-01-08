package ee.ioc.phon.android.speak.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RewriteRuleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vararg rewriteRules: RewriteRule)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rewriteRules: List<RewriteRule>)

    @Delete
    suspend fun delete(user: RewriteRule)

    @Query("DELETE FROM rewrite_rules")
    suspend fun deleteAll()

    @Query("SELECT * FROM rewrite_rules")
    fun getRewriteRules(): Flow<List<RewriteRule>>

    //@Query("UPDATE rewrite_rules SET freq = freq + 1 WHERE userId = :userId")
    //suspend fun incrementFreq(userId: String)
}