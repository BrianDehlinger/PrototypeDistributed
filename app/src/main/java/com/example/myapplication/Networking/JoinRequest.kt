package com.example.myapplication.Networking

data class JoinRequest (
    val information: NetworkInformation,
    val type: String = "join_request",
    val peer_type: String
)