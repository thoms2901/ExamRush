package com.example.examrush

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.examrush.ui.theme.ExamRushTheme
import androidx.compose.ui.viewinterop.AndroidView



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExamRushTheme {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            webViewClient = WebViewClient() // Mantiene la navigazione in WebView
                            loadUrl("file:///android_asset/pages/index.html") // Carica una pagina di test
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
