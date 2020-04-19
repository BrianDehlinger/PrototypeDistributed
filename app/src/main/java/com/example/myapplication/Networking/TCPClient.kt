package com.example.myapplication.Networking

import com.example.myapplication.Models.ElectionNotification
import com.example.myapplication.Models.MultipleChoiceQuestion1
import com.example.myapplication.Models.MultipleChoiceResponse
import com.example.myapplication.Models.NewServerNotification
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

class TCPClient: Client{

    override fun sendMessage(message: String, host: String, port: Int) {
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
            outputSocket?.close()
            outputStream?.close()
        }
    }

    override fun activateQuestion(
        instructorUserName: String,
        host: String,
        port: Int,
        questionToActivate: MultipleChoiceQuestion1
    ) {
        println("PASS")
    }

    override fun propagateMultipleChoiceResponse(
        toString: String,
        ip: String,
        port: Int,
        multipleChoiceResponse: MultipleChoiceResponse
    ) {
        TODO("Not yet implemented")
    }

    override fun propagateNewElectionNotification(
        electionNotification: ElectionNotification,
        ip: String,
        port: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun propagateNewServerNotification(
        newServerNotification: NewServerNotification,
        ip: String,
        port: Int
    ) {
        TODO("Not yet implemented")
    }
}