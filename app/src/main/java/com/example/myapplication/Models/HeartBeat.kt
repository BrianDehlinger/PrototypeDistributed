package com.example.myapplication.Models

data class HeartBeat(
    val type: String = "hb",
    val ip: String,
    val port: String,
    val userType: UserType,
    val userName: String,
    val userId: Double
)