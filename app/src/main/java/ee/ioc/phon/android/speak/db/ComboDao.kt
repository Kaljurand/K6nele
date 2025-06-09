package ee.ioc.phon.android.speak.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ComboDao {
    @Query("SELECT * FROM combos ORDER BY position")
    fun getAll(): Flow<List<ComboEntity>>

    @Insert
    suspend fun insert(combo: ComboEntity)

    @Update
    suspend fun update(combo: ComboEntity)

    @Delete
    suspend fun delete(combo: ComboEntity)
}
