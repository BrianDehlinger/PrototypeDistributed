package com.example.quizlash.service.network

import java.util.concurrent.ConcurrentHashMap

class PeerMonitor(clients: ArrayList<NetworkInformation> = arrayListOf()) {
    private var clientTracker: ConcurrentHashMap<NetworkInformation, PeerStatus>? = ConcurrentHashMap()

    init {
        for (client in clients) {
            clientTracker?.put(client, PeerStatus())
        }
        println(clients)
    }

    fun addClient(client: NetworkInformation) {
        clientTracker?.put(client, PeerStatus())
    }

    fun getClient(client: NetworkInformation): PeerStatus?{
        return clientTracker!![client]
    }

    override fun toString(): String {
        return clientTracker.toString()
    }
}