package com.example.myapplication.Networking

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService

class TCPServer : Server{

    private val listeners = mutableListOf<DataListener>()
    private var port = 6000
    var serverSocket: ServerSocket? = null;
    var socket: Socket? = null
    var threadExService: ExecutorService? = null

    fun setPort(port_to_set: Int){
        port = port_to_set
    }

    override fun listenForPackets(port: Int) {
        println(port)
        serverSocket = ServerSocket(port)
        while (true) {
            try {
                socket = serverSocket!!.accept()
            } catch (e: Exception) {
                println(e.toString())
                e.printStackTrace()
            }
            if (threadExService != null){
                threadExService?.execute(serverThread(socket as Socket))
            }
            else {
                serverThread(socket as Socket).start()
            }
        }
    }

    fun setThreadPool(executorService: ExecutorService){
        threadExService = executorService
    }

    inner class serverThread(val clientSocket: Socket): Thread() {
        override fun run() {
            var data: String? = null
            val inputStream: InputStream
            val bufferedReader: BufferedReader
            try {
                inputStream = clientSocket.getInputStream()
                bufferedReader = BufferedReader(InputStreamReader(inputStream))
                data = bufferedReader.readLine()

                // Prevents a weird error. This needs to be troubleshooted.
                if (data.takeLast(1) != "}"){
                    println("THERE IS NO }!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    data = "$data}"
                }
                inputStream.close()
                clientSocket.close()
            } catch (e: Exception) {
                println(e.toString())
                e.printStackTrace()
            }
            for (listener in listeners) {
                if (data != null){
                    listener.onData(data)
                }
            }
        }
    }

    override fun run() {
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
