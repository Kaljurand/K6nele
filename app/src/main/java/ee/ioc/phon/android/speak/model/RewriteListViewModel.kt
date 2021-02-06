package ee.ioc.phon.android.speak.model

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class RewriteListViewModel(private val repository: RewriteListRepository) : ViewModel() {

    val allWords: LiveData<List<RewriteList>> = repository.all.asLiveData()

    fun addEmpty(item: RewriteList) = viewModelScope.launch {
        repository.insert(item)
    }

    fun delete(item: RewriteList) = viewModelScope.launch {
        repository.delete(item)
    }
}

class RewriteListViewModelFactory(private val repository: RewriteListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewriteListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RewriteListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}