package com.example.opencvandcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.opencvandcamera.ui.theme.OpenCVandCameraTheme
import org.opencv.android.CameraActivity

import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


import java.io.InputStream



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var msg = ""
        if (OpenCVLoader.initDebug())
            msg = "Loaded OpenCV version: "+ OpenCVLoader.OPENCV_VERSION
        else msg="Fail to load OpenCV"

        setContentView(
            mView(this)
        )
    }


}

class mView(context: Context): View(context){

    var bm : Bitmap

  init {
      //https://www.flickr.com/photos/199311433@N08/54149345537/in/photolist-2qw3PJM-2qvc7pU-2quZxTP-2qw1My2-2qvRj3i-2qs9Gku-2qtV4iP-2qteQGF-2quUp7h-2qw6n6h-2qvEDFV-2qr4WeU-2quhkqf-2qsBUXf-2quyZgC-2qu982G-2qroVjr-2qtUazm-2qsfcFM-2qsMQHB-2quET9h-2qtUTdu-2qqPvTr-2quiZEp-2qtcmDC-2quF9VP-2qrQb1n-2qrDQ17-2quKiiL-2qsvZBn-2qq6VYJ-2qvnZez-2qtVQJ3-2qrSE4N-2qvZesH-2qvaq2J-2qq8ZQQ-2qq2Be4-2qvvK7J-2qrkQrC-2qrEeJF-2qvnxFU-2qtDwhN-2qtbpE8-2qtVAAx-2qtBpLm-2qr6tVE-2qqGPBL-2qusaXk-2qvuRnQ
      val inputStream: InputStream = context.assets.open("images/child.jpg")
      bm = BitmapFactory.decodeStream(inputStream)

      inputStream.close()

      var mat = Mat()
      Utils.bitmapToMat(bm, mat)
      Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR) // Ensure 3-channel format


      // Convert to grayscale
      val grayMat = Mat()
      Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
      //Utils.matToBitmap(grayMat,bm)


      // Apply median blur to reduce noise
      val blurredMat = Mat()
      Imgproc.medianBlur(grayMat, blurredMat, 7)
      //Utils.matToBitmap(blurredMat,bm)


      // Detect edges using adaptive threshold
      val edgesMat = Mat()
      Imgproc.adaptiveThreshold(blurredMat, edgesMat,
          255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 2.0
      )

      // Reduce the color palette of the original image
      val colorMat = Mat()
      Imgproc.bilateralFilter(mat, colorMat, 9, 700.0, 700.0)

      //Utils.matToBitmap(colorMat,bm)

      // Combine the edges with the color image
      val cartoonMat = Mat()
      Core.bitwise_and(colorMat, colorMat, cartoonMat, edgesMat)

      // Convert the result back to Bitmap
      Utils.matToBitmap(cartoonMat,bm)



  }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mtx : Matrix = Matrix().also {
            it.setScale(1f*canvas.width/bm.width.toFloat(),
            canvas.height/bm.height.toFloat()) }
        canvas.drawBitmap(bm,mtx,null)
        //canvas.drawRGB(255,0,0)
        //invalidate()
    }


}