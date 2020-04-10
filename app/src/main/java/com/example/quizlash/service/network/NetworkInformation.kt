package com.example.quizlash.service.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter

data class NetworkInformation (
    val ip: String,
    val port: Int,
    val peer_type: String
) {
    companion object NetworkInfoFactory{

        fun getNetworkInfo(context: Context): NetworkInformation {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

            return NetworkInformation(
                ip,
                6000,
                "quiz_server"
            )
        }
    }
}