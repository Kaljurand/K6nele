package ee.ioc.phon.android.speak

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.model.RewriteRuleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// consider setting a directory for Room to use to export the schema so you can check
// the current schema into your version control system
@Database(entities = arrayOf(RewriteRule::class), version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rewriteRuleDao(): RewriteRuleDao

    private class AppDatabaseCallback(
            private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    var wordDao = database.rewriteRuleDao()

                    // Delete all content here.
                    wordDao.deleteAll()

                    // Add sample words.
                    //var word = RewriteRule("Hello")
                    //wordDao.insertAll(word)
                    var word1 = RewriteRule("World!")
                    wordDao.insertAll(word1, RewriteRule("TODO!"))

                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
                context: Context,
                scope: CoroutineScope
        ): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                )
                        .addCallback(AppDatabaseCallback(scope))
                        .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}