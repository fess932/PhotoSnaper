package com.example.photosender

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

//  my
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val localIP = getIpv4HostAddress()
        Log.d("WIFI", "ip adress: $localIP")

        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        setContent {
            MaterialTheme {
                Column {
                    CameraPreview(localIP)

                    Spacer(Modifier.preferredSize(16.dp))

                    Text("bla")
                }
            }
        }
    }

//    fun getIpv4HostAddress(): String {
//        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
//            networkInterface.inetAddresses?.toList()?.find {
//                !it.isLoopbackAddress && it is Inet4Address
//            }?.let { return it.hostAddress }
//        }
//        return ""
//    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat
            .checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET)
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
}

fun getIpv4HostAddress(): String {
    NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
        networkInterface.inetAddresses?.toList()?.find {
            !it.isLoopbackAddress && it is Inet4Address
        }?.let { return it.hostAddress }
    }
    return ""
}

@Composable
fun CameraPreview(localIP: String) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


    val previewView = remember {
        PreviewView(context).apply {
            id = R.id.previewView
        }
    }

    AndroidView(
        viewBlock = { previewView },
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }


            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("CAMERA PREVIEW", "use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Spacer(Modifier.preferredSize(16.dp))

    Button(onClick = {
        takePhoto(
            context,
            cameraExecutor,
            cameraProviderFuture.get(),
            lifecycleOwner,
            cameraSelector,
            localIP
        )
    }) {
        Text("take photo")
    }


    val client = OkHttpClient.Builder().build()
    val wsListener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d("WS", "message $text")
            if (text == "snap") {
                Log.d("WS", "make snap!!!")
                snap()
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d("WS", "on open")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e("WS", "error: ", t)
        }


        private fun snap() {
            Log.d("WS", "photo snap!")
            val imageCapture = ImageCapture.Builder().build()

            try {
                cameraProviderFuture.get().unbindAll()
                cameraProviderFuture.get().bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
            } catch (exc: Exception) {
                Log.e("CAMERA PREVIEW", "use case binding failed", exc)
            }

            imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("TAKE_PHOTO", "err take photo", exception)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val cl = OkHttpClient()
                    val mediaTypeJPG = "image/jpeg".toMediaType()

                    Log.d("TAKE_PHOTO", "image info: ${image.imageInfo}")


                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }

                    Log.d("TAKE_PHOTO", "$buffer")
                    Log.d("TAKE_PHOTO", "$bytes")
                    Log.d("TAKE_PHOTO", "photo taked, ${image.format}, ${image.imageInfo}")


                    val f = File.createTempFile("file0", "file0End")
                    f.writeBytes(bytes)

                    val host = when (localIP) {
                        "10.51.1.110" -> "http://10.51.1.38:5000/"
                        "10.0.2.16" -> "http://10.0.2.2:5000/"
                        else -> "null"
                    }

                    val request = Request.Builder()
                        .url(host)
                        .post(f.asRequestBody(mediaTypeJPG))
                        .build()

                    try {
                        cl.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")

                            println(response.body!!.string())
                        }
                    } catch (exc: Exception) {
                        Log.e("WIFI", "network exception", exc)
                    }
                }
            })
        }
    }


    val ws = client.newWebSocket(
        Request.Builder().url("http://10.0.2.2:5000/ws").build(),
        wsListener
    )


}

fun takePhoto(
    context: Context,
    executor: ExecutorService,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    localIP: String
) {

    Toast.makeText(context, "photo", Toast.LENGTH_SHORT).show()
    val imageCapture = ImageCapture.Builder().build()


    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
    } catch (exc: Exception) {
        Log.e("CAMERA PREVIEW", "use case binding failed", exc)
    }

    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onError(exception: ImageCaptureException) {
            Log.e("TAKE_PHOTO", "err take photo", exception)
        }

        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)

            val client = OkHttpClient()
            val mediaTypeJPG = "image/jpeg".toMediaType()

            Log.d("TAKE_PHOTO", "image info: ${image.imageInfo}")


            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }

            Log.d("TAKE_PHOTO", "$buffer")
            Log.d("TAKE_PHOTO", "$bytes")
            Log.d("TAKE_PHOTO", "photo taked, ${image.format}, ${image.imageInfo}")


            val f = File.createTempFile("file0", "file0End")
            f.writeBytes(bytes)

            val host = when (localIP) {
                "10.51.1.110" -> "http://10.51.1.38:5000/"
                "10.0.2.16" -> "http://10.0.2.2:5000/"
                else -> "null"
            }

            val request = Request.Builder()
                .url(host)
                .post(f.asRequestBody(mediaTypeJPG))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    println(response.body!!.string())
                }
            } catch (exc: Exception) {
                Log.e("WIFI", "network exception", exc)
            }

        }
    })
}


class WSListener(
    private val context: Context,
    private val executor: ExecutorService,
    private val cameraProvider: ProcessCameraProvider,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraSelector: CameraSelector,
    val localIP: String
) : WebSocketListener() {


    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        Log.d("WS", "message $text")
        if (text == "snap") {
            Log.d("WS", "make snap!!!")
            snap()
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.d("WS", "on open")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        Log.e("WS", "error: ", t)
    }


    private fun snap() {
        Log.d("WS", "photo snap!")
        val imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
        } catch (exc: Exception) {
            Log.e("CAMERA PREVIEW", "use case binding failed", exc)
        }

        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onError(exception: ImageCaptureException) {
                Log.e("TAKE_PHOTO", "err take photo", exception)
            }

            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)

                val client = OkHttpClient()
                val mediaTypeJPG = "image/jpeg".toMediaType()

                Log.d("TAKE_PHOTO", "image info: ${image.imageInfo}")


                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }

                Log.d("TAKE_PHOTO", "$buffer")
                Log.d("TAKE_PHOTO", "$bytes")
                Log.d("TAKE_PHOTO", "photo taked, ${image.format}, ${image.imageInfo}")


                val f = File.createTempFile("file0", "file0End")
                f.writeBytes(bytes)

                val host = when (localIP) {
                    "10.51.1.110" -> "http://10.51.1.38:5000/"
                    "10.0.2.16" -> "http://10.0.2.2:5000/"
                    else -> "null"
                }

                val request = Request.Builder()
                    .url(host)
                    .post(f.asRequestBody(mediaTypeJPG))
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        println(response.body!!.string())
                    }
                } catch (exc: Exception) {
                    Log.e("WIFI", "network exception", exc)
                }

            }
        })
    }
}


