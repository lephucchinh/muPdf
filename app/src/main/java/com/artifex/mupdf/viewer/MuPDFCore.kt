package com.artifex.mupdf.viewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.artifex.mupdf.fitz.*
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.example.mupdfviewer.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MuPDFCore private constructor(private var doc: Document?) {

    private var resolution: Int = 160
    var outline: Array<Outline>? = null
    private var pageCount: Int = -1
    private var reflowable: Boolean = false
    private var currentPage: Int = -1
    private var page: Page? = null
    private var pageWidth: Float = 0f
    private var pageHeight: Float = 0f
    private var displayList: DisplayList? = null

    /* Default to "A Format" pocket book size. */
    private var layoutW = 312
    private var layoutH = 504
    private var layoutEM = 10

    companion object {
        var nightMode = false
    }


    init {
        doc?.let {
            it.layout(layoutW.toFloat(), layoutH.toFloat(), layoutEM.toFloat())
            pageCount = it.countPages()
            reflowable = it.isReflowable()
        }
    }

    constructor(buffer: ByteArray, magic: String) : this(Document.openDocument(buffer, magic))

    constructor(stm: SeekableInputStream, magic: String) : this(Document.openDocument(stm, magic))

    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
    }

    fun isNightMode() = nightMode

    fun getTitle(): String? {
        return doc?.getMetaData(Document.META_INFO_TITLE)
    }

    fun countPages(): Int {
        return pageCount
    }

    fun isReflowable(): Boolean {
        return reflowable
    }

    @Synchronized
    fun layout(oldPage: Int, w: Int, h: Int, em: Int): Int {
        if (w != layoutW || h != layoutH || em != layoutEM) {
            println("LAYOUT: $w,$h")
            layoutW = w
            layoutH = h
            layoutEM = em
            val mark = doc!!.makeBookmark(doc!!.locationFromPageNumber(oldPage))
            doc!!.layout(layoutW.toFloat(), layoutH.toFloat(), layoutEM.toFloat())
            currentPage = -1
            pageCount = doc!!.countPages()
            outline = null
            try {
                outline = doc!!.loadOutline()
            } catch (ex: Exception) {
                /* ignore error */
            }
            return doc!!.pageNumberFromLocation(doc!!.findBookmark(mark))
        }
        return oldPage
    }

    @Synchronized
    private fun gotoPage(pageNum: Int) {
        /* TODO: page cache */
        var pageNum = pageNum
        if (pageNum > pageCount - 1)
            pageNum = pageCount - 1
        else if (pageNum < 0)
            pageNum = 0
        if (pageNum != currentPage) {
            page?.destroy()
            page = null
            displayList?.destroy()
            displayList = null
            pageWidth = 0f
            pageHeight = 0f
            currentPage = -1

            doc?.let {
                page = it.loadPage(pageNum)
                val b = page!!.bounds
                pageWidth = b.x1 - b.x0
                pageHeight = b.y1 - b.y0
            }

            currentPage = pageNum
        }
    }

    @Synchronized
    fun getPageSize(pageNum: Int): PointF {
        gotoPage(pageNum)
        return PointF(pageWidth, pageHeight)
    }

    @Synchronized
    fun onDestroy() {
        displayList?.destroy()
        displayList = null
        page?.destroy()
        page = null
        doc?.destroy()
        doc = null
    }

    @Synchronized
    fun drawPage(
        bm: Bitmap, pageNum: Int,
        pageW: Int, pageH: Int,
        patchX: Int, patchY: Int,
        patchW: Int, patchH: Int,
        cookie: Cookie
    ) {
        gotoPage(pageNum)

        if (displayList == null && page != null) {
            try {
                displayList = page!!.toDisplayList()
            } catch (ex: Exception) {
                displayList = null
            }
        }

        if (displayList == null || page == null)
            return

        val zoom = resolution / 72f
        val ctm = Matrix(zoom, zoom)
        val bbox = RectI(page!!.bounds.transform(ctm))
        val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0)
        val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0)
        ctm.scale(xscale, yscale)

        // Tạo một đối tượng Rect từ các tọa độ patch.
        // Lời gọi 'run' yêu cầu một Rect để xác định vùng cần vẽ lại.
        val areaToDraw = Rect(
            patchX.toFloat(),
            patchY.toFloat(),
            (patchX + patchW).toFloat(),
            (patchY + patchH).toFloat()
        )

        val dev = AndroidDrawDevice(bm, patchX, patchY)
        try {
            // Sửa lại lời gọi run() với các tham số theo đúng thứ tự
            // (Device, Matrix, Rect, Cookie)
            displayList!!.run(dev, ctm, areaToDraw, cookie)
            dev.close()
        } finally {
            dev.destroy()
        }

        // Xử lý chế độ ban đêm sau khi vẽ xong
        if (nightMode) {
            invertBitmap(bm)
        }
    }

    private fun invertBitmap(bitmap: Bitmap) {
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        val matrixInvert = android.graphics.ColorMatrix()
        matrixInvert.set(
            floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
        val filter = android.graphics.ColorMatrixColorFilter(matrixInvert)
        paint.colorFilter = filter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }


    @Synchronized
    fun updatePage(
        bm: Bitmap, pageNum: Int,
        pageW: Int, pageH: Int,
        patchX: Int, patchY: Int,
        patchW: Int, patchH: Int,
        cookie: Cookie
    ) {
        drawPage(bm, pageNum, pageW, pageH, patchX, patchY, patchW, patchH, cookie)
    }

    @Synchronized
    fun getPageLinks(pageNum: Int): Array<Link>? {
        gotoPage(pageNum)
        return page?.links
    }

    @Synchronized
    fun resolveLink(link: Link): Int {
        return doc!!.pageNumberFromLocation(doc!!.resolveLink(link))
    }

    @Synchronized
    fun searchPage(pageNum: Int, text: String): Array<Array<Quad>>? {
        gotoPage(pageNum)
        return page?.search(text)
    }

    @Synchronized
    fun hasOutline(): Boolean {
        if (outline == null) {
            try {
                outline = doc!!.loadOutline()
            } catch (ex: Exception) {
                /* ignore error */
            }
        }
        return outline != null
    }

    private fun flattenOutlineNodes(
        result: ArrayList<Item>,
        list: Array<Outline>?,
        indent: String
    ) {
        list?.forEach { node ->
            node.title?.let {
                val page = doc!!.pageNumberFromLocation(doc!!.resolveLink(node))
                result.add(Item(indent + it, page))
            }
            flattenOutlineNodes(result, node.down, indent + "    ")
        }
    }

    @Synchronized
    fun getOutline(): ArrayList<Item> {
        val result = ArrayList<Item>()
        flattenOutlineNodes(result, outline, "")
        return result
    }

    @Synchronized
    fun needsPassword(): Boolean {
        return doc!!.needsPassword()
    }

    @Synchronized
    fun authenticatePassword(password: String): Boolean {
        return doc!!.authenticatePassword(password)
    }

    suspend fun convertPDFPageToPNG(
        pageNum: Int,
        width: Int = 1080,
        height: Int = 1920,
        quality: Int = 90,
        fileName: String? = null,
        cookie: Cookie? = null
    ): File? = withContext(Dispatchers.IO) {
        if (pageNum < 0 || pageNum >= pageCount) {
            Log.e("PDFToPNGConverter", "Invalid page number: $pageNum")
            return@withContext null
        }

        try {
            // Convert page to bitmap
            val bitmap = convertPageToBitmap(pageNum, width, height, cookie)
                ?: return@withContext null

            // Create cache directory if not exists
            val cacheDir = File(App.instance.cacheDir, "pdf_to_png")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Generate file name
            val finalFileName = fileName ?: "page_${pageNum}_${System.currentTimeMillis()}.png"
            val pngFile = File(cacheDir, finalFileName)

            // Save bitmap to PNG file
            val success = saveBitmapToPNG(bitmap, pngFile, quality)

            // Recycle bitmap to free memory
            bitmap.recycle()

            if (success) {
                Log.d("PDFToPNGConverter", "Successfully saved PNG: ${pngFile.absolutePath}")
                return@withContext pngFile
            } else {
                Log.e("PDFToPNGConverter", "Failed to save PNG file")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e("PDFToPNGConverter", "Error converting page $pageNum to PNG: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun convertAllPagesToPNG(
        width: Int = 1080,
        height: Int = 1920,
        quality: Int = 90,
        fileNamePrefix: String = "page",
        cookie: Cookie? = null
    ): List<File> = withContext(Dispatchers.IO) {
        val convertedFiles = mutableListOf<File>()

        try {
            for (i in 0 until pageCount) {
                val fileName = "${fileNamePrefix}_${i + 1}.png"
                val file = convertPDFPageToPNG(i, width, height, quality, fileName, cookie)
                file?.let { convertedFiles.add(it) }
            }
        } catch (e: Exception) {
            Log.e("PDFToPNGConverter", "Error converting all pages: ${e.message}", e)
        }

        return@withContext convertedFiles
    }

    private fun convertPageToBitmap(
        pageNum: Int,
        width: Int,
        height: Int,
        cookie: Cookie? = null
    ): Bitmap? {
        try {
            // Lấy kích thước thực của trang để tính toán tỷ lệ chính xác
            gotoPage(pageNum)
            if (page == null) {
                Log.e("PDFToPNGConverter", "Page object is null for page $pageNum")
                return null
            }
            val pageSize = getPageSize(pageNum)

            // Tạo Bitmap trống với kích thước mong muốn
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Tính toán tỷ lệ để phù hợp với kích thước Bitmap
            val scaleX = width / pageSize.x
            val scaleY = height / pageSize.y
            val scale = Math.min(scaleX, scaleY) // Giữ tỷ lệ khung hình

            val scaledPageWidth = (pageSize.x * scale).toInt()
            val scaledPageHeight = (pageSize.y * scale).toInt()

            // Tính toán offset để căn giữa trang nếu tỷ lệ không khớp hoàn toàn
            val offsetX = (width - scaledPageWidth) / 2
            val offsetY = (height - scaledPageHeight) / 2

            // Vẽ trang lên Bitmap với background trắng
            bitmap.eraseColor(android.graphics.Color.WHITE)

            drawPage(
                bm = bitmap,
                pageNum = pageNum,
                pageW = width,
                pageH = height,
                patchX = 0,
                patchY = 0,
                patchW = width,
                patchH = height,
                cookie = cookie ?: Cookie()
            )

            return bitmap
        } catch (e: Exception) {
            Log.e("PDFToPNGConverter", "Error converting page $pageNum to bitmap: ${e.message}", e)
            return null
        }
    }

    private fun saveBitmapToPNG(bitmap: Bitmap, file: File, quality: Int): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, quality, out)
                out.flush()
                true
            }
        } catch (e: IOException) {
            Log.e("PDFToPNGConverter", "Error saving bitmap to PNG: ${e.message}", e)
            false
        }
    }

    // Function to share/download the PNG file
    fun saveToDownloads(file: File) {
        try {
            val context = App.instance
            val filename = file.name

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "image/png")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection =
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)

                itemUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    Toast.makeText(context, "Saved to Downloads: $filename", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                // Android 9 and below
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, filename)
                file.copyTo(destFile, overwrite = true)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
                Toast.makeText(context, "Saved to Downloads: $filename", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("PDFToPNGConverter", "Error saving PNG: ${e.message}", e)
            Toast.makeText(App.instance, "Error saving file: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // Function to save to Downloads folder
    suspend fun saveToDownloads(file: File, fileName: String? = null): File? =
        withContext(Dispatchers.IO) {
            try {
                // Get Downloads directory
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val finalFileName = fileName ?: file.name
                val downloadFile = File(downloadsDir, finalFileName)

                // Copy file to Downloads
                file.copyTo(downloadFile, overwrite = true)

                // Notify media scanner
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(downloadFile)
                App.instance.sendBroadcast(intent)

                Log.d("PDFToPNGConverter", "File saved to Downloads: ${downloadFile.absolutePath}")
                return@withContext downloadFile

            } catch (e: Exception) {
                Log.e("PDFToPNGConverter", "Error saving to Downloads: ${e.message}", e)
                return@withContext null
            }
        }

    // Function to get all PNG files in cache
    fun getCachedPNGFiles(): List<File> {
        val cacheDir = File(App.instance.cacheDir, "pdf_to_png")
        return if (cacheDir.exists()) {
            cacheDir.listFiles { _, name -> name.endsWith(".png") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Function to clear cache
    fun clearCache() {
        try {
            val cacheDir = File(App.instance.cacheDir, "pdf_to_png")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                Log.d("PDFToPNGConverter", "Cache cleared successfully")
            }
        } catch (e: Exception) {
            Log.e("PDFToPNGConverter", "Error clearing cache: ${e.message}", e)
        }
    }
}
