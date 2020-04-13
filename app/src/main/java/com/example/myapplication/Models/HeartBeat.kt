package com.example.myapplication.Models

data class HeartBeat(
    val type: String = "hb",
    val ip: String,
    val port: Int,
    val peer_type: String
)