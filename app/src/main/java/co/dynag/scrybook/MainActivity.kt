package co.dynag.scrybook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import co.dynag.scrybook.ui.navigation.AppNavigation
import co.dynag.scrybook.ui.theme.ScryBookTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** If set, the app opens this .sb file directly */
    val openFilePath = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            ScryBookTheme {
                AppNavigation(openFilePath = openFilePath.value)
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

    /** Resolve a content: or file: URI to a local file path */
    private fun resolveUri(uri: Uri): String? {
        // file:// scheme — direct path
        if (uri.scheme == "file") return uri.path

        // content:// scheme — copy to app cache
        try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "project.sb"
            val cacheFile = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
