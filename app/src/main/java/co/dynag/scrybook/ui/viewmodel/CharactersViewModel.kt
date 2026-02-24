package co.dynag.scrybook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.dynag.scrybook.data.model.Personnage
import co.dynag.scrybook.data.repository.ScryBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val repository: ScryBookRepository
) : ViewModel() {

    private val _characters = MutableStateFlow<List<Personnage>>(emptyList())
    val characters: StateFlow<List<Personnage>> = _characters

    private val _selected = MutableStateFlow<Personnage?>(null)
    val selected: StateFlow<Personnage?> = _selected

    fun load(projectPath: String) {
        viewModelScope.launch {
            repository.openProject(projectPath)
            _characters.value = repository.getPersonnages()
        }
    }

    fun select(p: Personnage?) { _selected.value = p }

    fun save(p: Personnage) {
        viewModelScope.launch {
            if (p.id == 0L) repository.insertPersonnage(p)
            else repository.updatePersonnage(p)
            _characters.value = repository.getPersonnages()
            _selected.value = null
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deletePersonnage(id)
            _characters.value = repository.getPersonnages()
            _selected.value = null
        }
    }

    fun newCharacter() { _selected.value = Personnage() }
}
