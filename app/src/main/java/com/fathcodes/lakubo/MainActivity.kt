package com.fathcodes.lakubo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var fileChooserParams: WebChromeClient.FileChooserParams? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = createWebChromeClient()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val url = "https://lakubo.shop"
        webView.loadUrl(url)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        webView.webViewClient = createWebViewClient()

        fileChooserLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val uris = when {
                        data.clipData != null -> {
                            val count = data.clipData!!.itemCount
                            (0 until count).map { data.clipData!!.getItemAt(it).uri }.toTypedArray()
                        }
                        data.data != null -> arrayOf(data.data!!)
                        else -> arrayOf()
                    }
                    filePathCallback?.onReceiveValue(uris)
                    filePathCallback = null
                }
            }
        }

        // Memeriksa izin WRITE_EXTERNAL_STORAGE secara dinamis
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            // Izin diberikan, Anda dapat melanjutkan unduhan file
            setupDownloadListener()
        }
    }

    private fun setupDownloadListener() {
        webView.setDownloadListener { url, _, _, _, _ ->
            val fileName = URLUtil.guessFileName(url, null, null)
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType("application/octet-stream")
            request.addRequestHeader("User-Agent", webView.settings.userAgentString)
            request.setDescription("Lakubo.shop")
            request.setTitle(fileName)
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Menentukan direktori tujuan penyimpanan file yang diunduh
            val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadDirectory, fileName)
            request.setDestinationUri(Uri.fromFile(file))

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, Anda dapat melanjutkan unduhan file
                setupDownloadListener()
            } else {
                // Izin ditolak, berikan penanganan yang sesuai (misalnya, beri tahu pengguna bahwa unduhan tidak dapat dilakukan)
            }
        }
    }

    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                this@MainActivity.fileChooserParams = fileChooserParams

                val intent = fileChooserParams?.createIntent()
                intent?.type = "image/*"
                intent?.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                fileChooserLauncher.launch(intent)
                return true
            }
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Menghandle unduhan file yang dimulai dari WebView
                if (!url.isNullOrEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                    if (url.endsWith("/pdf")) { // Ganti dengan URL akhiran yang sesuai dengan file PDF
                        val fileName = getFileNameFromUrl(url) // Mendapatkan nama file dari URL

                        // Menentukan direktori tujuan penyimpanan file yang diunduh
                        val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        val file = File(downloadDirectory, fileName)

                        val request = DownloadManager.Request(Uri.parse(url))
                        request.setMimeType(getMimeType(url))
                        request.addRequestHeader("User-Agent", webView.settings.userAgentString)
                        request.setDescription("Lakubo.shop")
                        request.setTitle(fileName)
                        request.allowScanningByMediaScanner()
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        request.setDestinationUri(Uri.fromFile(file))

                        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.enqueue(request)
                        return true
                    }
                }
                return false
            }

            private fun getFileNameFromUrl(url: String): String {
                val transactionId = url.substringAfterLast("/transactions/").substringBefore("/pdf")
                val fileName = "transactions_$transactionId.pdf"
                return fileName
            }


            private fun getMimeType(url: String): String {
                val fileExtension = MimeTypeMap.getFileExtensionFromUrl(url)
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
                    ?: "application/octet-stream"
            }
        }
    }


    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
