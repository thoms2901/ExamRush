package com.example.opencvandcamera

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Point3
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.threshold
import org.opencv.imgproc.Imgproc.blur
import org.opencv.imgproc.Imgproc.*
import org.opencv.objdetect.CascadeClassifier
import org.opencv.video.BackgroundSubtractor
import java.io.File
import java.io.FileOutputStream

object ImageProcessing {

    fun BlurFilter(m: Mat) {
        val s = Size(150.0, 40.0)
        val p = Point(29.0, 39.0)
        blur(m, m, s, p, Core.BORDER_DEFAULT)
    }
    fun getChannel(m: Mat,i:Int):Mat{
        val mv = ArrayList<Mat>(4)
        Core.split(m,mv)

        return mv[i]
    }
    fun threshold(m: Mat){
        cvtColor(m,m, COLOR_BGR2GRAY)
        //threshold(m,m,150.0,250.0, THRESH_TRUNC)
        //threshold(m,m,150.0,250.0, THRESH_BINARY)
        //adaptiveThreshold(m,m,255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY,15,3.0)
        threshold(m,m,0.0,255.0, THRESH_OTSU)
    }
    fun thresholdColor(m: Mat){
        val mv = ArrayList<Mat>(4)
        Core.split(m,mv)
        threshold(mv[0],mv[0],0.0,255.0, THRESH_OTSU)
        threshold(mv[1],mv[1],0.0,255.0, THRESH_OTSU)
        threshold(mv[2],mv[2],0.0,255.0, THRESH_OTSU)
        Core.merge(mv,m)
        //threshold(m,m,150.0,250.0, THRESH_TRUNC)
        //threshold(m,m,150.0,250.0, THRESH_BINARY)
        //adaptiveThreshold(m,m,255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY,15,3.0)
        //threshold(m,m,0.0,255.0, THRESH_OTSU)
    }

    fun changeBackground(m: Mat):Mat{
        val forground = Mat()
        val back = Mat()

        Point3()
        cvtColor(m,forground, COLOR_BGR2GRAY)
        cvtColor(m,back, COLOR_BGR2GRAY)
        //Imgproc.threshold()
        threshold(back,back,100.0,250.0, THRESH_BINARY)
        threshold(forground,forground,100.0,255.0, THRESH_BINARY_INV)

        //threshold(binary,binary,150.0,255.0, THRESH_BINARY)
        val mv = ArrayList<Mat>(4)
        Core.split(m,mv)//Core.add(binary,m,binary)
        Core.bitwise_and(mv[0],forground,mv[0])
        //Core.bitwise_and(mv[1],forground,mv[1])
        //Core.bitwise_and(mv[2],forground,mv[2])
        Core.merge(mv,m)

        val background = Mat(m.size(),m.type()).also {
            it.setTo(Scalar(255.0,0.0,0.0))
        }
        Core.bitwise_and(background,back,background)
        Core.add(m,background,m)
        return m

        Core.bitwise_and(m,forground,m)
        //background.setTo(m)
    }
    fun negative(m: Mat){
        //perform negative of an image
        cvtColor(m,m, COLOR_BGR2GRAY)
        val mm = Mat(m.size(),m.type()).also {
            it.setTo(Scalar(255.0))
        }

        Core.subtract(mm,m,m)

    }
    fun add(m: Mat){
        val mm = Mat(m.size(),m.type()).also {
            it.setTo(Scalar(2.0,1.0,1.0,1.0))
        }



        //Core.add(m,mm,m)
        Core.multiply(m,mm,m)
    }
    fun and(m: Mat){

        val mm = Mat(m.size(),m.type()).also {
            //it.setTo(Scalar(0.0,0.0,255.0,1.0))
        }


        Imgproc.circle(mm,
            Point(m.size().width/2,m.size().height/2),
            (m.size().width/2).toInt(),
            Scalar(255.0,255.0,255.0,255.0),-1)

        //Core.add(m,mm,m)
        Core.bitwise_and(m,mm,m)
        //mm.copyTo(m)
    }

    fun equalizer(m: Mat){
        equalizeHist(m,m)
    }

    fun contrastStretching(m: Mat):Mat{
        cvtColor(m,m, COLOR_RGB2GRAY)
        val ba = ByteArray(1)
        for ( j in 0..m.width())
            for ( i in 0..m.height())
            {
                m.get(i,j,ba)
                var x = ba[0].toInt()
                if (x>20 && x<256){
                    x=255
                }
                ba[0]=x.toByte()
                m.put(i,j,ba)
            }
        return m
    }
}