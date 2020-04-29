package com.example.myapplication.Networking

import com.example.myapplication.Models.ElectionNotification
import com.example.myapplication.Models.MultipleChoiceQuestion1
import com.example.myapplication.Models.MultipleChoiceResponse
import com.example.myapplication.Models.NewServerNotification

interface Client {
    fun sendMessage(message: String, host: String, port: Int)
    fun activateQuestion(instructorUserName: String, host: String, port: Int, questionToActivate: MultipleChoiceQuestion1)
    fun propagateMultipleChoiceResponse(toString: String, ip: String, port: Int, multipleChoiceResponse: MultipleChoiceResponse)
    fun propagateNewElectionNotification(electionNotification: ElectionNotification, ip: String, port: Int)
    fun propagateNewServerNotification(newServerNotification: NewServerNotification, ip:String, port: Int)
}