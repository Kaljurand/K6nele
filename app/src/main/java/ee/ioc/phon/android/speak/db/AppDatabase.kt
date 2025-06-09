package ee.ioc.phon.android.speak.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ComboEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun comboDao(): ComboDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "combos.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
