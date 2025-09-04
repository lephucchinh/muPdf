package com.apacherpoi

import android.app.AlertDialog
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
