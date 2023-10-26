package ee.ioc.phon.android.speak.database

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.google.android.datatransport.runtime.dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()
    }

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao {
        return database.itemDao()
    }
}

/*
@HiltViewModel
class ItemViewModel @Inject constructor(private val dao: ItemDao) : ViewModel() {
    // ... rest of the code
}
*/