package co.dynag.scrybook.ui.viewmodel

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.Color
import android.graphics.Typeface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.dynag.scrybook.data.repository.ScryBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: ScryBookRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting

    private val _result = MutableStateFlow<ExportResult?>(null)
    val result: StateFlow<ExportResult?> = _result

    fun clearResult() { _result.value = null }

    /** Export tout le livre en PDF */
    fun exportBookPdf(projectPath: String, outputPath: String) {
        viewModelScope.launch {
            _exporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    repository.openProject(projectPath)
                    val info = repository.getInfo()
                    val chapitres = repository.getChapitres()

                    val pageWidth = 595 // A4 points
                    val pageHeight = 842
                    val margin = 72f // 1 inch
                    val contentWidth = pageWidth - (2 * margin).toInt()

                    val document = PdfDocument()
                    var pageNumber = 1

                    // --- Page de titre ---
                    val titlePage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    val titleCanvas = titlePage.canvas
                    val titlePaint = TextPaint().apply {
                        color = Color.BLACK; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        textSize = 28f; isAntiAlias = true
                    }
                    val subtitlePaint = TextPaint().apply {
                        color = Color.DKGRAY; typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                        textSize = 18f; isAntiAlias = true
                    }
                    val authorPaint = TextPaint().apply {
                        color = Color.DKGRAY; typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                        textSize = 14f; isAntiAlias = true
                    }

                    // Titre centré
                    val titleText = info.titre.ifBlank { File(projectPath).nameWithoutExtension }
                    var y = pageHeight * 0.35f
                    drawCenteredText(titleCanvas, titleText, titlePaint, pageWidth.toFloat(), y)
                    y += 50f

                    if (info.stitre.isNotBlank()) {
                        drawCenteredText(titleCanvas, info.stitre, subtitlePaint, pageWidth.toFloat(), y)
                        y += 40f
                    }

                    drawCenteredText(titleCanvas, info.auteur.ifBlank { "" }, authorPaint, pageWidth.toFloat(), pageHeight - margin - 30f)

                    document.finishPage(titlePage)
                    pageNumber++

                    // --- Chapitres ---
                    val bodyPaint = TextPaint().apply {
                        color = Color.BLACK; typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                        textSize = 12f; isAntiAlias = true
                    }
                    val chapterTitlePaint = TextPaint().apply {
                        color = Color.BLACK; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        textSize = 18f; isAntiAlias = true
                    }
                    val headerPaint = TextPaint().apply {
                        color = Color.GRAY; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                        textSize = 9f; isAntiAlias = true
                    }

                    for (chapitre in chapitres) {
                        val plainText = htmlToPlain(chapitre.contenuHtml)
                        val lines = wrapText(plainText, bodyPaint, contentWidth.toFloat())

                        // Titre du chapitre
                        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        var canvas = page.canvas
                        y = margin + 20f

                        // Header
                        canvas.drawText(chapitre.nom, pageWidth / 2f - chapterTitlePaint.measureText(chapitre.nom) / 2f, margin - 10f, headerPaint)

                        // Titre chapitre centré
                        drawCenteredText(canvas, chapitre.nom, chapterTitlePaint, pageWidth.toFloat(), y)
                        y += 40f

                        for (line in lines) {
                            if (y > pageHeight - margin) {
                                // Numéro de page
                                canvas.drawText("${pageNumber - 1}", pageWidth - margin, pageHeight - margin / 2, headerPaint)
                                document.finishPage(page)
                                pageNumber++
                                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                                canvas = page.canvas
                                y = margin + 10f
                                // Header
                                canvas.drawText(chapitre.nom, pageWidth / 2f - headerPaint.measureText(chapitre.nom) / 2f, margin - 10f, headerPaint)
                            }
                            canvas.drawText(line, margin, y, bodyPaint)
                            y += bodyPaint.textSize * 1.6f
                        }

                        canvas.drawText("${pageNumber - 1}", pageWidth - margin, pageHeight - margin / 2, headerPaint)
                        document.finishPage(page)
                        pageNumber++
                    }

                    // Sauvegarder
                    val file = File(outputPath)
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { document.writeTo(it) }
                    document.close()
                }
                _result.value = ExportResult.Success(outputPath)
            } catch (e: Exception) {
                _result.value = ExportResult.Error(e.message ?: "Erreur inconnue")
            } finally {
                _exporting.value = false
            }
        }
    }

    /** Export un seul chapitre en PDF */
    fun exportChapterPdf(projectPath: String, chapterId: Long, outputPath: String) {
        viewModelScope.launch {
            _exporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    repository.openProject(projectPath)
                    val chapitre = repository.getChapitre(chapterId) ?: throw Exception("Chapitre introuvable")

                    val pageWidth = 595
                    val pageHeight = 842
                    val margin = 72f
                    val contentWidth = pageWidth - (2 * margin).toInt()

                    val document = PdfDocument()
                    var pageNumber = 1

                    val bodyPaint = TextPaint().apply {
                        color = Color.BLACK; typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                        textSize = 12f; isAntiAlias = true
                    }
                    val chapterTitlePaint = TextPaint().apply {
                        color = Color.BLACK; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        textSize = 18f; isAntiAlias = true
                    }
                    val headerPaint = TextPaint().apply {
                        color = Color.GRAY; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                        textSize = 9f; isAntiAlias = true
                    }

                    val plainText = htmlToPlain(chapitre.contenuHtml)
                    val lines = wrapText(plainText, bodyPaint, contentWidth.toFloat())

                    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    var canvas = page.canvas
                    var y = margin + 20f

                    drawCenteredText(canvas, chapitre.nom, chapterTitlePaint, pageWidth.toFloat(), y)
                    y += 40f

                    for (line in lines) {
                        if (y > pageHeight - margin) {
                            canvas.drawText("$pageNumber", pageWidth - margin, pageHeight - margin / 2, headerPaint)
                            document.finishPage(page)
                            pageNumber++
                            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                            canvas = page.canvas
                            y = margin + 10f
                        }
                        canvas.drawText(line, margin, y, bodyPaint)
                        y += bodyPaint.textSize * 1.6f
                    }
                    canvas.drawText("$pageNumber", pageWidth - margin, pageHeight - margin / 2, headerPaint)
                    document.finishPage(page)
                    FileOutputStream(File(outputPath)).use { document.writeTo(it) }
                    document.close()
                }
                _result.value = ExportResult.Success(outputPath)
            } catch (e: Exception) {
                _result.value = ExportResult.Error(e.message ?: "Erreur inconnue")
            } finally {
                _exporting.value = false
            }
        }
    }

    private fun htmlToPlain(html: String): String {
        if (html.isBlank()) return ""
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    private fun wrapText(text: String, paint: TextPaint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isBlank()) { result.add(""); continue }
            val layout = StaticLayout.Builder.obtain(paragraph, 0, paragraph.length, paint, maxWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .build()
            for (i in 0 until layout.lineCount) {
                result.add(paragraph.substring(layout.getLineStart(i), layout.getLineEnd(i)).trimEnd())
            }
        }
        return result
    }

    private fun drawCenteredText(canvas: android.graphics.Canvas, text: String, paint: TextPaint, pageWidth: Float, y: Float) {
        val x = (pageWidth - paint.measureText(text)) / 2f
        canvas.drawText(text, x, y, paint)
    }
}

sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
