package ee.ioc.phon.android.speak.model

import androidx.room.*

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services")
    suspend fun getAll(): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getById(id: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ServiceEntity>)

    @Delete
    suspend fun delete(service: ServiceEntity)
}

@Dao
interface ComboDao {
    @Query("SELECT * FROM combos WHERE comboListType = :type ORDER BY comboId")
    suspend fun getByListType(type: String): List<ComboEntity>

    @Query("SELECT * FROM combos WHERE comboId = :comboId")
    suspend fun getById(comboId: Long): ComboEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(combo: ComboEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(combos: List<ComboEntity>)

    @Update
    suspend fun update(combo: ComboEntity)

    @Delete
    suspend fun delete(combo: ComboEntity)
}

@Dao
interface ComboListDao {
    @Query("SELECT * FROM combo_lists WHERE type = :type")
    suspend fun getByType(type: String): ComboListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comboList: ComboListEntity)

    @Update
    suspend fun update(comboList: ComboListEntity)
}
