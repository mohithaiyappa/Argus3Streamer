package com.example.argus3streamer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.InputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class DisplayActivity : AppCompatActivity() {

    var imageView: ImageView? = null

    val subject: PublishSubject<ByteArray> = PublishSubject.create()

    val compositeDisposable = CompositeDisposable()

    /*
    private val onImageReceived: (Bitmap) -> Unit =  { receivedBitmap ->
        runOnUiThread {
            imageView?.setImageBitmap(receivedBitmap)
        }
    }*/

    private val imageReceiver: ImageReceiver = ImageReceiver(subject)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        imageView = findViewById(R.id.imageView)

        val dis = subject
            .debounce(100, TimeUnit.MICROSECONDS)
            .flatMap (this::mapByteArrayToBitmap)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    imageView?.setImageBitmap(it)
                },
                {
                    Log.d(TAG, "error $it")
                }
            )
        compositeDisposable.add(dis)
    }

    override fun onResume() {
        super.onResume()

        imageReceiver.startReceiver()
    }

    override fun onPause() {
        super.onPause()

        imageReceiver.stopReceiver()
    }

    override fun onDestroy() {
        imageReceiver.stopReceiver()
        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onBackPressed() {
        imageReceiver.stopReceiver()
        finish()
        super.onBackPressed()

    }

    fun mapByteArrayToBitmap(byteArray: ByteArray): Observable<Bitmap> {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return Observable.just(bitmap)
    }
}
