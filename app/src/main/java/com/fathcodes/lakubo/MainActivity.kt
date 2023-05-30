package com.fathcodes.lakubo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1
    }

    // Properti untuk menyimpan callback dan parameter file chooser
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var fileChooserParams: WebChromeClient.FileChooserParams? = null

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
        webView.webChromeClient = object : WebChromeClient() {
            // Untuk Android 5.0+
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                this@MainActivity.fileChooserParams = fileChooserParams

                val intent = fileChooserParams?.createIntent()
                intent!!.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Mengizinkan pemilihan multiple gambar
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                return true
            }

            // Sebelum Android 5.0
            @SuppressWarnings("unused")
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri>?,
                acceptType: String?,
                capture: String?
            ) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*" // Menambahkan tipe intent sebagai "image/*"
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            }

            // Android 4.1 - 4.4
            @SuppressWarnings("unused")
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri>?,
                acceptType: String?
            ) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*" // Menambahkan tipe intent sebagai "image/*"
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            }
        }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val clipData = data.clipData
                val result = if (clipData != null) {
                    // Multiple gambar dipilih
                    val count = clipData.itemCount
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until count) {
                        val uri = clipData.getItemAt(i).uri
                        uris.add(uri)
                    }
                    uris.toTypedArray()
                } else {
                    // Satu gambar dipilih
                    arrayOf(data.data!!)
                }
                filePathCallback?.onReceiveValue(result) // Berikan URI ke WebView
                filePathCallback = null // Setelah memberikan nilai, kosongkan kembali
            }
        }
    }
}
