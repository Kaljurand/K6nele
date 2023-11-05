package ee.ioc.phon.android.speak.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import ee.ioc.phon.android.speak.database.Combo1
import ee.ioc.phon.android.speak.database.Combo1Repository
import kotlinx.coroutines.launch
import java.util.Locale

class Combo1ViewModel(private val repository: Combo1Repository) : ViewModel() {
    val allItems: LiveData<List<Combo1>> = repository.allItems.asLiveData()

    fun insert(combo1: Combo1) = viewModelScope.launch {
        repository.insert(combo1)
    }

    fun updateLocale(id: Int, locale: Locale) = viewModelScope.launch {
        repository.updateLocale(id, locale)
    }

    fun delete(combo: Combo1) = viewModelScope.launch {
        repository.delete(combo)
    }
}

class Combo1ViewModelFactory(private val repository: Combo1Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(Combo1ViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return Combo1ViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}