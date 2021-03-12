package com.example.photo_sender_stable.webrtc

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SdpObserverImpl : SdpObserver {
    private val tag = "sdpObserverImpl"


    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d(tag, "onCreateSuccess")
    }

    override fun onSetSuccess() {
        Log.d(tag, "onSetSuccess")
    }

    override fun onCreateFailure(p0: String?) {
        Log.d(tag, "onCreateFailure")
    }

    override fun onSetFailure(p0: String?) {
        Log.d(tag, "onSetFailure: $p0")
    }
}