package ee.ioc.phon.android.speak

import android.app.Application
import ee.ioc.phon.android.speak.database.Combo1Database
import ee.ioc.phon.android.speak.database.Combo1Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class K6neleApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { Combo1Database.getDatabase(this, applicationScope) }
    val repository by lazy { Combo1Repository(database.combo1Dao()) }
}