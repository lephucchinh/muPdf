package com.words

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import org.docx4j.convert.out.pdf.viaXSLFO.Conversion
import org.docx4j.convert.out.pdf.viaXSLFO.PdfSettings
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.DocPropsCorePart
import org.docx4j.openpackaging.parts.DocPropsExtendedPart
import org.docx4j.wml.P
import org.docx4j.wml.R
import org.docx4j.wml.Text
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordOfficeHelper(private val context: Context) {

    //region Đọc file Word cơ bản
    fun readWordFile(filePath: String): String {
        return try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))
            wordPackage.mainDocumentPart.content.toString()
        } catch (e: Exception) {
            "Lỗi khi đọc file: ${e.message}"
        }
    }

    fun readWordFileWithDetails(filePath: String) {
        try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))
            val mainDocumentPart = wordPackage.mainDocumentPart

            println("=== THÔNG TIN TÀI LIỆU ===")
            println("Tổng số phần tử: ${mainDocumentPart.content.size}")

            mainDocumentPart.content.forEachIndexed { index, element ->
                println("\n--- Phần tử $index ---")
                println("Loại: ${element.javaClass.simpleName}")
                println("Nội dung: $element")
            }

        } catch (e: Exception) {
            println("Lỗi: ${e.message}")
        }
    }
    //endregion

    //region Xử lý nội dung chi tiết
    fun processWordDocument(filePath: String) {
        try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))
            val paragraphs = wordPackage.mainDocumentPart.content

            println("=== XỬ LÝ ĐOẠN VĂN ===")

            paragraphs.forEachIndexed { index, obj ->
                if (obj is P) {
                    println("\n--- Đoạn văn ${index + 1} ---")
                    processParagraph(obj)
                }
            }

        } catch (e: Exception) {
            println("Lỗi khi xử lý tài liệu: ${e.message}")
        }
    }

    private fun processParagraph(paragraph: P) {
        paragraph.content.forEach { runObj ->
            if (runObj is R) {
                runObj.content.forEach { textObj ->
                    if (textObj is Text) {
                        print(textObj.value)
                    }
                }
            }
        }
        println()
    }

    fun extractTextOnly(filePath: String): String {
        return try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))
            val stringBuilder = StringBuilder()

            wordPackage.mainDocumentPart.content.forEach { obj ->
                if (obj is P) {
                    processParagraphToBuilder(obj, stringBuilder)
                    stringBuilder.append("\n")
                }
            }

            stringBuilder.toString()
        } catch (e: Exception) {
            "Lỗi khi trích xuất văn bản: ${e.message}"
        }
    }

    private fun processParagraphToBuilder(paragraph: P, builder: StringBuilder) {
        paragraph.content.forEach { runObj ->
            if (runObj is R) {
                runObj.content.forEach { textObj ->
                    if (textObj is Text) {
                        builder.append(textObj.value)
                    }
                }
            }
        }
    }
    //endregion

    //region Chuyển đổi định dạng
    fun convertToPdf(inputPath: String, outputPath: String): Boolean {
        return try {
            val wordPackage = WordprocessingMLPackage.load(File(inputPath))
            val pdfSettings = PdfSettings()

            val conversion = Conversion(wordPackage)
            FileOutputStream(outputPath).use { out ->
                conversion.output(out, pdfSettings)
            }

            println("Chuyển đổi thành công: $inputPath → $outputPath")
            true
        } catch (e: Exception) {
            println("Lỗi chuyển đổi: ${e.message}")
            false
        }
    }
    //endregion

    //region Đọc metadata - ĐÃ SỬA LỖI
    fun readMetadata(filePath: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))

            // Core properties - Sửa lỗi truy cập
            wordPackage.docPropsCorePart?.let { coreProps ->
                metadata["title"] = coreProps.contents.title?.toString() ?: "Không có"
                metadata["author"] = coreProps.contents.creator?.toString() ?: "Không có"
                metadata["createdDate"] = coreProps.contents.created?.toString() ?: "Không có"
                metadata["description"] = coreProps.contents.description?.toString() ?: "Không có"
            }

            // Extended properties - Sửa lỗi truy cập
            wordPackage.docPropsExtendedPart?.let { extendedProps ->
                extendedProps.contents?.let { contents ->
                    metadata["wordCount"] = contents.words?.toString() ?: "Không có"
                    metadata["pageCount"] = contents.pages?.toString() ?: "Không có"
                    metadata["characterCount"] = contents.characters?.toString() ?: "Không có"
                }
            }

        } catch (e: Exception) {
            metadata["error"] = "Lỗi đọc metadata: ${e.message}"
        }

        return metadata
    }

    fun printMetadata(filePath: String) {
        val metadata = readMetadata(filePath)

        println("=== METADATA TÀI LIỆU ===")
        metadata.forEach { (key, value) ->
            println("${key.replaceFirstChar { it.uppercase() }}: $value")
        }
    }
    //endregion

    //region Hiển thị lên WebView
    fun displayInWebView(uri: Uri, webView: WebView) {
        try {
            // Mở file từ URI sử dụng ContentResolver
            val inputStream = context.contentResolver.openInputStream(uri)
            val wordPackage = WordprocessingMLPackage.load(inputStream)
            val htmlContent = convertDocxToHtml(wordPackage)

            webView.loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )

        } catch (e: Exception) {
            webView.loadData(
                "<html><body><h3>Lỗi khi đọc file Word</h3><p>${e.message}</p></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    // Overload method để hỗ trợ cả file path và URI
    fun displayInWebView(filePath: String, webView: WebView) {
        try {
            val file = File(filePath)
            val uri = Uri.fromFile(file)
            displayInWebView(uri, webView)
        } catch (e: Exception) {
            webView.loadData(
                "<html><body><h3>Lỗi khi đọc file Word</h3><p>${e.message}</p></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    fun displayTextInWebView(filePath: String, webView: WebView) {
        try {
            val textContent = extractTextOnly(filePath)
            val formattedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            margin: 20px;
                            background-color: #f5f5f5;
                        }
                        .content {
                            background: white;
                            padding: 20px;
                            border-radius: 8px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        pre {
                            white-space: pre-wrap;
                            word-wrap: break-word;
                            font-size: 16px;
                        }
                    </style>
                </head>
                <body>
                    <div class="content">
                        <pre>${textContent.escapeHtml()}</pre>
                    </div>
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(
                null,
                formattedHtml,
                "text/html",
                "UTF-8",
                null
            )

        } catch (e: Exception) {
            webView.loadData(
                "<html><body><h3>Lỗi khi đọc file Word</h3><p>${e.message}</p></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun convertDocxToHtml(wordPackage: WordprocessingMLPackage): String {
        val stringBuilder = StringBuilder()
        val metadata = readMetadataFromPackage(wordPackage)

        stringBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${metadata["title"] ?: "Document"}</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        margin: 20px;
                        background-color: #f5f5f5;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                        background: white;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                    }
                    .header {
                        border-bottom: 2px solid #e0e0e0;
                        padding-bottom: 20px;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #2c3e50;
                        margin: 0;
                    }
                    .metadata {
                        color: #7f8c8d;
                        font-size: 14px;
                        margin-top: 10px;
                    }
                    .content {
                        font-size: 16px;
                    }
                    p {
                        margin-bottom: 16px;
                    }
                    .paragraph {
                        margin-bottom: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>${metadata["title"] ?: "Tài liệu Word"}</h1>
                        <div class="metadata">
                            <strong>Tác giả:</strong> ${metadata["author"] ?: "Không rõ"} | 
                            <strong>Ngày tạo:</strong> ${metadata["createdDate"] ?: "Không rõ"} | 
                            <strong>Số từ:</strong> ${metadata["wordCount"] ?: "N/A"}
                        </div>
                    </div>
                    <div class="content">
        """.trimIndent())

        // Xử lý nội dung
        wordPackage.mainDocumentPart.content.forEach { obj ->
            if (obj is P) {
                stringBuilder.append("<div class=\"paragraph\">")
                processParagraphToHtml(obj, stringBuilder)
                stringBuilder.append("</div>")
            }
        }

        stringBuilder.append("""
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent())

        return stringBuilder.toString()
    }

    private fun processParagraphToHtml(paragraph: P, builder: StringBuilder) {
        paragraph.content.forEach { runObj ->
            if (runObj is R) {
                runObj.content.forEach { textObj ->
                    if (textObj is Text) {
                        builder.append(textObj.value.escapeHtml())
                    }
                }
            }
        }
        builder.append("<br>")
    }

    private fun readMetadataFromPackage(wordPackage: WordprocessingMLPackage): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            // Core properties
            wordPackage.docPropsCorePart?.let { coreProps ->
                metadata["title"] = coreProps.contents.title?.toString() ?: "Không có"
                metadata["author"] = coreProps.contents.creator?.toString() ?: "Không có"
                metadata["createdDate"] = coreProps.contents.created?.toString() ?: "Không có"
                metadata["description"] = coreProps.contents.description?.toString() ?: "Không có"
            }

            // Extended properties
            wordPackage.docPropsExtendedPart?.let { extendedProps ->
                extendedProps.contents?.let { contents ->
                    metadata["wordCount"] = contents.words?.toString() ?: "Không có"
                    metadata["pageCount"] = contents.pages?.toString() ?: "Không có"
                    metadata["characterCount"] = contents.characters?.toString() ?: "Không có"
                }
            }

        } catch (e: Exception) {
            // Bỏ qua lỗi metadata
        }

        return metadata
    }

    private fun String.escapeHtml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
    //endregion

    //region Utility methods
    fun loadWordPackage(filePath: String): WordprocessingMLPackage? {
        return try {
            WordprocessingMLPackage.load(File(filePath))
        } catch (e: Exception) {
            println("Lỗi load file: ${e.message}")
            null
        }
    }

    fun getWordPackageContent(wordPackage: WordprocessingMLPackage): String {
        val builder = StringBuilder()
        wordPackage.mainDocumentPart.content.forEach { element ->
            if (element is P) {
                builder.append(element.extractText())
                builder.append("\n")
            }
        }
        return builder.toString()
    }

    private fun P.extractText(): String {
        val builder = StringBuilder()
        this.content.forEach { runObj ->
            if (runObj is R) {
                runObj.content.forEach { textObj ->
                    if (textObj is Text) {
                        builder.append(textObj.value)
                    }
                }
            }
        }
        return builder.toString()
    }
    //endregion

    //region Xử lý bất đồng bộ với Coroutines
    suspend fun readWordFileAsync(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))
            wordPackage.mainDocumentPart.content.toString()
        } catch (e: Exception) {
            "Lỗi khi đọc file: ${e.message}"
        }
    }

    suspend fun convertToPdfAsync(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val wordPackage = WordprocessingMLPackage.load(File(inputPath))
            val pdfSettings = PdfSettings()

            val conversion = Conversion(wordPackage)
            FileOutputStream(outputPath).use { out ->
                conversion.output(out, pdfSettings)
            }
            true
        } catch (e: Exception) {
            println("Lỗi chuyển đổi: ${e.message}")
            false
        }
    }

    suspend fun extractTextOnlyAsync(filePath: String): String = withContext(Dispatchers.IO) {
        extractTextOnly(filePath)
    }

    suspend fun readMetadataAsync(filePath: String): Map<String, String> = withContext(Dispatchers.IO) {
        readMetadata(filePath)
    }

    suspend fun displayInWebViewAsync(filePath: String, webView: WebView) = withContext(Dispatchers.IO) {
        val htmlContent = try {
            val wordPackage = WordprocessingMLPackage.load(File(filePath))
            convertDocxToHtml(wordPackage)
        } catch (e: Exception) {
            "<html><body><h3>Lỗi khi đọc file Word</h3><p>${e.message}</p></body></html>"
        }

        withContext(Dispatchers.Main) {
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }
    //endregion

    //region Kiểm tra file
    fun isValidWordFile(filePath: String): Boolean {
        return try {
            WordprocessingMLPackage.load(File(filePath))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getFileInfo(filePath: String): Map<String, Any> {
        val file = File(filePath)
        return mapOf(
            "exists" to file.exists(),
            "isFile" to file.isFile,
            "size" to file.length(),
            "lastModified" to file.lastModified(),
            "isValidWord" to isValidWordFile(filePath)
        )
    }
    //endregion
}

// Extension functions để sử dụng bên ngoài class
fun WordOfficeHelper.quickRead(filePath: String): String {
    return this.extractTextOnly(filePath)
}

fun WordOfficeHelper.batchConvertToPdf(inputFiles: List<String>, outputDirectory: String): Map<String, Boolean> {
    val results = mutableMapOf<String, Boolean>()

    inputFiles.forEach { inputFile ->
        val outputFile = "$outputDirectory/${File(inputFile).nameWithoutExtension}.pdf"
        results[inputFile] = this.convertToPdf(inputFile, outputFile)
    }

    return results
}