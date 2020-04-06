package com.example.myapplication.Networking

import com.example.myapplication.Models.MultipleChoiceQuestion1

interface Client {
    fun sendMessage(message: String, host: String, port: Int)
    fun activateQuestion(instructorUserName: String, host: String, port: Int, questionToActivate: MultipleChoiceQuestion1)
}