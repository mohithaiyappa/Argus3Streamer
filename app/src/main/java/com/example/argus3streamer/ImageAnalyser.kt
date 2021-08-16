package com.example.argus3streamer

import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.reactivex.subjects.PublishSubject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class ImageAnalyser(
    val imageSender: ImageSender?,
    val onImageReceived: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {



    override fun analyze(image: ImageProxy) {
        Log.d(TAG, "analyze: imageproxy")



        Log.d(TAG, "analyze: owner ${imageSender?.groupOwnerAddress}")
        Log.d(TAG, "analyze: owner ${imageSender?.groupOwnerPort}")


        val host: String = imageSender!!.groupOwnerAddress
        val port: Int = imageSender.groupOwnerPort
        val socket = Socket()

        Log.d(TAG, "analyze: launching global scope")
        //GlobalScope.launch(Dispatchers.IO) {


        var outputStream: OutputStream? = null

        try {

            val bitmap: Bitmap = image.toBitmap()
            onImageReceived.invoke(bitmap)
            val imageAsByteArray: ByteArray = bitmap.toByteArray()

            Log.d(TAG, "sendSomething: ")
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port), 500)

            Log.d(TAG, "analyze: getting output stream")
            outputStream = socket.getOutputStream()

            Log.d(TAG, "analyze: writing output")
            outputStream.write(imageAsByteArray)
            outputStream.flush()

            Log.d(TAG, " sent image successfully")

        } catch (e: Exception) {
            Log.d(TAG, "analyze: $e")
            e.printStackTrace()
        } finally {

            /**
             * Clean up any open sockets when done
             * transferring or if an exception occurred.
             */
            Log.d(TAG, "Cleaning up")
            outputStream?.close()
            socket.takeIf { it.isConnected }?.apply {
                close()
            }
        }
        //}



        image.close()
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        recycle()
        return byteArray
    }

    private fun ImageProxy.toBitmap(): Bitmap {

        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        val sourceBitmap =  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(sourceBitmap,0,0,sourceBitmap.width,sourceBitmap.height,matrix,true)
    }

    private fun ImageProxy.toYuvImage(): YuvImage {

        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        return YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    }






}
