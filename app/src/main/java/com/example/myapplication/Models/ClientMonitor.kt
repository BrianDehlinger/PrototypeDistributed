package com.example.myapplication.Models

import android.accounts.NetworkErrorException
import android.net.Network
import com.example.myapplication.Networking.NetworkInformation
import java.util.concurrent.ConcurrentHashMap

class ClientMonitor(clients: ArrayList<NetworkInformation> = arrayListOf()) {
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

    fun getClients(): List<NetworkInformation> {
        println(clientTracker)
        if (clientTracker?.keys()?.toList() != null) {
            return clientTracker?.keys!!.toList()
        } else {
            return listOf<NetworkInformation>()
        }
    }

    fun getClient(client: NetworkInformation): PeerStatus?{
        return clientTracker!![client]
    }

    override fun toString(): String {
        return clientTracker.toString()
    }
}