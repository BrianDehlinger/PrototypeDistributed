package com.example.myapplication.Networking

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.text.format.Formatter

data class NetworkInformation (
    val ip: String,
    var port: Int,
    var peer_type: String
) {
    companion object NetworkInfoFactory{

        fun getNetworkInfo(context: Context, port: Int = 5000, type: String = "client"): NetworkInformation{
            val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

            return NetworkInformation(ip, port = port, peer_type = type)
        }
    }
}