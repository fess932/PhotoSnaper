package com.example.photo_sender_stable.trash.webRTC

import android.content.Context
import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRTCClient(private val context: Context, private val listener: IStateChangeListener) {
    private lateinit var peerConnection: PeerConnection
    private var peerConnectionInitialized: Boolean = false
    var channel: DataChannel? = null

    enum class State {
        /**
         * Initialization in progress.
         */
        INITIALIZING,

        /**
         * App is waiting for offer, fill in the offer into the edit text.
         */
        WAITING_FOR_OFFER,

        /**
         * App is creating the offer.
         */
        CREATING_OFFER,

        /**
         * App is creating answer to offer.
         */
        CREATING_ANSWER,

        /**
         * App created the offer and is now waiting for answer
         */
        WAITING_FOR_ANSWER,

        /**
         * Waiting for establishing the connection.
         */
        WAITING_TO_CONNECT,

        /**
         * Connection was established. You can chat now.
         */
        CHAT_ESTABLISHED,

        /**
         * Connection is terminated chat ended.
         */
        CHAT_ENDED
    }

    private var state: State = State.INITIALIZING
        private set(value) {
            field = value
            listener.onStateChanged(value)
        }

    interface IStateChangeListener {
        fun onStateChanged(state: State)
    }

    private lateinit var peerConnectionFactory: PeerConnectionFactory

    val peerConnectionConstraints = object : MediaConstraints() {
        init {
            optional.add(KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }
    }

    abstract inner class DefaultObserver : PeerConnection.Observer {
        override fun onDataChannel(p0: DataChannel?) {
            Log.d("default observer", "data channel ${p0?.label()} established")
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Log.d("default observer", "data channel $p0 established")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Log.d("default observer", ("ice connection state change:${p0?.name}"))

            if (p0 == PeerConnection.IceConnectionState.DISCONNECTED) {
                Log.d("default observer", ("closing channel"))
                channel?.close()
            }
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Log.d("default observer", ("onIceGatheringChange :${p0?.name}"))
        }

        override fun onAddStream(p0: MediaStream?) {
            Log.d("default observer", ("onAddStream"))
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Log.d("default observer", "onSignalingChange :${p0?.name}")
        }

        override fun onRemoveStream(p0: MediaStream?) {
        }

        override fun onRenegotiationNeeded() {
            Log.d("default observer", "onRenegotiationNeeded")
        }
    }

    open inner class DefaultSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {
            Log.d("sdp observer", "onCreateSuccess")
        }

        override fun onCreateFailure(p0: String?) {
            Log.d("sdp observer", "failed to create offer:$p0")
        }

        override fun onSetFailure(p0: String?) {
            Log.d("sdp observer", "onSetFailure $p0")
        }

        override fun onSetSuccess() {
            Log.d("sdp observer", "onSetSuccess")
        }
    }

    open inner class DefaultDataChannelObserver(val channel: DataChannel) : DataChannel.Observer {
        override fun onBufferedAmountChange(p0: Long) {
            TODO("Not yet implemented")
        }

        override fun onStateChange() {
            TODO("Not yet implemented")
        }

        override fun onMessage(p0: DataChannel.Buffer?) {
            Log.d("data channel observer", "on message: $p0")
        }
    }

    fun init() {
        val initializeOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializeOptions)
        val options = PeerConnectionFactory.Options()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
        state = State.INITIALIZING
    }

    fun destroy() {
        channel?.close()
        if (peerConnectionInitialized) {
            peerConnection.close()
        }
    }
}