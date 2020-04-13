package com.example.myapplication.Networking

import android.content.Context
import android.net.NetworkInfo
import com.example.myapplication.MainActivity

class DebugProviders(
    context: Context
) {
    fun provideRingLeader(): NetworkInformation {
        return NetworkInformation("10.0.2.2", 5023, "server")
    }

    fun provideIsReplica(context: Context): Boolean {
        return NetworkInformation.getNetworkInfo(context).ip == "10.0.2.16"
    }

    fun provideOtherReplicas(context: Context): MutableList<NetworkInformation> {
        val listOfNetworkInformation = mutableListOf<NetworkInformation>()
        if (NetworkInformation.getNetworkInfo(context).ip != "10.0.2.16"){
            listOfNetworkInformation.add(NetworkInformation("10.0.2.2", 5000, "replica"))
        }
        return listOfNetworkInformation
    }

    fun providePeerId(context: Context): Int {
        if (NetworkInformation.getNetworkInfo(context).ip == "10.0.2.18") {
            return 2
        } else if (NetworkInformation.getNetworkInfo(context).ip == "10.0.2.16") {
            return 1
        } else {
            return 0
        }
    }

    fun provideNetworkInformation(context: Context): NetworkInformation {
        if (NetworkInformation.getNetworkInfo(context).ip == "10.0.2.18") {
            return NetworkInformation("10.0.2.2", 5023, "server")
        } else if (NetworkInformation.getNetworkInfo(context).ip == "10.0.2.16") {
            return NetworkInformation("10.0.2.2", 5000, "replica")
        } else {
            return NetworkInformation("10.0.2.2", 6000, "client")
        }
    }

    fun provideServerPort(context: Context): Int {
        if (NetworkInformation.getNetworkInfo(context).ip == "10.0.2.18") {
            return 5023
        } else if (NetworkInformation.getNetworkInfo(context).ip == "10.0.2.16") {
            return 5000
        } else {
            return 6000
        }
    }
}