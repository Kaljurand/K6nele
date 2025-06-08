package ee.ioc.phon.android.speak.model

import kotlinx.coroutines.flow.Flow

class ComboRepository(
    private val serviceDao: ServiceDao,
    private val comboDao: ComboDao,
    private val comboListDao: ComboListDao
) {
    // Service operations
    suspend fun getAllServices(): List<ServiceEntity> = serviceDao.getAll()
    suspend fun getServiceById(id: String): ServiceEntity? = serviceDao.getById(id)
    suspend fun insertService(service: ServiceEntity) = serviceDao.insert(service)
    suspend fun insertServices(services: List<ServiceEntity>) = serviceDao.insertAll(services)
    suspend fun deleteService(service: ServiceEntity) = serviceDao.delete(service)

    // Combo operations
    suspend fun getCombosByListType(type: String): List<ComboEntity> = comboDao.getByListType(type)
    suspend fun getComboById(comboId: Long): ComboEntity? = comboDao.getById(comboId)
    suspend fun insertCombo(combo: ComboEntity): Long = comboDao.insert(combo)
    suspend fun insertCombos(combos: List<ComboEntity>) = comboDao.insertAll(combos)
    suspend fun updateCombo(combo: ComboEntity) = comboDao.update(combo)
    suspend fun deleteCombo(combo: ComboEntity) = comboDao.delete(combo)

    // ComboList operations
    suspend fun getComboListByType(type: String): ComboListEntity? = comboListDao.getByType(type)
    suspend fun insertComboList(comboList: ComboListEntity) = comboListDao.insert(comboList)
    suspend fun updateComboList(comboList: ComboListEntity) = comboListDao.update(comboList)
}
