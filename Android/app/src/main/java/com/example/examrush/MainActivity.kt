package com.example.examrush

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.io.InputStream


class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var webView: WebView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.")
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully.")
        }

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
                val base64Image = imageProcessing(it)
                Log.d("Base64 Image", base64Image)  // Debugging: Check Base64 string
                sendImageToWebView(base64Image)
            }
        }
    }

    // Send the Base64 image to WebView
    private fun sendImageToWebView(base64Image: String) {
        webView.post {
            try {
                val safeImagePath = base64Image.replace("'", "\\'") // Escape single quotes for JS
                webView.evaluateJavascript("onImagePicked('$safeImagePath')", null)
            } catch (e: Exception) {
                Log.e("Image", "Error passing image to WebView: ${e.message}")
            }
        }
    }

    // Image processing function
    private fun imageProcessing(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Apply median blur to reduce noise
        val blurredMat = Mat()
        Imgproc.medianBlur(grayMat, blurredMat, 7)

        // Detect edges using adaptive threshold
        val edgesMat = Mat()
        Imgproc.adaptiveThreshold(
            blurredMat, edgesMat,
            255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 2.0
        )

        // Reduce color palette of the original image
        val colorMat = Mat()
        Imgproc.bilateralFilter(mat, colorMat, 9, 700.0, 700.0)

        // Combine edges with color image
        val cartoonMat = Mat()
        Core.bitwise_and(colorMat, colorMat, cartoonMat, edgesMat)

        // Convert result back to Bitmap
        val processedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cartoonMat, processedBitmap)

        return getBase64ImageSrc(processedBitmap)
    }

    // Convert Bitmap to Base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)  // FIX: Use NO_WRAP to avoid line breaks
    }

    // Generate Base64 image source for WebView
    private fun getBase64ImageSrc(bitmap: Bitmap): String {
        val base64String = bitmapToBase64(bitmap)
        return "data:image/png;base64,$base64String"
    }

}
