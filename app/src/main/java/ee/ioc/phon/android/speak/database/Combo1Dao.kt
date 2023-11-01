package ee.ioc.phon.android.speak.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface Combo1Dao {
    @Query("SELECT * FROM combos")
    fun getAllItems(): Flow<List<Combo1>>

    @Query("SELECT * FROM key_value_pairs WHERE comboId = :comboId")
    fun getKeyValuePairsForItem(comboId: Int): Flow<List<KeyValuePair>>

    @Insert
    suspend fun insert(combo1: Combo1): Long

    // TODO: should take itemId as argument
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeyValuePair(keyValuePair: KeyValuePair)

    @Delete
    suspend fun delete(combo1: Combo1)

    @Query("DELETE FROM combos")
    suspend fun deleteAll()

    @Query("SELECT * FROM combos WHERE id = :id")
    fun getItemById(id: Int): Flow<Combo1?>

    @Update
    suspend fun update(combo1: Combo1)
}