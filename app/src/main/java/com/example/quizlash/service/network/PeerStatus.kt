package com.example.quizlash.service.network

import java.util.concurrent.atomic.AtomicInteger

data class PeerStatus(
    var color: String = "green",
    var last_received: AtomicInteger = AtomicInteger(0),
    var other_client_failure_count: AtomicInteger = AtomicInteger(0)
)