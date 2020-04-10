package com.example.quizlash.service.network

import java.io.DataOutputStream
import java.net.Socket

class TCPClient: Client{

    override fun sendMessage(message: String, port: Int, host: String) {
        println("Message: $message sent to $host at port $port")
        var outputStream: DataOutputStream? = null
        var outputSocket: Socket? = null
        try {
            outputSocket = Socket(host, port)
            outputStream = DataOutputStream(outputSocket.getOutputStream())
            val data = message.toByteArray(Charsets.UTF_8)
            outputStream.write(data)
        } catch (e: Exception) {
            println(e.toString())
            e.printStackTrace()
        }
        finally {
            outputStream?.close()
            outputSocket?.close()
        }
    }
}