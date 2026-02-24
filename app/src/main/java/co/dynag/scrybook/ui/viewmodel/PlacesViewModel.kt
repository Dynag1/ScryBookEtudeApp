package co.dynag.scrybook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.dynag.scrybook.data.model.Lieu
import co.dynag.scrybook.data.repository.ScryBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val repository: ScryBookRepository
) : ViewModel() {

    private val _places = MutableStateFlow<List<Lieu>>(emptyList())
    val places: StateFlow<List<Lieu>> = _places

    private val _selected = MutableStateFlow<Lieu?>(null)
    val selected: StateFlow<Lieu?> = _selected

    fun load(projectPath: String) {
        viewModelScope.launch {
            repository.openProject(projectPath)
            _places.value = repository.getLieux()
        }
    }

    fun select(l: Lieu?) { _selected.value = l }
    fun newPlace() { _selected.value = Lieu() }

    fun save(id: Long, nom: String, desc: String) {
        viewModelScope.launch {
            if (id == 0L) repository.insertLieu(nom, desc)
            else repository.updateLieu(id, nom, desc)
            _places.value = repository.getLieux()
            _selected.value = null
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteLieu(id)
            _places.value = repository.getLieux()
            _selected.value = null
        }
    }
}
