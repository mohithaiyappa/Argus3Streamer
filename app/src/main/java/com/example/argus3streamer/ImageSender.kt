package com.example.argus3streamer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class ImageSender(
    val groupOwnerAddress: String,
    val groupOwnerPort: Int
) {

    fun send(imageAsByteArray: ByteArray) {
        val host: String = groupOwnerAddress
        val port: Int = groupOwnerPort
        val socket = Socket()


        var outputStream: OutputStream? = null

        try {

            Log.d(TAG, "sendSomething: ")
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port), 500)

            outputStream = socket.getOutputStream()
            outputStream.write(imageAsByteArray)
            outputStream.flush()

            Log.d(TAG, " sent image successfully")

        } catch (e: Exception) {
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

    }
}
