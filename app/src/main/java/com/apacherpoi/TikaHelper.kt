package com.apacherpoi

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.ToHTMLContentHandler
import org.apache.tika.metadata.Metadata
import java.io.InputStream

class TikaHelper {

    fun readFileFromUri(context: Context, uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun readExcelAndShow(uri: Uri, context: Context) {
        readFileFromUri(context, uri)?.use { inputStream ->
            XSSFWorkbook(inputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val sb = StringBuilder()
                for (row in sheet) {
                    val rowText = row.joinToString("\t") { cell ->
                        when (cell.cellType) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            else -> ""
                        }
                    }
                    sb.append(rowText).append("\n")
                }
            }
        }
    }

    fun readWordAndShow(uri: Uri, context: Context) {
        readFileFromUri(context, uri)?.use { inputStream ->
            XWPFDocument(inputStream).use { doc ->
                val allText = doc.paragraphs.joinToString("\n") { it.text }
            }
        }
    }

    fun parseWordToHtmlWithImages(context: Context, uri: Uri): String {
        readFileFromUri(context, uri)?.use { inputStream ->
            val doc = XWPFDocument(inputStream)
            val sb = StringBuilder()

            for (paragraph in doc.paragraphs) {
                sb.append("<p>")
                for (run in paragraph.runs) {
                    // text
                    run.text()?.let { sb.append(it) }

                    // hình ảnh nhúng
                    run.embeddedPictures.forEach { pic ->
                        val base64 = android.util.Base64.encodeToString(pic.pictureData.data, android.util.Base64.DEFAULT)
                        val ext = pic.pictureData.suggestFileExtension() ?: "png"
                        sb.append("<img src='data:image/$ext;base64,$base64' />")
                    }
                }
                sb.append("</p>")
            }

            return "<html><body>$sb</body></html>"
        }
        return "<html><body><p>Không đọc được file</p></body></html>"
    }



    fun readPowerPointAndShow(uri: Uri, context: Context) {
        readFileFromUri(context, uri)?.use { inputStream ->
            XMLSlideShow(inputStream).use { ppt ->
                val sb = StringBuilder()
                ppt.slides.forEachIndexed { index, slide ->
                    slide.shapes.filterIsInstance<org.apache.poi.xslf.usermodel.XSLFTextShape>()
                        .forEach { textShape ->
                            sb.append("Slide $index: ${textShape.text}\n")
                        }
                }
            }
        }
    }


}
