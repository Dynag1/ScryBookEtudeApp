package co.dynag.scrybook.data.repository

import android.content.Context
import co.dynag.scrybook.data.db.ScryBookDatabase
import co.dynag.scrybook.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScryBookRepository(private val context: Context) {

    private var dbHelper: ScryBookDatabase? = null
    private var currentPath: String = ""

    fun openProject(path: String) {
        if (currentPath != path) {
            dbHelper?.close()
            dbHelper = ScryBookDatabase.open(context, path)
            currentPath = path
        }
    }

    fun closeProject() {
        dbHelper?.close()
        dbHelper = null
        currentPath = ""
    }

    fun createProject(path: String) {
        dbHelper?.close()
        dbHelper = ScryBookDatabase.open(context, path)
        currentPath = path
        dbHelper!!.writableDatabase // triggers onCreate
    }

    // ─── Chapitres ────────────────────────────────────────────────────────────

    suspend fun getChapitres(): List<Chapitre> = withContext(Dispatchers.IO) {
        dbHelper?.getChapitres() ?: emptyList()
    }

    suspend fun getChapitre(id: Long): Chapitre? = withContext(Dispatchers.IO) {
        dbHelper?.getChapitre(id)
    }

    suspend fun insertChapitre(nom: String, numero: String, resume: String): Long = withContext(Dispatchers.IO) {
        dbHelper?.insertChapitre(nom, numero, resume) ?: -1L
    }

    suspend fun updateChapitre(id: Long, nom: String, numero: String, resume: String) = withContext(Dispatchers.IO) {
        dbHelper?.updateChapitre(id, nom, numero, resume)
    }

    suspend fun saveChapitreContenu(id: Long, html: String) = withContext(Dispatchers.IO) {
        dbHelper?.saveChapitreContenu(id, html)
    }

    suspend fun deleteChapitre(id: Long) = withContext(Dispatchers.IO) {
        dbHelper?.deleteChapitre(id)
    }

    // ─── Personnages ──────────────────────────────────────────────────────────

    suspend fun getPersonnages(): List<Personnage> = withContext(Dispatchers.IO) {
        dbHelper?.getPersonnages() ?: emptyList()
    }

    suspend fun insertPersonnage(p: Personnage): Long = withContext(Dispatchers.IO) {
        dbHelper?.insertPersonnage(p) ?: -1L
    }

    suspend fun updatePersonnage(p: Personnage) = withContext(Dispatchers.IO) {
        dbHelper?.updatePersonnage(p)
    }

    suspend fun deletePersonnage(id: Long) = withContext(Dispatchers.IO) {
        dbHelper?.deletePersonnage(id)
    }

    // ─── Lieux ────────────────────────────────────────────────────────────────

    suspend fun getLieux(): List<Lieu> = withContext(Dispatchers.IO) {
        dbHelper?.getLieux() ?: emptyList()
    }

    suspend fun insertLieu(nom: String, desc: String): Long = withContext(Dispatchers.IO) {
        dbHelper?.insertLieu(nom, desc) ?: -1L
    }

    suspend fun updateLieu(id: Long, nom: String, desc: String) = withContext(Dispatchers.IO) {
        dbHelper?.updateLieu(id, nom, desc)
    }

    suspend fun deleteLieu(id: Long) = withContext(Dispatchers.IO) {
        dbHelper?.deleteLieu(id)
    }

    // ─── Info ─────────────────────────────────────────────────────────────────

    suspend fun getInfo(): Info = withContext(Dispatchers.IO) {
        dbHelper?.getInfo() ?: Info()
    }

    suspend fun saveInfo(info: Info) = withContext(Dispatchers.IO) {
        dbHelper?.saveInfo(info)
    }

    // ─── Param ────────────────────────────────────────────────────────────────

    suspend fun getParam(): Param = withContext(Dispatchers.IO) {
        dbHelper?.getParam() ?: Param()
    }

    suspend fun saveParam(param: Param) = withContext(Dispatchers.IO) {
        dbHelper?.saveParam(param)
    }
}
