package com.example.quizlash.service.network

import java.net.Socket

interface Client {
    fun sendMessage(message: String, port: Int, host: String)
}