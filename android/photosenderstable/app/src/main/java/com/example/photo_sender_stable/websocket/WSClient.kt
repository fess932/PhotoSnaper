package com.example.photo_sender_stable.websocket

import android.util.Log
import com.example.photo_sender_stable.models.ICECandidateAdapter
import com.example.photo_sender_stable.models.JSONObjectAdapter
import com.example.photo_sender_stable.models.MTYPE_ANSWER
import com.example.photo_sender_stable.models.MTYPE_CAMERA_LIST
import com.example.photo_sender_stable.models.MTYPE_MESSAGE
import com.example.photo_sender_stable.models.MTYPE_NEW_ICE_CANDIDATE
import com.example.photo_sender_stable.models.MTYPE_OFFER
import com.example.photo_sender_stable.models.MTYPE_SNAP
import com.example.photo_sender_stable.models.Messasge
import com.example.photo_sender_stable.webrtc.WebRTCClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.webrtc.IceCandidate

class WSClient {
    private val job = Job()

    var webRTCClient: WebRTCClient? = null

    private lateinit var ws: WebSocket
    private val wsListener by lazy {
        WSListener()
    }

    private var currentHost = ""
    private val client = OkHttpClient.Builder().build()

    fun updateHost(host: String) {
        Log.d("START_WS", "lel")

        if (this.currentHost == host) {
            return
        }

        if (host != this.currentHost) {
            this.currentHost = host

            ws = client.newWebSocket(
                Request.Builder()
                    .url("ws://${host}/ws?t=2") // 2 is typeCamera
                    .build(),
                wsListener
            )
        }
    }

    fun destroy() {
        ws.close(1000, "on destroy")
        job.complete()
    }

    private val moshi = Moshi.Builder()
        .add(JSONObjectAdapter)
        .add(ICECandidateAdapter)
        .build()
    private val messageJsonAdapter = moshi.adapter(Messasge::class.java)
    private val iceCandidateJsonAdapter = moshi.adapter(IceCandidate::class.java)

    inner class WSListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d("WebSocket", "on open")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e("WebSocket", "error: ", t)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.e("WebSocket", "on closed: $code, $reason")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            this.handleOnMessage(text)
        }

        private fun handleOnMessage(text: String) {
            val message = messageJsonAdapter.fromJson(text)

            // get type of message
            when (message?.type) {
                MTYPE_MESSAGE -> Log.d("message", "sample message")
                MTYPE_OFFER -> {
                    message.body?.let {
                        Log.d("message", "offer")
                        webRTCClient?.handleOffer(it.getString("sdp"))
                    }
                }
                MTYPE_ANSWER -> Log.d("message", "answer message")
                MTYPE_CAMERA_LIST -> Log.d("message", "camera list message")
                MTYPE_NEW_ICE_CANDIDATE -> {
                    message.body?.let {
                        iceCandidateJsonAdapter.fromJson(it.toString())?.let { ice ->
                            webRTCClient?.handleNewICECandidate(ice)
                        }
                    }
                }
                MTYPE_SNAP -> Log.d("message", "snap message, needs snap and send photo")
                else -> Log.d("message", "not correct type message $message")
            }
        }
    }

    fun sendMessage(m: Messasge) {
        try {
            val msg = messageJsonAdapter.toJson(m)
            Log.d("send_message", msg)
            ws.send(msg)
        } catch (exc: Exception) {
            Log.e("send_message", "exception: $exc")
        }
    }

    fun updateWebRTCClient(wrc: WebRTCClient) {
        Log.d("updatertc", "$wrc ${wrc.peerConnection}")
        this.webRTCClient = wrc
        Log.d("updatertc", "${this.webRTCClient}")
    }
}




