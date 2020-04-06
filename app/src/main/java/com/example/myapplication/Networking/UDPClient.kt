package com.example.myapplication.Networking

import com.example.myapplication.Models.MultipleChoiceQuestion1
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

    /**
     * The Server (instructor) will be the only device with the ability to activate a question.
     * When the instructor does so, this function will be invoked and will broadcast the question
     * to all of the Clients (students).
     */
    fun activateQuestion(instructorUserName: String, host: String, port: Int,
                         questionToActivate: MultipleChoiceQuestion1) {
        val socket = DatagramSocket()
        log.info("activateQuestion function received an `activateQuestion` request from: "
                + instructorUserName + " For the question: " + questionToActivate)

        val questionAsString = questionToActivate.toString()

        val questionToActivateAsByteArray = questionAsString.toByteArray(Charsets.UTF_8)

        try {
            //preparing the UDP packet for sending
            val packet = DatagramPacket(questionToActivateAsByteArray, questionToActivateAsByteArray.size, InetAddress.getByName(host), port)

            log.info("Attempting to send the packet to the following host and port: " + host + " " + port)
            socket.send(packet) //error here

            log.info("The following question has been propagated to client " + questionAsString + " " + host + " at port " + port)
        } catch (e: Exception) {
            println(e.toString())
            e.printStackTrace()
        }
    }
}

