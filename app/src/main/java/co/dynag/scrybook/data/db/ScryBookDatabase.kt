package co.dynag.scrybook.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import co.dynag.scrybook.data.model.*

/**
 * ScryBookDatabase — raw SQLite helper.
 * Keeps 100% compatibility with the .sb file format from the desktop app.
 * The .sb file IS the SQLite database file.
 */
class ScryBookDatabase(context: Context, dbPath: String) :
    SQLiteOpenHelper(context, dbPath, null, DB_VERSION) {

    companion object {
        const val DB_VERSION = 4

        // Table names
        const val TABLE_CHAPITRE = "chapitre"
        const val TABLE_PERSO = "perso"
        const val TABLE_LIEUX = "lieux"
        const val TABLE_INFO = "info"
        const val TABLE_PARAM = "param"
        const val TABLE_SITE = "site"

        // Défaut chapitres créés à la nouvelle création d'un projet
        val DEFAULT_CHAPTERS = listOf(
            Triple("Présentation", "1", ""),
            Triple("Introduction", "2", ""),
            Triple("Demande Initiale", "3", ""),
            Triple("Audit", "4", ""),
            Triple("Préconisations", "5", ""),
            Triple("Conclusion", "6", "")
        )

        fun open(context: Context, dbPath: String): ScryBookDatabase {
            return ScryBookDatabase(context, dbPath)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            // Disable WAL mode to ensure all data is in the main .sb file for sync-back
            db.disableWriteAheadLogging()
            if (!db.isReadOnly) {
                db.execSQL("PRAGMA journal_mode=DELETE")
                db.execSQL("PRAGMA synchronous=FULL")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            try { db.execSQL("PRAGMA journal_mode=DELETE") } catch (e: Exception) {}
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$TABLE_CHAPITRE'", null)
        val hasChapitre = cursor.moveToFirst()
        cursor.close()

        if (hasChapitre) {
            // Desktop file without 'android_metadata' table, do not recreate or insert default chapters
            return
        }

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CHAPITRE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nom TEXT NOT NULL,
                numero TEXT NOT NULL,
                resume TEXT NOT NULL,
                contenu_html TEXT DEFAULT ''
            )""")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PERSO (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                alias TEXT, nom TEXT, prenom TEXT, sexe TEXT,
                age INTEGER, desc_phys TEXT, desc_global TEXT, skill TEXT
            )""")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_LIEUX (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nom TEXT, desc TEXT
            )""")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_INFO (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titre TEXT, stitre TEXT, auteur TEXT, date TEXT, resume TEXT
            )""")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PARAM (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                police TEXT, taille TEXT, save_time TEXT, langue TEXT, theme TEXT, lic TEXT, format_page TEXT
            )""")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SITE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nom TEXT NOT NULL DEFAULT '',
                contenu TEXT NOT NULL DEFAULT ''
            )""")

        // Insert default rows
        db.execSQL("INSERT OR IGNORE INTO $TABLE_INFO (id, titre, auteur, date, resume) VALUES (1,'','','','')")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_PARAM (id, police, taille, save_time, langue, theme) VALUES (1,'serif','16','30','fr','dark')")

        // Créer les chapitres (modèle ou défaut)
        val prefs = context.getSharedPreferences("scrybook_recent", android.content.Context.MODE_PRIVATE)
        val templateJson = prefs.getString("chapitres_auto", null)

        if (!templateJson.isNullOrBlank()) {
            try {
                val array = org.json.JSONArray(templateJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val nom = obj.optString("nom", "")
                    val numero = obj.optString("numero", "")
                    val resume = obj.optString("resume", "")
                    val contenuHtml = obj.optString("contenu_html", "")
                    db.execSQL("INSERT INTO $TABLE_CHAPITRE (nom, numero, resume, contenu_html) VALUES (?, ?, ?, ?)", arrayOf(nom, numero, resume, contenuHtml))
                }
                return // Modèle correctement chargé, on sort !
            } catch (e: Exception) { e.printStackTrace() }
        }

        DEFAULT_CHAPTERS.forEach { (nom, numero, resume) ->
            db.execSQL("INSERT INTO $TABLE_CHAPITRE (nom, numero, resume, contenu_html) VALUES ('$nom','$numero','$resume','')")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PARAM ADD COLUMN format TEXT DEFAULT 'A4'")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_SITE (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nom TEXT NOT NULL DEFAULT '',
                        contenu TEXT DEFAULT ''
                    )""")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE $TABLE_INFO ADD COLUMN stitre TEXT")
            } catch (e: Exception) { e.printStackTrace() }
            try {
                db.execSQL("ALTER TABLE $TABLE_CHAPITRE ADD COLUMN contenu_html TEXT DEFAULT ''")
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // ─── Chapitres ──────────────────────────────────────────────────────────

    fun getChapitres(): List<Chapitre> {
        val list = mutableListOf<Chapitre>()
        val db = readableDatabase
        
        // Exclude full text content in list retrieval to avoid CursorWindow 2MB limitation crashes when containing inline images
        val cursor = db.rawQuery("SELECT id, nom, numero, resume FROM $TABLE_CHAPITRE ORDER BY CAST(numero AS INTEGER)", null)
        val idIdx = cursor.columnNames.indexOfFirst { it.equals("id", true) }
        val nomIdx = cursor.columnNames.indexOfFirst { it.equals("nom", true) }
        val numIdx = cursor.columnNames.indexOfFirst { it.equals("numero", true) }
        val resIdx = cursor.columnNames.indexOfFirst { it.equals("resume", true) }

        while (cursor.moveToNext()) {
            list.add(Chapitre(
                id = if (idIdx != -1) cursor.getLong(idIdx) else 0L,
                nom = if (nomIdx != -1) cursor.getString(nomIdx) ?: "" else "",
                numero = if (numIdx != -1) cursor.getString(numIdx) ?: "" else "",
                resume = if (resIdx != -1) cursor.getString(resIdx) ?: "" else "",
                contenuHtml = "" // Loaded per chapter in getChapitre()
            ))
        }
        cursor.close()
        return list
    }

    fun getChapitre(id: Long): Chapitre? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CHAPITRE WHERE id=?", arrayOf(id.toString()))
        return if (cursor.moveToFirst()) {
            val idIdx = cursor.columnNames.indexOfFirst { it.equals("id", true) }
            val nomIdx = cursor.columnNames.indexOfFirst { it.equals("nom", true) }
            val numIdx = cursor.columnNames.indexOfFirst { it.equals("numero", true) }
            val resIdx = cursor.columnNames.indexOfFirst { it.equals("resume", true) }
            val htmlIdx = cursor.columnNames.indexOfFirst { it.equals("contenu_html", true) }

            val finalHtml = if (htmlIdx != -1) cursor.getString(htmlIdx) ?: "" else ""

            Chapitre(
                id = if (idIdx != -1) cursor.getLong(idIdx) else 0L,
                nom = if (nomIdx != -1) cursor.getString(nomIdx) ?: "" else "",
                numero = if (numIdx != -1) cursor.getString(numIdx) ?: "" else "",
                resume = if (resIdx != -1) cursor.getString(resIdx) ?: "" else "",
                contenuHtml = finalHtml
            ).also { cursor.close() }
        } else { cursor.close(); null }
    }

    fun insertChapitre(nom: String, numero: String, resume: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("nom", nom); put("numero", numero); put("resume", resume)
            put("contenu_html", "")
        }
        return db.insert(TABLE_CHAPITRE, null, cv)
    }

    fun updateChapitre(id: Long, nom: String, numero: String, resume: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("nom", nom); put("numero", numero); put("resume", resume) }
        db.update(TABLE_CHAPITRE, cv, "id=?", arrayOf(id.toString()))
    }

    fun saveChapitreContenu(id: Long, html: String) {
        android.util.Log.d("ScryBookDB", "Saving chapter $id, content length: ${html.length}")
        val db = writableDatabase
        val cv = ContentValues().apply { put("contenu_html", html) }
        val count = db.update(TABLE_CHAPITRE, cv, "id=?", arrayOf(id.toString()))
        android.util.Log.d("ScryBookDB", "Update result: $count rows affected")
    }

    fun deleteChapitre(id: Long) {
        writableDatabase.delete(TABLE_CHAPITRE, "id=?", arrayOf(id.toString()))
    }

    // ─── Personnages ─────────────────────────────────────────────────────────

    fun getPersonnages(): List<Personnage> {
        val list = mutableListOf<Personnage>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, COALESCE(alias,''), COALESCE(nom,''), COALESCE(prenom,''), COALESCE(sexe,''), COALESCE(age,0), COALESCE(desc_phys,''), COALESCE(desc_global,''), COALESCE(skill,'') FROM $TABLE_PERSO", null)
        while (cursor.moveToNext()) {
            list.add(Personnage(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getString(6), cursor.getString(7), cursor.getString(8)))
        }
        cursor.close()
        return list
    }

    fun insertPersonnage(p: Personnage): Long {
        val cv = ContentValues().apply {
            put("alias", p.alias); put("nom", p.nom); put("prenom", p.prenom)
            put("sexe", p.sexe); put("age", p.age); put("desc_phys", p.descPhys)
            put("desc_global", p.descGlobal); put("skill", p.skill)
        }
        return writableDatabase.insert(TABLE_PERSO, null, cv)
    }

    fun updatePersonnage(p: Personnage) {
        val cv = ContentValues().apply {
            put("alias", p.alias); put("nom", p.nom); put("prenom", p.prenom)
            put("sexe", p.sexe); put("age", p.age); put("desc_phys", p.descPhys)
            put("desc_global", p.descGlobal); put("skill", p.skill)
        }
        writableDatabase.update(TABLE_PERSO, cv, "id=?", arrayOf(p.id.toString()))
    }

    fun deletePersonnage(id: Long) { writableDatabase.delete(TABLE_PERSO, "id=?", arrayOf(id.toString())) }

    // ─── Lieux ───────────────────────────────────────────────────────────────

    fun getLieux(): List<Lieu> {
        val list = mutableListOf<Lieu>()
        val cursor = readableDatabase.rawQuery("SELECT id, COALESCE(nom,''), COALESCE(desc,'') FROM $TABLE_LIEUX", null)
        while (cursor.moveToNext()) list.add(Lieu(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
        cursor.close()
        return list
    }

    fun insertLieu(nom: String, desc: String): Long {
        val cv = ContentValues().apply { put("nom", nom); put("desc", desc) }
        return writableDatabase.insert(TABLE_LIEUX, null, cv)
    }

    fun updateLieu(id: Long, nom: String, desc: String) {
        val cv = ContentValues().apply { put("nom", nom); put("desc", desc) }
        writableDatabase.update(TABLE_LIEUX, cv, "id=?", arrayOf(id.toString()))
    }

    fun deleteLieu(id: Long) { writableDatabase.delete(TABLE_LIEUX, "id=?", arrayOf(id.toString())) }

    // ─── Sites ────────────────────────────────────────────────────────────────

    fun getSites(): List<co.dynag.scrybook.data.model.Site> {
        val list = mutableListOf<co.dynag.scrybook.data.model.Site>()
        val cursor = readableDatabase.rawQuery("SELECT id, COALESCE(nom,''), COALESCE(contenu,'') FROM $TABLE_SITE ORDER BY id", null)
        while (cursor.moveToNext()) list.add(co.dynag.scrybook.data.model.Site(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
        cursor.close()
        return list
    }

    fun getSite(id: Long): co.dynag.scrybook.data.model.Site? {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_SITE WHERE id=?", arrayOf(id.toString()))
        val idIdx = cursor.columnNames.indexOfFirst { it.equals("id", true) }
        val nomIdx = cursor.columnNames.indexOfFirst { it.equals("nom", true) }
        val conIdx = cursor.columnNames.indexOfFirst { it.equals("contenu", true) }

        return if (cursor.moveToFirst()) {
            co.dynag.scrybook.data.model.Site(
                id = if (idIdx != -1) cursor.getLong(idIdx) else 0L,
                nom = if (nomIdx != -1) cursor.getString(nomIdx) ?: "" else "",
                contenu = if (conIdx != -1) cursor.getString(conIdx) ?: "" else ""
            ).also { cursor.close() }
        } else { cursor.close(); null }
    }

    fun insertSite(nom: String, contenu: String): Long {
        val cv = ContentValues().apply { put("nom", nom); put("contenu", contenu) }
        return writableDatabase.insert(TABLE_SITE, null, cv)
    }

    fun updateSite(id: Long, nom: String, contenu: String) {
        val cv = ContentValues().apply { put("nom", nom); put("contenu", contenu) }
        writableDatabase.update(TABLE_SITE, cv, "id=?", arrayOf(id.toString()))
    }

    fun deleteSite(id: Long) { writableDatabase.delete(TABLE_SITE, "id=?", arrayOf(id.toString())) }

    // ─── Info ─────────────────────────────────────────────────────────────────

    fun getInfo(): Info {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_INFO WHERE id=1", null)
        val idIdx = cursor.columnNames.indexOfFirst { it.equals("id", true) }
        val titIdx = cursor.columnNames.indexOfFirst { it.equals("titre", true) }
        val stiIdx = cursor.columnNames.indexOfFirst { it.equals("stitre", true) }
        val autIdx = cursor.columnNames.indexOfFirst { it.equals("auteur", true) }
        val datIdx = cursor.columnNames.indexOfFirst { it.equals("date", true) }
        val resIdx = cursor.columnNames.indexOfFirst { it.equals("resume", true) }

        return if (cursor.moveToFirst()) {
            Info(
                id = if (idIdx != -1) cursor.getLong(idIdx) else 1L,
                titre = if (titIdx != -1) cursor.getString(titIdx) ?: "" else "",
                stitre = if (stiIdx != -1) cursor.getString(stiIdx) ?: "" else "",
                auteur = if (autIdx != -1) cursor.getString(autIdx) ?: "" else "",
                date = if (datIdx != -1) cursor.getString(datIdx) ?: "" else "",
                resume = if (resIdx != -1) cursor.getString(resIdx) ?: "" else ""
            ).also { cursor.close() }
        } else { cursor.close(); Info() }
    }

    fun saveInfo(info: Info) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("titre", info.titre); put("stitre", info.stitre)
            put("auteur", info.auteur); put("date", info.date); put("resume", info.resume)
        }
        val rows = db.update(TABLE_INFO, cv, "id=1", null)
        if (rows == 0) { cv.put("id", 1L); db.insert(TABLE_INFO, null, cv) }
    }

    // ─── Param ────────────────────────────────────────────────────────────────

    fun getParam(): Param {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_PARAM WHERE id=1", null)
        val idIdx = cursor.columnNames.indexOfFirst { it.equals("id", true) }
        val polIdx = cursor.columnNames.indexOfFirst { it.equals("police", true) }
        val taiIdx = cursor.columnNames.indexOfFirst { it.equals("taille", true) }
        val savIdx = cursor.columnNames.indexOfFirst { it.equals("save_time", true) }
        val lanIdx = cursor.columnNames.indexOfFirst { it.equals("langue", true) }
        val thmIdx = cursor.columnNames.indexOfFirst { it.equals("theme", true) }
        val fmtIdx = cursor.columnNames.indexOfFirst { it.equals("format_page", true) }

        return if (cursor.moveToFirst()) {
            Param(
                id = if (idIdx != -1) cursor.getLong(idIdx) else 1L,
                police = if (polIdx != -1) cursor.getString(polIdx) ?: "serif" else "serif",
                taille = if (taiIdx != -1) cursor.getString(taiIdx) ?: "16" else "16",
                saveTime = if (savIdx != -1) cursor.getString(savIdx) ?: "30" else "30",
                langue = if (lanIdx != -1) cursor.getString(lanIdx) ?: "fr" else "fr",
                theme = if (thmIdx != -1) cursor.getString(thmIdx) ?: "dark" else "dark",
                format = if (fmtIdx != -1) cursor.getString(fmtIdx) ?: "A4" else "A4"
            ).also { cursor.close() }
        } else { cursor.close(); Param() }
    }

    fun saveParam(param: Param) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("police", param.police); put("taille", param.taille)
            put("save_time", param.saveTime); put("langue", param.langue)
            put("theme", param.theme); put("format_page", param.format)
        }
        val rows = db.update(TABLE_PARAM, cv, "id=1", null)
        if (rows == 0) { cv.put("id", 1L); db.insert(TABLE_PARAM, null, cv) }
    }

    /** Force a checkpoint to ensure all data is in the main file */
    fun checkpoint() {
        try {
            readableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        } catch (e: Exception) {
            // Might fail if not in WAL mode, but it's okay.
        }
    }

    fun getChaptersAsJson(): String {
        val array = org.json.JSONArray()
        val cursor = readableDatabase.rawQuery("SELECT nom, numero, resume, contenu_html FROM $TABLE_CHAPITRE", null)
        while (cursor.moveToNext()) {
             val obj = org.json.JSONObject()
             obj.put("nom", cursor.getString(0) ?: "")
             obj.put("numero", cursor.getString(1) ?: "")
             obj.put("resume", cursor.getString(2) ?: "")
             obj.put("contenu_html", cursor.getString(3) ?: "")
             array.put(obj)
        }
        cursor.close()
        return array.toString()
    }
}
