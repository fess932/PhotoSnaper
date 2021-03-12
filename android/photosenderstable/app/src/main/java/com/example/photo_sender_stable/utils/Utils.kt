package com.example.photo_sender_stable.utils

import android.content.Context
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService

fun getWifiIp(context: Context): String {
    return context.getSystemService<WifiManager>().let {
        when {
            it == null -> "No wifi avalible"
            !it.isWifiEnabled -> "Wifi is disabled"
            it.connectionInfo == null -> "Wifi not connected"
            else -> {
                val ip = it.connectionInfo.ipAddress
                ((ip and 0xFF).toString() + "." + (ip shr 8 and 0xFF) + "." + (ip shr 16 and 0xFF) + "." + (ip shr 24 and 0xFF))
            }
        }
    }
}

fun getHostIP(localIP: String): String {
    return when (localIP) {
        "10.51.1.50","10.51.1.65", "10.51.2.78", "10.51.1.68"  -> "10.51.1.67:5000"
        "10.0.2.16" -> "10.0.2.2:5000"
        "192.168.1.109" -> "192.168.1.135:5000"
        else -> "null"
    }
}