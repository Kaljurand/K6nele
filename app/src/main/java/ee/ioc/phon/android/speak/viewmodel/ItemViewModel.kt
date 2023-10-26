package ee.ioc.phon.android.speak.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import ee.ioc.phon.android.speak.database.Item
import ee.ioc.phon.android.speak.database.ItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ItemViewModel(private val dao: ItemDao) : ViewModel() {
    val items = dao.getAllItems().asLiveData()

    fun addItem(item: Item) = viewModelScope.launch {
        dao.insert(item)
    }

    fun getItemById(id: Int): Flow<Item?> {
        return dao.getItemById(id)
    }

    fun updateItem(item: Item) = viewModelScope.launch {
        dao.update(item)
    }

}