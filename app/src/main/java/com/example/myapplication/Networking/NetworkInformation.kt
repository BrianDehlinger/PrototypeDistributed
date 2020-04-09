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

        fun getNetworkInfo(context: Context, port: Int = 5000, type: String = "client", debugUse: Boolean = false): NetworkInformation{
            var peer_type = type
            var setPort = port
            val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
            var ip = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            if (debugUse) {
                if (ip == "10.0.2.18") {
                    peer_type = "server"
                    setPort = 5023
                    ip = "10.0.2.2"
                }
                if (ip == "10.0.2.16"){
                    peer_type = "client"
                    setPort = 5000
                    ip = "10.0.2.2"
                }
            }

            return NetworkInformation(ip, port = setPort, peer_type = peer_type)
        }
    }
}