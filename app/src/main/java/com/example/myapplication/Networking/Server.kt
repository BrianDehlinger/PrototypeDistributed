package com.example.myapplication.Networking

interface Server: Runnable{

    fun listenForPackets(port: Int)

    fun addListener(listener: DataListener)

    fun removeListener(listener: DataListener)

    fun clearListeners()
}
