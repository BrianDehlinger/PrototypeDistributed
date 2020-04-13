package com.example.myapplication.Networking

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.media.Ringtone
import android.net.Network
import android.os.Debug
import com.example.myapplication.GSONConverter
import com.example.myapplication.Models.MultipleChoiceQuestion
import com.example.myapplication.MainActivity
import com.example.myapplication.Models.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.concurrent.schedule
import kotlin.reflect.jvm.internal.impl.types.checker.NewCapturedType


open class Session(val context: Context, var RingLeader: NetworkInformation?, protected var sessionReplicas: MutableCollection<NetworkInformation> = arrayListOf(), val threadPool: ExecutorService): DataListener {

    protected val gson = Gson()
    protected val gsonConverter = GSONConverter()
    protected val client = TCPClient()
    open protected val messageHandler: MessageHandler = MessageHandler()
    var activeQuestion: MultipleChoiceQuestion? = null

    protected val networkService = TCPServer().also{
        it.addListener(this)
        val portToSet = DebugProviders(context).provideServerPort(context)
        println(portToSet)
        it.setPort(portToSet)
        it.threadExService = threadPool
    }

    var isRingLeader = false
    protected val listeningServerThread = Thread(networkService).also{
        it.start()
    }

    fun sendMessage(message: String, recipient: NetworkInformation){
        threadPool.execute(Thread(Runnable{client.sendMessage(message, port =recipient.port, host = recipient.ip)}))
    }
    open fun broadcast(message: String) {
        sendMessage(message, RingLeader!!)
        for (replica in sessionReplicas){
            sendMessage(message, replica)
        }
    }

    open fun addReplica(client: NetworkInformation){
        sessionReplicas.add(client)
    }

    override fun onData(data: String) {
        threadPool.execute(Thread(Runnable{
            messageHandler.handleMessage(data)
        }))
    }

    fun close(){
        listeningServerThread.join()
    }

    open fun onHeartBeat(heartBeat: HeartBeat){
        println("Heartbeat received")
    }

    open fun activateQuestion(question: MultipleChoiceQuestion){
        activeQuestion = question
    }

    open fun replicaFailure(failure: NetworkInformation){
        println("Failure detected")
    }

    protected open inner class MessageHandler {

        val parentContext: Context = context

        open fun handleMessage(message: String) {
            println("Message is $message")
            val type = gson.fromJson(message, Map::class.java)["type"] as String
            val instantiatedObject = gsonConverter.convertToClass(type, message)
            when (type){
                "multiple_choice_question" -> {
                    val activeQuestion = instantiatedObject as MultipleChoiceQuestion
                    activateQuestion(activeQuestion)
                }
                "hb" -> {
                    println("HEARTBEAT LOGIC")
                    val heartBeat = instantiatedObject as HeartBeat
                    onHeartBeat(heartBeat)
                }
                "failure_detected" -> {
                    println("FAILURE LOGIC")
                    val failure = instantiatedObject as NetworkInformation
                    replicaFailure(failure)
                }
                "connection_restored" -> {
                    println("Connection restored")
                }
                "new_replica" -> {
                    val replica = instantiatedObject as NewReplica
                    addReplica(replica.information)
                }
                "sync_replicas" -> {
                    val replicas = instantiatedObject as List<NetworkInformation>
                    val mutableReplicas = replicas.toMutableList()
                    val thisClient = DebugProviders(context).provideNetworkInformation(context)
                    mutableReplicas.remove(thisClient)
                    sessionReplicas = mutableReplicas
                }
            }
        }
    }
}

class ReplicaSession(context: Context, RingLeader: NetworkInformation?, sessionReplicas: MutableCollection<NetworkInformation> = arrayListOf(), threadPool: ExecutorService, var clients: MutableCollection<NetworkInformation> = arrayListOf(), val peerId: Int): Session(context, RingLeader, sessionReplicas, threadPool){

    override val messageHandler = ReplicaMessageHandler()
    private val peerMonitor: PeerMonitor = PeerMonitor().also {
        for (replica in sessionReplicas) {
            it.addClient(replica)
        }
    }
    private val bully = Bully(this)


    override fun addReplica(replica: NetworkInformation) {
        super.addReplica(replica)
        peerMonitor.addClient(replica)
        val newReplicas = sessionReplicas.toList()
        broadcast(gson.toJson(SyncReplicas(newReplicas)))
    }
    private fun updateReplicaStatus(replica: NetworkInformation) {
        val replicaMonitor = peerMonitor.getClient(replica)
        if (replicaMonitor != null) {
            if (replicaMonitor.last_received.get() == 2){
                replicaMonitor.color = "yellow"
            }
            else if (replicaMonitor.last_received.get() == 3){
                replicaMonitor.color = "red"
                val data = hashMapOf<String, String>()
                data.put("type", "failure_detected")
                data.put("ip", replica.ip)
                data.put("port", replica.port.toString())
                data.put("peer_type", replica.peer_type)
                val message = gson.toJson(data)
                for (sessionReplica in sessionReplicas){
                    sendMessage(message, sessionReplica)
                }
            }
            replicaMonitor.last_received.getAndIncrement()
        }
    }
    private fun replicaHB(replica: NetworkInformation){
        val replicaMonitor = peerMonitor.getClient(replica)
        if (replicaMonitor != null){
            replicaMonitor.color = "green"
            replicaMonitor.last_received.getAndSet(0)
            // Connection Restored
        }
    }

    fun hasHighestPeerID(): Boolean{
        return true
    }

    private fun failOverProtocol() {
        CoroutineScope(Dispatchers.IO).launch {
            bully.start()
        }
    }

    override fun broadcast(message: String) {
        if (!isRingLeader){
            sendMessage(message, RingLeader!!)
        }
        sendToReplicas(message)
        sendToClients(message)
    }

    override fun replicaFailure(failure: NetworkInformation) {
        val replicaMonitor = peerMonitor.getClient(failure)
        replicaMonitor?.other_client_failure_count?.getAndIncrement()
    }
    private fun emitHB(context: Context){
        println("HB")
        val networkInformation = DebugProviders(context).provideNetworkInformation(context)
        val heartBeat = HeartBeat(ip = networkInformation.ip, port = networkInformation.port, peer_type = networkInformation.peer_type)
        for (replica in sessionReplicas){
            sendMessage(gson.toJson(heartBeat), recipient = replica)
            updateReplicaStatus(replica)
        }
        if (!isRingLeader){
            sendMessage(gson.toJson(heartBeat), RingLeader as NetworkInformation)
        }
    }

    fun startHB(context: Context){
        println("STARTING HB")
        Timer("heart", false).schedule(100, 10000){
            emitHB(context = context)
        }
    }

    fun sendToReplicas(message: String){
        for (replica in sessionReplicas) {
            sendMessage(message, replica)
        }
    }

    fun sendToClients(message: String){
        for (client in clients){
            sendMessage(gson.toJson(message), client)
        }
    }

    override fun onHeartBeat(heartBeat: HeartBeat) {
        val replica = NetworkInformation(heartBeat.ip, heartBeat.port, heartBeat.peer_type)
        replicaHB(replica)
    }

    fun getPeersWithHigherIds(): List<NetworkInformation>?{
        return null
    }

    fun getPeerWithId(id: Int): NetworkInformation{
        return NetworkInformation.getNetworkInfo(MainActivity())
    }

    override fun activateQuestion(question: MultipleChoiceQuestion) {
        if (isRingLeader) {
            val jsonTree = gson.toJsonTree(question).also {
                it.asJsonObject.addProperty("type", "multiple_choice_question")
            }
            val json = gson.toJson(jsonTree)
            broadcast(json)
        }
        super.activateQuestion(question)
    }

    protected inner class ReplicaMessageHandler: MessageHandler() {

        override fun handleMessage(message: String) {
            println("Message is $message")
            val type = gson.fromJson(message, Map::class.java)["type"] as String
            val instantiatedObject = gsonConverter.convertToClass(type, message)
            when (type){
                "multiple_choice_question" -> {
                    println("ACTIVATING A QUESTION")
                    val activeQuestion = instantiatedObject as MultipleChoiceQuestion
                    activateQuestion(activeQuestion)
                }
                "hb" -> {
                    println("HEARTBEAT LOGIC")
                    val heartBeat = instantiatedObject as HeartBeat
                    onHeartBeat(heartBeat)
                }
                "failure_detected" -> {
                    println("FAILURE LOGIC")
                    val failure = instantiatedObject as NetworkInformation
                    replicaFailure(failure)
                }
                "connection_restored" -> {
                    println("Connection restored")
                }
                "bully_election" -> {
                    bully.onElectionMessage(instantiatedObject as BullyElectionMessage)
                }
                "bully_ok" -> {
                    bully.onOKMessage(instantiatedObject as BullyOKMessage)
                }
                "bully_coordinator" -> {
                    bully.onCoordinatorMessage(instantiatedObject as BullyCoordinatorMessage)
                }
                "join_request" -> {
                    val joinRequest = instantiatedObject as JoinRequest
                    val client = joinRequest.information
                    val peer_type = joinRequest.peer_type
                    when (peer_type){
                        "client" -> {
                            clients.add(client)
                        }
                        "replica" -> {
                            addReplica(replica = client)
                        }
                    }
                }
                "sync_replicas" -> {
                    val replicas = instantiatedObject as List<NetworkInformation>
                    val mutableReplicas = replicas.toMutableList()
                    val thisClient = DebugProviders(context).provideNetworkInformation(context)
                    mutableReplicas.remove(thisClient)
                    sessionReplicas = mutableReplicas
                }

            }
        }
    }
}

data class SyncReplicas(
    val replicas: List<NetworkInformation>,
    val type: String = "sync_replicas"
)