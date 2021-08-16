package com.example.argus3streamer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket

class ImageReceiver(
    val subject: PublishSubject<ByteArray>
) {

    var shouldReceive = false

    var job: Job? =  null

    fun startReceiver() {
        shouldReceive = true

        job = GlobalScope.launch(Dispatchers.IO) {

            val port = 1212
            val serverSocket: ServerSocket = ServerSocket(port)
            Log.d( TAG,"DataReceiver: Server: Socket opened")

            var inputStream: InputStream? = null

            while (shouldReceive) {
                try {

                    val client: Socket = serverSocket.accept()
                    Log.d(TAG,"DataReceiver: Server: connection done")

                    inputStream = client.getInputStream()
                    Log.d(TAG,"startReceiver: $inputStream")

                    subject.onNext(inputStream.readBytes())

                    //val imageBitmap: Bitmap = fromByteArrayToBitmap(inputStream)
                    //onImageReceived.invoke(imageBitmap)

                    Log.d(TAG, "Data received")
                } catch (e: Exception) {
                    Log.d(TAG,"startReceiver $e ")
                    e.printStackTrace()
                } finally {
                    inputStream?.close()
                    Log.d(TAG,"in finally block")
                }
            }

            shouldReceive = false
            serverSocket.close()
        }
    }

    fun stopReceiver() {
        job?.cancel()
        shouldReceive = false
    }

    private fun fromByteArrayToBitmap( inputStream: InputStream): Bitmap {
        return BitmapFactory.decodeStream(inputStream)
    }
}