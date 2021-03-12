package com.example.photo_sender_stable.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalingClientListener {
    fun onConnectionEstablished()
    fun onOfferReceived(description: SessionDescription)
    fun onIceCandidateReceived(iceCandidate: IceCandidate)
}