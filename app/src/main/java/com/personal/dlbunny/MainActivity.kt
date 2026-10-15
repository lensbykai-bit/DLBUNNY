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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        browser = WebView(this)
        container.addView(browser, FrameLayout.LayoutParams(-1, -1))

        splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(13, 18, 36), Color.rgb(31, 18, 55))
            )
        }
        val emblem = TextView(this).apply {
            text = "🐰"
            textSize = 62f
            gravity = Gravity.CENTER
        }
        val brand = TextView(this).apply {
            text = "DL Bunny"
            textSize = 34f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val tagline = TextView(this).apply {
            text = "PERSONAL DOWNLOADER"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(Color.rgb(176, 180, 210))
            gravity = Gravity.CENTER
        }
        splash.addView(emblem)
        splash.addView(brand)
        splash.addView(tagline)
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
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
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
