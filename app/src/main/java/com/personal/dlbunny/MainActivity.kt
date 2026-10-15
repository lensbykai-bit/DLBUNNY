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
