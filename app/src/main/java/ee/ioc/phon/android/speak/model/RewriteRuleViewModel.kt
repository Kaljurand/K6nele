package ee.ioc.phon.android.speak.model

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class RewriteRuleViewModel(private val repository: RewriteRuleRepository) : ViewModel() {

    // Using LiveData and caching what allWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allWords: LiveData<List<RewriteRule>> = repository.allRewriteRules.asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun addNewRule(tableName: String, rewriteRule: RewriteRule) = viewModelScope.launch {
        repository.addNewRule(tableName, rewriteRule)
    }

    fun delete(word: RewriteRule) = viewModelScope.launch {
        repository.delete(word)
    }

    fun incFreq(word: RewriteRule) = viewModelScope.launch {
        repository.incFreq(word)
    }
}

class RewriteRuleViewModelFactory(private val repository: RewriteRuleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewriteRuleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RewriteRuleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}