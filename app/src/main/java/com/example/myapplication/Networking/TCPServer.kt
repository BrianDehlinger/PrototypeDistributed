package com.example.myapplication.Networking

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.nio.Buffer

class TCPServer : Server{

    private val listeners = mutableListOf<UDPListener>()
    private var port = 6000
    var serverSocket: ServerSocket? = null;
    var socket: Socket? = null

    fun setPort(port_to_set: Int){
        port = port_to_set
    }

    override fun listenForPackets(port: Int) {
        serverSocket = ServerSocket(port)
        while (true) {
            try {
                socket = serverSocket!!.accept()
            } catch (e: Exception) {
                println(e.toString())
                e.printStackTrace()
            }
            serverThread(socket as Socket).start()
        }
    }

    inner class serverThread(val clientSocket: Socket): Thread() {
        override fun run() {
            println("HERE")
            var data: String? = null
            val inputStream: InputStream
            val bufferedReader: BufferedReader
            try {
                inputStream = clientSocket.getInputStream()
                bufferedReader = BufferedReader(InputStreamReader(inputStream))
                data = bufferedReader.readText()
                clientSocket.close()
                inputStream.close()
            } catch (e: Exception) {
                println(e.toString())
                e.printStackTrace()
            }
            for (listener in listeners) {
                if (data != null){
                    listener.onUDP(data)
                }
            }
        }
    }

    override fun run() {
        println("Listening for UDP packets")
        listenForPackets(port)
    }

    override fun addListener(listener: UDPListener){
        listeners.add(listener)
    }

    override fun removeListener(listener: UDPListener){
        listeners.remove(listener)
    }

    override fun clearListeners(){
        listeners.clear()
    }
}
