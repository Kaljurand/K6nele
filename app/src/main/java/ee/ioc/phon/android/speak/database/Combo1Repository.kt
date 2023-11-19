package ee.ioc.phon.android.speak.database

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import java.util.Locale

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class Combo1Repository(private val combo1Dao: Combo1Dao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allItems: Flow<List<Combo1>> = combo1Dao.getAllItems()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getAllItems0(): List<Combo1> {
        return combo1Dao.getAllItems0()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getEnabledItemsIme(): List<Combo1> {
        return combo1Dao.getEnabledItemsIme()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getKeyValuePairsForItem1(comboId: Int): List<KeyValuePair> {
        return combo1Dao.getKeyValuePairsForItem1(comboId)
    }

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(combo1: Combo1) {
        combo1Dao.insert(combo1)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateLocale(id: Int, locale: Locale) {
        combo1Dao.updateLocale(id, locale)
    }

    suspend fun delete(combo: Combo1) {
        combo1Dao.delete(combo)
    }
}