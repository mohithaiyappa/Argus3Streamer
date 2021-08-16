package com.example.argus3streamer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class StreamActivity : AppCompatActivity() {

    /*
    private val Context.executor: Executor
        get() = ContextCompat.getMainExecutor(this)

     */

    private var imageView: ImageView? = null

    private val executor: Executor = Executors.newFixedThreadPool(1)

    private var channel: WifiP2pManager.Channel? = null

    private var imageSender: ImageSender? = null


    private val onImageReceived: (Bitmap) -> Unit =  { receivedBitmap ->
        runOnUiThread {
            imageView?.setImageBitmap(receivedBitmap)
        }
    }

    private val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { wifiP2pInfo ->

        imageSender = ImageSender(
            groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.hostAddress,
            groupOwnerPort = 1212
        )

        startImageStreamer()
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peers: WifiP2pDeviceList? ->
        peers?.deviceList?.forEach { wifiP2pDevice ->

            Log.d(TAG,"WiFi P2P Device : ${wifiP2pDevice.deviceName} ${wifiP2pDevice.deviceAddress} ")
            Log.d(TAG,"device : $wifiP2pDevice")
            if (wifiP2pDevice.isGroupOwner) {
                Log.d(TAG,"StreamActivity : is Group Owner")
            } else {
                Log.d(TAG,"StreamActivity: not Group Owner")
            }

            manager?.requestConnectionInfo(channel, connectionInfoListener)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        imageView = findViewById(R.id.imageView)

        channel = manager?.initialize(this, mainLooper, null)
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG,"discoverPeers : onSuccess: ")
                manager?.requestPeers(channel!!, peerListListener)
            }

            override fun onFailure(reasonCode: Int) {
                Log.d(TAG,"discoverPeers : onFailure: $reasonCode")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


    private fun startImageStreamer() {

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .setTargetResolution(Size(720,1280))
            .build()
        imageAnalysis.setAnalyzer( executor, ImageAnalyser(imageSender, onImageReceived ) )

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val cameraProvider = ProcessCameraProvider.getInstance(this).get()

        val camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis
        )
    }

    private fun mapYuvImageToBitmap(pair: Pair<YuvImage,Int>): Observable<Bitmap> {
        val yuvImage = pair.first
        val rotation = pair.second
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        val sourceBitmap =  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        val bitmap = Bitmap.createBitmap(sourceBitmap,0,0,sourceBitmap.width,sourceBitmap.height,matrix,true)
        return Observable.just(bitmap)
    }

    private fun updateUi(bitmap: Bitmap) : Observable<Bitmap> {
        imageView?.setImageBitmap(bitmap)

        return Observable.just(bitmap)
    }
    private fun mapBitmapToByteArray(bitmap: Bitmap): Observable<ByteArray> {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        return Observable.just(byteArray)
    }
}
