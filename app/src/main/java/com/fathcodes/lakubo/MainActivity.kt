package com.fathcodes.lakubo

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menginisialisasi tata letak dan WebView
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Mengaktifkan JavaScript
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Mengatur klien WebView agar tampil di aplikasi kita, bukan browser eksternal
        webView.webViewClient = WebViewClient()

        // Mengatur klien WebChrome agar mendapatkan notifikasi terkait halaman web (misalnya judul halaman)
        webView.webChromeClient = WebChromeClient()

        // Memuat URL halaman web yang diinginkan
        val url = "https://lakubo.shop"
        webView.loadUrl(url)

        // Mengatur listener refresh saat swipe ke bawah
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Mengatur listener saat proses refresh selesai
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // Menangani navigasi kembali saat tombol Kembali ditekan
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
