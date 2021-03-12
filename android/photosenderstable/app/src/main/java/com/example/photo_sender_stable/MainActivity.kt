package com.example.photo_sender_stable

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.MutableLiveData
import com.example.photo_sender_stable.databinding.ActivityMainBinding
import com.example.photo_sender_stable.utils.SufaceProviderImpl
import com.example.photo_sender_stable.webrtc.WebRTCClient
import com.example.photo_sender_stable.websocket.WSClient
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/*
Main activity
Camera

WebSocket <-> WebRTC <- Camera.VideoStream
WebSocket.Snap -> Camera
http.photo <- Camera

 */



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private val optionsQR = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        private val scanner = BarcodeScanning.getClient(optionsQR)

        private var imageCapture: ImageCapture? = null
        private lateinit var cameraExecutor: ExecutorService

        val host = MutableLiveData("")
        val client = OkHttpClient().newBuilder().writeTimeout(30, TimeUnit.SECONDS).build()

        private const val TAG = "CameraXBasic"

        // private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE
            )

        private lateinit var webrtcClient: WebRTCClient
        private lateinit var wsClient: WSClient

        // private val sdpObserver = object : AppSdpObserver() {
        //     override fun onCreateSuccess(p0: SessionDescription?) {
        //         super.onCreateSuccess(p0)
        //         Log.d("sdp observer", "on create success $p0")
        //         // wsClient.send(p0)
        //     }
        // }

        // private fun createSignalingClientListener() = object : SignalingClientListener {
        //     override fun onConnectionEstablished() {
        //         Log.d("signaling client", "connection established")
        //     }
        //
        //     override fun onOfferReceived(description: SessionDescription) {
        //         Log.d("signaling client", "message answer $description ::: $sdpObserver")
        //         webrtcClient.onRemoteSessionReceived(description)
        //         webrtcClient.answer(sdpObserver)
        //     }
        //
        //     override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        //         rtcClient.addIceCandidate(iceCandidate)
        //     }
        // }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (allPermissionsGranted()) {
            onCameraPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun onCameraPermissionGranted() {
        wsClient = WSClient()
        host.observe(this, { host ->
            binding.host.text = host
            wsClient.updateHost(host)
        })

        webrtcClient = WebRTCClient(application, wsClient)


        // webrtcClient.startLocalVideoCapture(binding.viewFinder)

        wsClient.updateWebRTCClient(webrtcClient)//todo: update rtc client inside rtc


        // webRTCClient = WebRTCClient(applicationContext, object : WebRTCClient.IStateChangeListener{
        //     override fun onStateChanged(state: WebRTCClient.State) {
        //         Log.d("state change listener", "state changed $state")
        //     }
        // })

        startCamera()
        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }
    }

    //// utils
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d("PERMISSIONS", "Permissions granted")
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // val sur = binding.viewFinder.surfaceProvider as SufaceProviderImpl

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor, { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            barcodes.forEach { bar ->
                                host.value = bar.rawValue
                                Log.d("BARCODE", "barcodes: ${bar.rawValue}")
                            }
                        }
                        .addOnFailureListener { exc -> Log.d("BARCODE_EXC", "exc: $exc") }
                        .addOnCompleteListener { imageProxy.close() }
                }
            })

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer,
                )
            } catch (exc: Exception) {
                Log.e(TAG, "use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onError(exception: ImageCaptureException) {
                Log.e("TAKE_PHOTO", "err take photo", exception)
            }

            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                Log.d("TAKE_PHOTO", "photo taked, format: ${image.format}, ${image.imageInfo}")
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
                Log.d("TAKE_PHOTO", "image bytes size: ${bytes.size}")
                image.close()

                /// sending

                val mediaTypeJPG = "image/jpeg".toMediaType()
                val f = File.createTempFile("file0", "file1")
                f.deleteOnExit()
                f.writeBytes(bytes)

                val rb = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", "Image from android")
                    .addFormDataPart("file", "photo.jpg", f.asRequestBody(mediaTypeJPG))
                    .build()


                println(host.value)
                val request = Request.Builder()
                    .url("${host.value}/upload")
                    .post(rb)
                    // .post(f.asRequestBody(mediaTypeJPG))
                    .build()

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            println(response.body!!.string())
                        }
                    } catch (exc: IOException) {
                        Log.e("WIFI", "network exception, $exc", exc)
                    }
                }
            }
        })
    }
}



