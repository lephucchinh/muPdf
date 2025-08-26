package com.example.mupdfviewer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.artifex.mupdf.viewer.DocumentActivity
import com.example.mupdfviewer.databinding.LibraryActivityBinding

class LibraryActivity : Activity() {

    private val FILE_REQUEST = 42
    private lateinit var binding: LibraryActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LibraryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openFileButton.setOnClickListener {
            openFileChooser()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/vnd.ms-xpsdocument",
                "application/oxps",
                "application/x-cbz",
                "application/vnd.comicbook+zip",
                "application/epub+zip",
                "application/x-fictionbook",
                "application/x-mobipocket-ebook",
                "application/octet-stream"
            ))
        }
        startActivityForResult(intent, FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val intent = Intent(this, DocumentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    action = Intent.ACTION_VIEW
                    setDataAndType(uri, contentResolver.getType(uri))
                    putExtra("$packageName.ReturnToLibraryActivity", 1)
                }
                startActivity(intent)
                finish()
            }
        } /*else if (resultCode == Activity.RESULT_CANCELED) {
            finish()
        }*/
    }
}