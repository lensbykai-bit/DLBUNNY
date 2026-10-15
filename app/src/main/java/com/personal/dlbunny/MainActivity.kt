package com.personal.dlbunny

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import android.content.Intent
import android.widget.Toast
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var splash: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var minimumSplashElapsed = false
    private var pageReady = false
    private var currentDownload: Long? = null
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        browser = WebView(this)
        val appLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 15, 34))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 10, 20, 10)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(25, 23, 63), Color.rgb(48, 34, 93))
            )
        }
        val title = TextView(this).apply {
            text = "🐰  DL Bunny"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        toolbar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val downloads = TextView(this).apply {
            text = "⬇ Downloads"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(230, 65, 159), Color.rgb(94, 100, 244))
            ).apply { cornerRadius = 22f }
            setOnClickListener {
                try {
                    startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Downloads unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        }
        toolbar.addView(downloads)
        appLayout.addView(toolbar)
        appLayout.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        val downloadPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 12)
            setBackgroundColor(Color.rgb(19, 23, 52))
        }
        statusText = TextView(this).apply {
            text = "Ready to download"
            textSize = 13f
            setTextColor(Color.rgb(225, 224, 255))
        }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(244, 88, 179))
        }
        downloadPanel.addView(statusText)
        downloadPanel.addView(progressBar, LinearLayout.LayoutParams(-1, 12))
        appLayout.addView(downloadPanel)
        container.addView(appLayout, FrameLayout.LayoutParams(-1, -1))

        splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(8, 14, 33), Color.rgb(37, 24, 80), Color.rgb(13, 24, 57))
            )
        }
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()
        val logoCard = FrameLayout(this).apply {
            elevation = dp(20).toFloat()
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(255, 93, 189), Color.rgb(119, 86, 255), Color.rgb(48, 181, 255))
            ).apply {
                cornerRadius = dp(34).toFloat()
                setStroke(dp(2), Color.rgb(200, 198, 255))
            }
            rotation = -7f
        }
        val emblem = TextView(this).apply {
            text = "🐰"
            textSize = 67f
            gravity = Gravity.CENTER
            setShadowLayer(dp(9).toFloat(), 0f, dp(5).toFloat(), Color.rgb(54, 31, 100))
        }
        logoCard.addView(emblem, FrameLayout.LayoutParams(-1, -1))
        splash.addView(logoCard, LinearLayout.LayoutParams(dp(146), dp(146)).apply {
            bottomMargin = dp(30)
        })
        val brand = TextView(this).apply {
            text = "DL Bunny"
            textSize = 39f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(dp(8).toFloat(), 0f, dp(5).toFloat(), Color.rgb(156, 75, 226))
        }
        val tagline = TextView(this).apply {
            text = "YOUR PERSONAL DOWNLOADER"
            textSize = 11f
            letterSpacing = 0.2f
            setTextColor(Color.rgb(195, 202, 243))
            gravity = Gravity.CENTER
        }
        val loading = TextView(this).apply {
            text = "●  ●  ●"
            textSize = 16f
            letterSpacing = 0.25f
            setTextColor(Color.rgb(255, 123, 202))
            gravity = Gravity.CENTER
        }
        splash.addView(brand)
        splash.addView(tagline)
        splash.addView(loading, LinearLayout.LayoutParams(-1, dp(55)).apply {
            topMargin = dp(28)
        })
        container.addView(splash, FrameLayout.LayoutParams(-1, -1))
        setContentView(container)

        browser.settings.javaScriptEnabled = true
        browser.settings.domStorageEnabled = true
        browser.webChromeClient = WebChromeClient()
        browser.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                dismissSplashWhenReady()
            }
        }
        browser.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                if (!url.startsWith("https://", ignoreCase = true)) {
                    throw IllegalArgumentException("Only HTTPS downloads are supported")
                }
                val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url))
                request.addRequestHeader("User-Agent", userAgent ?: "")
                CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
                request.setTitle(name)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                currentDownload = manager.enqueue(request)
                statusText.text = "Downloading: $name"
                progressBar.progress = 0
                updateDownloadProgress()
            } catch (e: Exception) {
                Toast.makeText(this, "Download unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        handler.postDelayed({
            minimumSplashElapsed = true
            dismissSplashWhenReady()
        }, 1600)
        handler.postDelayed({ if (!isFinishing) splash.visibility = View.GONE }, 8000)
        browser.loadUrl("https://dlbunny.com/")
    }

    private fun updateDownloadProgress() {
        val id = currentDownload ?: return
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query().setFilterById(id))
        cursor.use {
            if (it.moveToFirst()) {
                val state = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val done = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                if (total > 0) {
                    val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                    progressBar.progress = percent
                    statusText.text = "Downloading... $percent%"
                } else {
                    statusText.text = "Downloading..."
                }
                when (state) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        statusText.text = "✓ Download complete — open Downloads"
                        progressBar.progress = 100
                        currentDownload = null
                    }
                    DownloadManager.STATUS_FAILED -> {
                        statusText.text = "Download failed — please retry"
                        currentDownload = null
                    }
                }
            } else {
                currentDownload = null
            }
        }
        if (currentDownload != null) handler.postDelayed({ updateDownloadProgress() }, 900)
    }

    private fun dismissSplashWhenReady() {
        if (minimumSplashElapsed && pageReady) {
            splash.animate().alpha(0f).setDuration(300).withEndAction {
                splash.visibility = View.GONE
            }.start()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        browser.destroy()
        super.onDestroy()
    }
}
