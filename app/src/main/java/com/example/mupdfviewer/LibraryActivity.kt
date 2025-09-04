package com.example.mupdfviewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.artifex.mupdf.viewer.DocumentActivity
import com.example.mupdfviewer.databinding.LibraryActivityBinding
import androidx.core.net.toUri
import com.apacherpoi.POIHelper

class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: LibraryActivityBinding

    // Launcher cho ACTION_OPEN_DOCUMENT
    private val openDocumentLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uri: Uri? = data?.data
                uri?.let { fileUri ->
                    val mimeType = contentResolver.getType(fileUri)
                    when (mimeType) {
                        "application/pdf" -> handleOpenDocument(fileUri)
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                            // đọc Word
                            POIHelper().readWordAndShow(fileUri, this)
                        }
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> {
                            // đọc Excel
                            POIHelper().readExcelAndShow(fileUri, this)
                        }
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> {
                            // đọc PPT
                            POIHelper().readPowerPointAndShow(fileUri, this)
                        }
                        else -> Toast.makeText(this, "Định dạng không hỗ trợ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LibraryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo launcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager().not()) {
                requestPermission()

            }
        }



        binding.openFileButton.setOnClickListener {
            openFileChooser()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback nếu ROM không hỗ trợ data=package
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        openDocumentLauncher.launch(intent)
    }

    val mimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       // xlsx
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" // pptx
    )

    private fun handleOpenDocument(uri: Uri) {
        try {
            // Giữ quyền truy cập lâu dài
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            val type = contentResolver.getType(uri) ?: "application/pdf"

            val intent = Intent(this, DocumentActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("$packageName.ReturnToLibraryActivity", 1)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LibraryActivity", "Không mở được file: ${e.message}", e)
        }
    }
}
