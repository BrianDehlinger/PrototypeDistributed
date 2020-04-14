package com.example.myapplication.Models

import com.example.myapplication.Networking.NetworkInformation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

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

    fun getClients(): List<NetworkInformation> {
        if (clientTracker?.keys()?.toList() != null) {
            return clientTracker?.keys!!.toList()
        } else {
            return listOf<NetworkInformation>()
        }
    }

    fun getClient(client: NetworkInformation): PeerStatus?{
        return clientTracker!![client]
    }

    fun removeClient(client: NetworkInformation){
        clientTracker?.remove(client)
    }

    override fun toString(): String {
        return clientTracker.toString()
    }

    fun setNewClients(listOfReplicas: CopyOnWriteArrayList<NetworkInformation>){
        clientTracker?.clear()
        for (replica in listOfReplicas){
            addClient(replica)
        }
    }
}