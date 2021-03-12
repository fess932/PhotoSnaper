package com.example.photo_sender_stable.webrtc

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.camera.view.PreviewView
import com.example.photo_sender_stable.models.MTYPE_ANSWER
import com.example.photo_sender_stable.models.Messasge
import com.example.photo_sender_stable.websocket.WSClient
import org.json.JSONObject
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaSource
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource

class WebRTCClient(
    ctx: Application,
    private val wsClient: WSClient
) {
    companion object {
        private const val LOCAL_TRACK_ID = "local_track"
        private const val LOCAL_STREAM_ID = "local_track"
    }


    private val rootEglBase = EglBase.create()

    private val peerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(ctx)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    rootEglBase.eglBaseContext,
                    true,
                    true
                )
            )
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private val peerConnectionConfiguration = PeerConnection.RTCConfiguration(listOf())

    private val peerConnectionObserver = PeerConnectionObserver()

    val peerConnection = peerConnectionFactory.createPeerConnection(
        peerConnectionConfiguration,
        peerConnectionObserver
    )

    private val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
    }

    fun handleOffer(offer: String) {

        //1
        peerConnection?.setRemoteDescription(
            object : SdpObserverImpl() {},
            SessionDescription(SessionDescription.Type.OFFER, offer)
        )
        //2
        peerConnection?.createAnswer(object : SdpObserverImpl() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.d("createAnswer", "on create success")
                peerConnection.setLocalDescription(object : SdpObserverImpl() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        if (p0 != null) {
                            wsClient.sendMessage(Messasge(MTYPE_ANSWER, rdpToJsonObject(p0)))
                        }
                    }
                }, p0)
            }
        }, constraints)

        //1 await peerConnection.SetRemoteDescription(offer)
        //2 videotracks.getTracks().forEach(track=> peerConnection.addTrack(track, videostream))
        //3 val answer = await peerConnection.createAnswer()
        //4 await peerConnection.setLocalDescription(answer)
    }

    fun handleNewICECandidate(ice: IceCandidate) {
        if (peerConnection == null) {
            Log.d("handleNewIce", "peerConnection is NULL")
            return
        }
        if (!peerConnection.addIceCandidate(ice)) {
            Log.d("handleNewIce", "wrong ice candidate!")
        }
    }

    private val videoCapturer by lazy { getVideoCapturer(ctx) }
    private fun getVideoCapturer(context: Context) = Camera2Enumerator(context).run {
        deviceNames.find {
            isFrontFacing(it)
        }?.let {
            createCapturer(it, null)
        } ?: throw IllegalStateException()
    }

    fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext, null)

    }

    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }

    // fun startLocalVideoCapture() {
    //     val videoSource = createVideoS
    //     videoCapturer.startCapture(320, 240, 30)
    //     val localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
    //     localVideoTrack.addSink(localVideoOutput)
    //     val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
    //     localStream.addTrack(localVideoTrack)
    //     peerConnection?.addStream(localStream)
    // }
}

fun rdpToJsonObject(rdp: SessionDescription): JSONObject {
    val remoteDescr = JSONObject()
    remoteDescr.put("sdp", rdp.description)
    remoteDescr.put("type", rdp.type.canonicalForm())

    val rootObj = JSONObject()
    rootObj.put("remoteDescription", remoteDescr)


    return rootObj
}


