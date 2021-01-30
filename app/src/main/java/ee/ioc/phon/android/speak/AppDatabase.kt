package ee.ioc.phon.android.speak

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import ee.ioc.phon.android.speak.model.RewriteList
import ee.ioc.phon.android.speak.model.RewriteRule
import ee.ioc.phon.android.speak.model.RewriteRuleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

// consider setting a directory for Room to use to export the schema so you can check
// the current schema into your version control system
@Database(entities = arrayOf(RewriteList::class, RewriteRule::class), version = 6, exportSchema = false)
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

                    wordDao.insertAll(
                            RewriteRule(1, Pattern.compile("myapp"), Pattern.compile("1"), "One"),
                            RewriteRule(1, Pattern.compile("myapp2"), Pattern.compile("2"), "Two")
                    );

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
                        // TODO: temporary, until we provide migrations
                        .fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}