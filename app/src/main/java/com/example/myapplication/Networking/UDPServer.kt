package com.example.myapplication.Networking

import java.lang.Exception
import java.net.*

// https://stackoverflow.com/questions/56874545/how-to-get-udp-data-constant-listening-on-kotlin
// https://stackoverflow.com/questions/19540715/send-and-receive-data-on-udp-socket-java-android
// Woodie was very helpful here. Thanks a lot

class UDPServer: Server{

    private val listeners = mutableListOf<DataListener>()

    // Can use composition by setting this to var to be more flexible.
    private var port = 6000

    fun setPort(port_to_set: Int){
        port = port_to_set
    }

    override fun listenForPackets(port: Int){
        val buffer = ByteArray(40000)
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(null)
            socket.bind(InetSocketAddress("0.0.0.0", port))
            socket.broadcast = true
            val packet = DatagramPacket(buffer, buffer.size)
            while (true) {
                socket.receive(packet)
                println("Packet received!")
                for (listener in listeners){
                    val stringOfData = String(packet.data, 0, packet.length)
                    listener.onData(stringOfData)
                }
            }
        }
        catch (e: Exception){
            println(e.toString())
            e.printStackTrace()
        }
    }

    override fun run() {
        println("Listening for UDP packets")
        listenForPackets(port)
    }

    override fun addListener(listener: DataListener){
        listeners.add(listener)
    }

    override fun removeListener(listener: DataListener){
        listeners.remove(listener)
    }

    override fun clearListeners(){
        listeners.clear()
    }
}


