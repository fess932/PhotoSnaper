package com.example.photo_sender_stable.trash.web_rtc

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class AppSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d("app sdp observer", "success create $p0")
    }

    override fun onSetSuccess() {
        Log.d("app sdp observer", "success set")

    }

    override fun onCreateFailure(p0: String?) {
        Log.d("app sdp observer", "failture create $p0")

    }

    override fun onSetFailure(p0: String?) {
        Log.d("app sdp observer", "failture set $p0")

    }
}