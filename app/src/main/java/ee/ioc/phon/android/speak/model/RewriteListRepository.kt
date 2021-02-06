package ee.ioc.phon.android.speak.model

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class RewriteListRepository(private val dao: RewriteRuleDao) {

    val all: Flow<List<RewriteList>> = dao.getRewriteLists()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(rewriteList: RewriteList) {
        dao.insert(rewriteList)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun delete(rewriteList: RewriteList) {
        dao.delete(rewriteList)
    }
}