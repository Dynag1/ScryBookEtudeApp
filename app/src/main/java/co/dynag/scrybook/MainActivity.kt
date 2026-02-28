package co.dynag.scrybook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import co.dynag.scrybook.data.repository.ScryBookRepository
import co.dynag.scrybook.ui.navigation.AppNavigation
import co.dynag.scrybook.ui.theme.ScryBookTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: ScryBookRepository

    /** If set, the app opens this .sb file directly */
    val openFilePath = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val param by repository.currentParam.collectAsState()
            
            val configuration = LocalConfiguration.current
            val locale = when (param.langue) {
                "fr" -> Locale.FRENCH
                "en" -> Locale.ENGLISH
                else -> Locale.getDefault()
            }
            
            val updatedConfig = configuration.apply {
                setLocale(locale)
            }

            CompositionLocalProvider(LocalConfiguration provides updatedConfig) {
                ScryBookTheme(appTheme = param.theme) {
                    AppNavigation(openFilePath = openFilePath.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            val path = resolveUri(uri)
            if (path != null && path.endsWith(".sb")) {
                openFilePath.value = path
            }
        }
    }

    /** Resolve a content: or file: URI to a local persistent path */
    private fun resolveUri(uri: Uri): String? {
        // file:// scheme — direct path
        if (uri.scheme == "file") return uri.path

        // content:// scheme — copy to persistent storage
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "project.sb"
            val destDir = File(getExternalFilesDir("ScryBook"), "")
            destDir.mkdirs()
            val destFile = File(destDir, fileName)
            
            // Only copy if destination doesn't exist to avoid overwriting current local edits
            if (!destFile.exists()) {
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // Take persistable permission if possible
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) { /* Not supported by all providers */ }

            // Link the URI to the path so syncBack will work
            repository.openProject(destFile.absolutePath, uri.toString())
            
            return destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
