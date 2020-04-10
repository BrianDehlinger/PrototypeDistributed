package com.example.quizlash.service.network

data class HeartBeat(
    val type: String = "hb",
    val ip: String,
    val port: Int,
    val peerType: String
)