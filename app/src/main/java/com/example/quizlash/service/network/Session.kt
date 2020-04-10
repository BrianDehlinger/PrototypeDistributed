package com.example.quizlash.service.network

import android.content.Intent
import com.example.quizlash.service.GSONConverter
import com.example.quizlash.service.model.MultipleChoiceQuestion
import com.google.gson.Gson
import io.atomix.cluster.Node
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider
import io.atomix.core.Atomix
import io.atomix.protocols.raft.partition.RaftPartitionGroup
import java.util.concurrent.ExecutorService


abstract class Session(var RingLeader: NetworkInformation, protected var sessionReplicas: MutableCollection<NetworkInformation>, val threadPool: ExecutorService): UDPListener {

    protected val gson = Gson()
    protected val gsonConverter = GSONConverter()
    protected val client = TCPClient()
    protected val messageHandler: MessageHandler = MessageHandler()
    protected val networkService = TCPServer().also{
        it.addListener(this)
        it.threadExService = threadPool
    }
    protected val listeningServerThread = Thread(networkService).also{
        it.start()
    }

    fun sendMessage(message: String, recipient: NetworkInformation){
        threadPool.execute(Thread(Runnable{client.sendMessage(message, recipient.port, recipient.ip)}))
    }

    fun broadcast(message: String, clients: Collection<NetworkInformation>) {
        for (recipient in clients) {
            threadPool.execute(Thread(Runnable {
                client.sendMessage(message, recipient.port, recipient.ip)
            }))
        }
    }

    open fun addReplica(client: NetworkInformation){
        sessionReplicas.add(client)
    }

    override fun onUDP(data: String) {
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
        println("Activating a question")
    }

    open fun replicaFailure(failure: NetworkInformation){
        println("Failure detected")
    }

    protected inner class MessageHandler {

        fun handleMessage(message: String) {
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
            }
        }
    }
}

class ReplicaSession(RingLeader: NetworkInformation, sessionReplicas: MutableCollection<NetworkInformation>, threadPool: ExecutorService, var clients: MutableCollection<NetworkInformation>): Session(RingLeader, sessionReplicas, threadPool){

    private val peerMonitor: PeerMonitor = PeerMonitor().also {
        for (replica in sessionReplicas) {
            it.addClient(replica)
        }
    }

    var isRingLeader = false


    override fun addReplica(client: NetworkInformation) {
        super.addReplica(client)
        peerMonitor.addClient(client)
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

    private fun failOverProtocol(){
        println("FAILOVER INIT")
    }

    override fun replicaFailure(failure: NetworkInformation) {
        val replicaMonitor = peerMonitor.getClient(failure)
        replicaMonitor?.other_client_failure_count?.getAndIncrement()
    }
    fun emitHB(){
        val heartBeat = HeartBeat(ip = RingLeader.ip, port = RingLeader.port, peerType = RingLeader.peer_type)
        for (replica in sessionReplicas){
            gson.toJson(heartBeat)
            sendMessage(gson.toJson(heartBeat), recipient = replica)
            updateReplicaStatus(replica)
        }
    }

    override fun onHeartBeat(heartBeat: HeartBeat) {
        val replica = NetworkInformation(heartBeat.ip, heartBeat.port, heartBeat.peerType)
        replicaHB(replica)
    }

    override fun activateQuestion(question: MultipleChoiceQuestion) {
        println("ACTIVATING QUESTION")
        if (isRingLeader) {
            val jsonTree = gson.toJsonTree(question).also {
                it.asJsonObject.addProperty("type", "multiple_choice_question")
            }
            val json = gson.toJson(jsonTree)
            broadcast(json, sessionReplicas + clients)
        }
        val activateQuestionIntent = Intent()
    }

}