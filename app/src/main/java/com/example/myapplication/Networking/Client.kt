package com.example.myapplication.Networking

interface Client {
    fun sendMessage(message: String, host: String, port: Int)
}