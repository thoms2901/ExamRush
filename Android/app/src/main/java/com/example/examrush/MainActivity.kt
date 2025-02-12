package com.example.examrush

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var webView: WebView



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
            webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            webViewClient = WebViewClient()

            addJavascriptInterface(JavaScriptInterface(context, this), "Android")

            loadUrl("file:///android_asset/pages/index.html")
        }
        return webView
    }

    // Handle the result from the gallery pick
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JavaScriptInterface.IMAGE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let {
                val imagePath = it.toString() // You can also get the path here
                Log.d("Image Path", imagePath )

                sendImageToWebView(imagePath)
            }
        }
    }

    // Send the selected image URL to WebView
    private fun sendImageToWebView(imagePath: String) {
        webView.post {
            try {
                webView.evaluateJavascript("onImagePicked('$imagePath')", null)
            } catch (e: Exception) {
                Log.e("Image", "Errore nel passare l'immagine alla WebView: ${e.message}")
            }
        }
    }


}
