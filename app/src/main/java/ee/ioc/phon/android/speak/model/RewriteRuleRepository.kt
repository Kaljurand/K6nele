package ee.ioc.phon.android.speak.model

import androidx.annotation.WorkerThread
import ee.ioc.phon.android.speak.Log
import ee.ioc.phon.android.speechutils.editor.Command
import kotlinx.coroutines.flow.Flow

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class RewriteRuleRepository(private val dao: RewriteRuleDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allRewriteRules: Flow<List<RewriteRule>> = dao.getRewriteRules()

    fun rulesByOwnerName(tableName: String): Flow<List<RewriteRule>> {
        return dao.getRewriteRulesByOwnerName(tableName)
    }

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun addNewRule(tableName: String, rewriteRule: RewriteRule) {
        var ownerId = dao.getId(tableName)
        if (ownerId == null) {
            ownerId = dao.insertTable(RewriteList(tableName, true))
            Log.i("Table (new)", ownerId.toString() + " " + tableName)
        } else {
            Log.i("Table (old)", ownerId.toString() + " " + tableName)
        }
        rewriteRule.ownerId = ownerId
        dao.insertAll(rewriteRule)
    }

    // Add Command to the rule database.
    // The table name is resolved to the table id to be used as the owner of the rule.
    // TODO: this should be a transaction (?)
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun addNewRule(tableName: String, command: Command) {
        var ownerId = dao.getId(tableName)
        if (ownerId == null) {
            ownerId = dao.insertTable(RewriteList(tableName, true))
            Log.i("Table (new)", ownerId.toString() + " " + tableName)
        } else {
            Log.i("Table (old)", ownerId.toString() + " " + tableName)
        }
        dao.insertAll(RewriteRule.fromCommand(ownerId, command))
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun delete(rewriteRule: RewriteRule) {
        dao.delete(rewriteRule)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun incFreq(rewriteRule: RewriteRule) {
        dao.incFreq(rewriteRule.id)
    }
}