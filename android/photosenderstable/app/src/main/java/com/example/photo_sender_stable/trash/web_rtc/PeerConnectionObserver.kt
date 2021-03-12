package com.example.photo_sender_stable.trash.web_rtc

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

open class PeerConnectionObserver: PeerConnection.Observer {
    private val tag = "peerConnectionObserver"

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.d(tag, "onSignalingChange")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onDataChannel(p0: DataChannel?) {
        Log.d(tag, "onSignalingChange")

        TODO("Not yet implemented")
    }

    override fun onRenegotiationNeeded() {
        TODO("Not yet implemented")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        TODO("Not yet implemented")
    }
}