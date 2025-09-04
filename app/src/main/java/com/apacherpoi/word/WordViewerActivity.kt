package com.apacherpoi.word

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.apacherpoi.TikaHelper
import com.example.mupdfviewer.R

class WordViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var edtSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnNext: Button
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_viewer)

        webView = findViewById(R.id.webView)
        edtSearch = findViewById(R.id.edtSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnNext = findViewById(R.id.btnNext)
        btnClear = findViewById(R.id.btnClear)


        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val fileUri = intent.data
        if (fileUri != null) {
            val html = TikaHelper().parseWordToHtmlWithImages(this, fileUri)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }

        btnSearch.setOnClickListener {
            val query = edtSearch.text.toString()
            if (query.isNotEmpty()) {
                webView.findAllAsync(query)
            }
        }

        btnNext.setOnClickListener {
            webView.findNext(true)
        }

        btnClear.setOnClickListener {
            webView.clearMatches()
        }
    }
}