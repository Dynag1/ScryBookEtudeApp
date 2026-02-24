package co.dynag.scrybook.ui.screens

import android.annotation.SuppressLint
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import co.dynag.scrybook.R
import co.dynag.scrybook.ui.viewmodel.EditorViewModel
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectPath: String,
    chapterId: Long,
    onBack: () -> Unit,
    onCharactersOpen: (() -> Unit)? = null,
    onPlacesOpen: (() -> Unit)? = null,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val chapitre by viewModel.chapitre.collectAsState()
    val htmlContent by viewModel.htmlContent.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
    val ttsReady by viewModel.ttsReady.collectAsState()

    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isSttListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Launcher for image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val imgDir = java.io.File(projectPath, "img")
                if (!imgDir.exists()) imgDir.mkdirs()
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                val destFile = java.io.File(imgDir, fileName)
                inputStream?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val path = destFile.absolutePath
                webView?.evaluateJavascript("insertImage('$path');", null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(projectPath, chapterId) {
        viewModel.loadChapitre(projectPath, chapterId)
    }

    // Save on back
    DisposableEffect(Unit) {
        onDispose { viewModel.saveNow() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNow()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    Column {
                        Text(
                            text = chapitre?.nom ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AnimatedVisibility(isSaving) {
                            Text(
                                text = stringResource(R.string.editor_saving),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    // Characters shortcut
                    onCharactersOpen?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_characters))
                        }
                    }
                    // Places shortcut
                    onPlacesOpen?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Place, contentDescription = stringResource(R.string.nav_places))
                        }
                    }
                    
                    // TTS button
                    IconButton(
                        onClick = {
                            val text = htmlContent
                            viewModel.toggleTts(text)
                        },
                        enabled = ttsReady
                    ) {
                        Icon(
                            if (isTtsPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = stringResource(R.string.action_tts),
                            tint = if (isTtsPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // STT button
                    IconButton(
                        onClick = {
                            if (isSttListening) {
                                speechRecognizer?.stopListening()
                                isSttListening = false
                            } else {
                                val sr = SpeechRecognizer.createSpeechRecognizer(context)
                                speechRecognizer = sr
                                sr.setRecognitionListener(object : RecognitionListener {
                                    override fun onResults(results: android.os.Bundle?) {
                                        isSttListening = false
                                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                        if (!matches.isNullOrEmpty()) {
                                            val text = matches[0]
                                            webView?.evaluateJavascript("insertTextAtCursor('${text.replace("'", "\\'")}');", null)
                                        }
                                    }
                                    override fun onReadyForSpeech(p: android.os.Bundle?) { isSttListening = true }
                                    override fun onError(error: Int) { isSttListening = false }
                                    override fun onEndOfSpeech() {}
                                    override fun onBeginningOfSpeech() {}
                                    override fun onRmsChanged(v: Float) {}
                                    override fun onBufferReceived(b: ByteArray?) {}
                                    override fun onPartialResults(b: android.os.Bundle?) {}
                                    override fun onEvent(t: Int, b: android.os.Bundle?) {}
                                })
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toLanguageTag())
                                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                }
                                sr.startListening(intent)
                            }
                        }
                    ) {
                        Icon(
                            if (isSttListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = stringResource(R.string.action_stt),
                            tint = if (isSttListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Formatting toolbar
            FormattingToolbar(webView = webView, onInsertImage = { imagePickerLauncher.launch("image/*") })

            // WebView editor
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onContentChanged(html: String) {
                                viewModel.updateContent(html)
                            }
                        }, "Android")
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                // Load initial content
                                val escaped = htmlContent
                                    .replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                    .replace("\n", "\\n")
                                view.evaluateJavascript("setContent('$escaped');", null)
                            }
                        }
                        loadDataWithBaseURL(null, EDITOR_HTML, "text/html", "UTF-8", null)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FormattingToolbar(webView: WebView?, onInsertImage: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { ToolbarIconButton(Icons.Default.FormatBold, "document.execCommand('bold');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatItalic, "document.execCommand('italic');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatUnderlined, "document.execCommand('underline');", webView) }
            item { ToolbarIconButton(Icons.Default.StrikethroughS, "document.execCommand('strikeThrough');", webView) }
            
            item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
            
            item { ToolbarTextButton("H1", "document.execCommand('formatBlock', false, 'H1');", webView) }
            item { ToolbarTextButton("H2", "document.execCommand('formatBlock', false, 'H2');", webView) }
            item { ToolbarTextButton("H3", "document.execCommand('formatBlock', false, 'H3');", webView) }
            
            item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
            
            item { ToolbarIconButton(Icons.Default.FormatAlignLeft, "document.execCommand('justifyLeft');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatAlignCenter, "document.execCommand('justifyCenter');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatAlignRight, "document.execCommand('justifyRight');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatAlignJustify, "document.execCommand('justifyFull');", webView) }
            
            item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
            
            item { ToolbarIconButton(Icons.Default.FormatListBulleted, "document.execCommand('insertUnorderedList');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatListNumbered, "document.execCommand('insertOrderedList');", webView) }
            
            item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
            
            item { ToolbarIconButton(Icons.Default.FormatIndentIncrease, "document.execCommand('indent');", webView) }
            item { ToolbarIconButton(Icons.Default.FormatIndentDecrease, "document.execCommand('outdent');", webView) }

            item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }

            item { ToolbarIconButton(Icons.Default.Image, "", webView, onClick = onInsertImage) }
            item { ToolbarIconButton(Icons.Default.FormatClear, "document.execCommand('removeFormat');", webView) }
            
            item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
            
            item { ToolbarIconButton(Icons.Default.Undo, "document.execCommand('undo');", webView) }
            item { ToolbarIconButton(Icons.Default.Redo, "document.execCommand('redo');", webView) }
        }
    }
}

@Composable
private fun ToolbarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, jsCommand: String, webView: WebView?, onClick: (() -> Unit)? = null) {
    IconButton(
        onClick = { 
            if (onClick != null) onClick() 
            else webView?.evaluateJavascript(jsCommand, null) 
        },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToolbarTextButton(label: String, jsCommand: String, webView: WebView?) {
    TextButton(
        onClick = { webView?.evaluateJavascript(jsCommand, null) },
        modifier = Modifier.height(36.dp).widthIn(min = 36.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

private val EDITOR_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: Georgia, serif;
    font-size: 19px;
    line-height: 1.6;
    background: #0F1318;
    color: #E8EAF0;
    padding: 16px;
    min-height: 100vh;
    overflow-x: hidden;
  }
  #editor {
    outline: none;
    min-height: 100%;
    caret-color: #B0C8FF;
    word-wrap: break-word;
  }
  h1 { text-align: center; font-size: 1.6em; margin: 1.2em 0 0.6em; color: #B0C8FF; }
  h2 { text-align: center; font-size: 1.4em; margin: 1em 0 0.5em; border-bottom: 1px solid #333; padding-bottom: 4px; }
  h3 { text-align: left; font-size: 1.2em; margin: 0.8em 0 0.4em; }
  p { margin-bottom: 0.8em; }
  ul, ol { margin-left: 20px; margin-bottom: 0.8em; }
  img { max-width: 100%; height: auto; display: block; margin: 10px auto; border-radius: 4px; }
</style>
</head>
<body>
<div id="editor" contenteditable="true" spellcheck="false"></div>
<script>
  var editor = document.getElementById('editor');
  var timer = null;

  editor.addEventListener('input', function() {
    clearTimeout(timer);
    timer = setTimeout(function() {
      Android.onContentChanged(editor.innerHTML);
    }, 500);
  });

  function setContent(html) {
    editor.innerHTML = html;
  }

  function insertTextAtCursor(text) {
    document.execCommand('insertText', false, text + ' ');
  }

  function insertImage(path) {
    document.execCommand('insertHTML', false, '<img src="file://' + path + '">');
  }

  // Focus helper
  document.addEventListener('click', function(e) {
    if (e.target.tagName !== 'A' && e.target.tagName !== 'IMG') {
        editor.focus();
    }
  });

  window.onload = function() {
    editor.focus();
  };
</script>
</body>
</html>
""".trimIndent()
