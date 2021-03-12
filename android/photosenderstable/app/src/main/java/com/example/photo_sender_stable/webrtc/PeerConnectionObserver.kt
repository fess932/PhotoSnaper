package com.example.photo_sender_stable.webrtc

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

open class PeerConnectionObserver: PeerConnection.Observer {
    private val tag = "peerConnectionObserver"

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.d(tag, "onSignalingChange, ${p0?.name}")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d(tag, "onIceConnectionChange")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d(tag, "onIceConnectionReceivingChange")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.d(tag, "onIceGatheringChange")
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        Log.d(tag, "onIceCandidate")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.d(tag, "onIceCandidatesRemoved")
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.d(tag, "onAddStream")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.d(tag, "onRemoveStream")
    }

    override fun onDataChannel(p0: DataChannel?) {
        Log.d(tag, "onDataChannel")
    }

    override fun onRenegotiationNeeded() {
        Log.d(tag, "onRenegotiationNeeded")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.d(tag, "onAddTrack")
    }
}