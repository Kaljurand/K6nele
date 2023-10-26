package ee.ioc.phon.android.speak.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM key_value_pairs WHERE itemId = :itemId")
    fun getKeyValuePairsForItem(itemId: Int): Flow<List<KeyValuePair>>

    @Insert
    suspend fun insert(item: Item): Long

    // TODO: should take itemId as argument
    @Insert
    suspend fun insertKeyValuePair(keyValuePair: KeyValuePair)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemById(id: Int): Flow<Item?>

    @Update
    suspend fun update(item: Item)
}

@Database(entities = [Item::class, KeyValuePair::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
