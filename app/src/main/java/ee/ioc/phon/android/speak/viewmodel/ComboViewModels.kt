package ee.ioc.phon.android.speak.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.ioc.phon.android.speak.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServicePickerViewModel(private val repository: ComboRepository) : ViewModel() {
    private val _services = MutableStateFlow<List<ServiceEntity>>(emptyList())
    val services: StateFlow<List<ServiceEntity>> = _services

    fun loadServices() {
        viewModelScope.launch {
            _services.value = repository.getAllServices()
        }
    }
}

class ComboListViewModel(private val repository: ComboRepository, private val listType: String) : ViewModel() {
    private val _combos = MutableStateFlow<List<ComboEntity>>(emptyList())
    val combos: StateFlow<List<ComboEntity>> = _combos

    fun loadCombos() {
        viewModelScope.launch {
            _combos.value = repository.getCombosByListType(listType)
        }
    }

    fun addCombo(combo: ComboEntity) {
        viewModelScope.launch {
            repository.insertCombo(combo)
            loadCombos()
        }
    }

    fun removeCombo(combo: ComboEntity) {
        viewModelScope.launch {
            repository.deleteCombo(combo)
            loadCombos()
        }
    }

    fun updateCombo(combo: ComboEntity) {
        viewModelScope.launch {
            repository.updateCombo(combo)
            loadCombos()
        }
    }
}

class ComboDetailsViewModel(private val repository: ComboRepository, private val comboId: Long) : ViewModel() {
    private val _combo = MutableStateFlow<ComboEntity?>(null)
    val combo: StateFlow<ComboEntity?> = _combo

    fun loadCombo() {
        viewModelScope.launch {
            _combo.value = repository.getComboById(comboId)
        }
    }

    fun updateCombo(combo: ComboEntity) {
        viewModelScope.launch {
            repository.updateCombo(combo)
            _combo.value = combo
        }
    }
}
