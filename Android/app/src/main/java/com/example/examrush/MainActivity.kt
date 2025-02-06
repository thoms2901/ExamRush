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
                            // Abilita JavaScript e altre impostazioni
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.setAllowUniversalAccessFromFileURLs(true)

                            // Configura la WebViewClient per mantenere la navigazione all'interno della WebView
                            webViewClient = WebViewClient()
                            loadUrl("file:///android_asset/pages/index.html") // Carica il file HTML

                            // Aggiungi l'interfaccia per JavaScript
                            val jsInterface = JavaScriptInterface(context)
                            addJavascriptInterface(jsInterface, "AndroidInterface") // Interfaccia tra JavaScript e Kotlin
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
