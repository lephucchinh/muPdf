package com.words

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.mupdfviewer.R

class WordViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var wordHelper: WordOfficeHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.word_viewer_activity)

        webView = findViewById(R.id.webView)
        wordHelper = WordOfficeHelper(this)

        // Cấu hình WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false

        // Lấy file path từ intent
        val filePath = intent.getStringExtra("file_path")
        Log.d("chinhlp", "filePath: $filePath")
        if (filePath != null) {
            // Hiển thị file Word lên WebView
            wordHelper.displayInWebView(filePath.toUri(), webView)

            // Hoặc hiển thị text only
            // wordHelper.displayTextInWebView(filePath, webView)

            // Hoặc sử dụng coroutine để xử lý bất đồng bộ
            // lifecycleScope.launch {
            //     wordHelper.displayInWebViewAsync(filePath, webView)
            // }
        }
    }

    // Xử lý nút back cho WebView
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}