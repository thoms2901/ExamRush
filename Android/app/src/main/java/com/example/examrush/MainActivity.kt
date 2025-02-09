package com.example.examrush

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.examrush.ui.theme.ExamRushTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            ExamRushTheme {
                AndroidView(
                    factory = { createWebView() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun createWebView(): WebView {
        return WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            webViewClient = WebViewClient()

            addJavascriptInterface(JavaScriptInterface(context, this), "Android")

            loadUrl("file:///android_asset/pages/index.html")
        }
    }
}
