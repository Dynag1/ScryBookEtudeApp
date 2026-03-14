package co.dynag.scrybook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.dynag.scrybook.data.model.Site
import co.dynag.scrybook.data.repository.ScryBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SiteViewModel @Inject constructor(
    private val repository: ScryBookRepository
) : ViewModel() {

    private val _sites = MutableStateFlow<List<Site>>(emptyList())
    val sites: StateFlow<List<Site>> = _sites

    private val _selected = MutableStateFlow<Site?>(null)
    val selected: StateFlow<Site?> = _selected

    fun load(projectPath: String) {
        repository.openProject(projectPath)
        viewModelScope.launch {
            _sites.value = repository.getSites()
        }
    }

    fun select(site: Site?) {
        _selected.value = site
    }

    fun newSite() {
        _selected.value = Site(id = 0, nom = "", contenu = "")
    }

    fun save(site: Site) {
        viewModelScope.launch {
            if (site.id == 0L) {
                repository.insertSite(site.nom, site.contenu)
            } else {
                repository.updateSite(site.id, site.nom, site.contenu)
            }
            _sites.value = repository.getSites()
            _selected.value = null
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteSite(id)
            _sites.value = repository.getSites()
        }
    }
}
