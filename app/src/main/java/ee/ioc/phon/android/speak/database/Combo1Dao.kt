package ee.ioc.phon.android.speak.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Dao
interface Combo1Dao {
    @Query("SELECT * FROM combos")
    fun getAllItems(): Flow<List<Combo1>>

    @Query("SELECT * FROM combos")
    suspend fun getAllItems0(): List<Combo1>

    @Query("SELECT * FROM combos WHERE isEnabledIme = 1")
    suspend fun getEnabledItemsIme(): List<Combo1>

    @Query("SELECT * FROM key_value_pairs WHERE comboId = :comboId")
    fun getKeyValuePairsForItem(comboId: Int): Flow<List<KeyValuePair>>

    @Query("SELECT * FROM key_value_pairs WHERE comboId = :comboId")
    suspend fun getKeyValuePairsForItem1(comboId: Int): List<KeyValuePair>

    @Insert
    suspend fun insert(combo: Combo1): Long

    // TODO: should take itemId as argument
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKeyValuePair(keyValuePair: KeyValuePair)

    @Delete
    suspend fun delete(combo: Combo1)

    @Query("DELETE FROM combos")
    suspend fun deleteAll()

    @Query("SELECT * FROM combos WHERE id = :id")
    fun getItemById(id: Int): Flow<Combo1?>

    @Update
    suspend fun update(combo: Combo1)

    @Query("UPDATE combos SET locale = :locale WHERE id = :id")
    suspend fun updateLocale(id: Int, locale: Locale)
    //suspend fun updateLocale(combo: Combo1, locale: Locale)
}