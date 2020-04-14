package com.example.myapplication.Networking

import android.content.Context
import android.net.wifi.WifiManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPClient: Client {

    //For logging relevant events
    private final var log: Logger = LoggerFactory.getLogger(UDPClient::class.java)


    override fun sendMessage(message: String, host: String, port: Int) {
        val socket = DatagramSocket()
        println("Message: $message sent to $host at port $port")
        println("THIS IS SENDING JUST FINE")
        val data = message.toByteArray(Charsets.UTF_8)
        try {
            val packet = DatagramPacket(data, data.size, InetAddress.getByName(host), port)
            socket.send(packet)
        } catch (e: Exception) {
            println(e.toString())
            e.printStackTrace()
        }
    }

    fun broadcast(message: String, port: Int, context: Context) {
        println("Broadcasting")
        val socket = DatagramSocket()
        socket.broadcast = true
        val data = message.toByteArray(Charsets.UTF_8)
        try {
            println("SENDING PACKET")
            val packet = DatagramPacket(data, data.size, InetAddress.getByName("10.0.2.255"), port)
            socket.send(packet)
            println("PACKET SENT")
        } catch (e: Exception) {
            println(e.toString())
            e.printStackTrace()
        }

    }
}

