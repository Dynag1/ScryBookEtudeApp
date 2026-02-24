package co.dynag.scrybook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.dynag.scrybook.data.model.Param
import co.dynag.scrybook.data.repository.ScryBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ScryBookRepository
) : ViewModel() {

    private val _param = MutableStateFlow(Param())
    val param: StateFlow<Param> = _param

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun load(projectPath: String) {
        viewModelScope.launch {
            repository.openProject(projectPath)
            _param.value = repository.getParam()
        }
    }

    fun update(param: Param) { _param.value = param }

    fun save() {
        viewModelScope.launch {
            repository.saveParam(_param.value)
            _saved.value = true
        }
    }

    fun resetSaved() { _saved.value = false }
}
