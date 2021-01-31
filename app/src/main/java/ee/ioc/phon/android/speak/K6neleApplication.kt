package ee.ioc.phon.android.speak

import android.app.Application
import ee.ioc.phon.android.speak.model.RewriteListRepository
import ee.ioc.phon.android.speak.model.RewriteRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class K6neleApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { RewriteRuleRepository(database.rewriteRuleDao()) }

    // TODO: do we need a separate repository here?
    val repositoryForList by lazy { RewriteListRepository(database.rewriteRuleDao()) }
}