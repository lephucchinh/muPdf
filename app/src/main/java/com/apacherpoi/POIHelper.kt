package com.apacherpoi

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.TextView
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

class POIHelper {

    fun readFileFromUri(context: Context, uri: Uri, block: (InputStream) -> Unit) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                block(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readExcelAndShow(uri: Uri, context: Context, textView: TextView) {
        readFileFromUri(context, uri) { inputStream ->
            XSSFWorkbook(inputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val sb = StringBuilder()
                for (row in sheet) {
                    val rowText = row.joinToString("\t") { cell ->
                        when (cell.cellTypeEnum) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            else -> ""
                        }
                    }
                    sb.append(rowText).append("\n")
                }
                textView.text = sb.toString()
            }
        }
    }


    fun readWordAndShow(uri: Uri, context: Context, textView: TextView) {
        readFileFromUri(context, uri) { inputStream ->
            XWPFDocument(inputStream).use { doc ->
                val allText = doc.paragraphs.joinToString("\n") { it.text }
                textView.text = allText
            }
        }
    }


    fun readPowerPointAndShow(uri: Uri, context: Context, textView: TextView) {
        readFileFromUri(context, uri) { inputStream ->
            XMLSlideShow(inputStream).use { ppt ->
                val sb = StringBuilder()
                ppt.slides.forEachIndexed { index, slide ->
                    slide.shapes.forEach { shape ->
                        if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                            sb.append("Slide $index: ${shape.text}\n")
                        }
                    }
                }
                textView.text = sb.toString()
            }
        }
    }


}
