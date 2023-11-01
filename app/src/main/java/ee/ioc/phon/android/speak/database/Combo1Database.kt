package ee.ioc.phon.android.speak.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// TODO: Set a directory for Room to use to export the schema and version control it.
@Database(entities = arrayOf(Combo1::class, KeyValuePair::class), version = 1, exportSchema = false)
public abstract class Combo1Database : RoomDatabase() {

    abstract fun combo1Dao(): Combo1Dao

    private class Combo1DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.combo1Dao())
                }
            }
        }

        suspend fun populateDatabase(combo1Dao: Combo1Dao) {
            // Delete all content here.
            combo1Dao.deleteAll()

            // TODO: populate based on the installed services, and locales that are active
            // TODO: prefer "Kõnele service" to "Kõnele (fast recognition)" if former is installed.
            combo1Dao.insert(
                Combo1(
                    shortLabel = "et-EE",
                    longLabel = "Estonian (Estonia) * Kõnele (fast recognition)",
                    componentName = "ee.ioc.phon.android.speak/.service.WebSocketRecognitionService"
                )
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: Combo1Database? = null

        fun getDatabase(context: Context, scope: CoroutineScope): Combo1Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, Combo1Database::class.java, "combo_database"
                ).addCallback(Combo1DatabaseCallback(scope)).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}