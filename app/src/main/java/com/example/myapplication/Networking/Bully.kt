package com.example.myapplication.Networking

import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class Bully(val session: ReplicaSession){

    var okReceived: AtomicBoolean = AtomicBoolean(false)
    var droppedOut: AtomicBoolean = AtomicBoolean(false)
    var victoryMessageReceived: AtomicBoolean = AtomicBoolean(false)

    suspend fun start(){
        println("STARTING ELECTION")
        this.clear()
        val clientsWithHigherIds = session.getPeersWithHigherIds()
        if (clientsWithHigherIds == null){
            becomeLeader()
        }
        else{
            for (replica in clientsWithHigherIds) {
                session.sendMessage(Gson().toJson(BullyElectionMessage(peerId = session.peerId)), replica)
            }
            try{
                withTimeout(9000){
                    repeat(10){
                        if (okReceived.get()){
                            cancel()
                        }
                        delay(1000)
                    }
                }
            }
            catch(e: TimeoutCancellationException) {
                becomeLeader()
            }
            catch(e: CancellationException){
                try{
                    withTimeout(9000){
                        repeat(10){
                            if (victoryMessageReceived.get()){
                                cancel()
                            }
                        }
                    }
                }
                catch(e: TimeoutCancellationException){
                    this.start()
                }
            }
        }
    }
    fun onElectionMessage(electionMessage: BullyElectionMessage, bully: Bully = this){
        if (droppedOut.get()){
            return
        }
        if (session.hasHighestPeerID()){
            becomeLeader()
        }
        else {
            session.sendMessage(Gson().toJson(BullyOKMessage(peerId = session.peerId)), session.getPeerWithId(electionMessage.peerId))
            if (electionMessage.peerId < session.peerId){
                CoroutineScope(Dispatchers.IO).launch {
                    bully.start()
                }
            }

        }
    }

    // This can be problematic. Assuming leadership
    private fun becomeLeader(){
        println("BECOMING LEADER")
        session.sendToReplicas(Gson().toJson(BullyCoordinatorMessage(peerId = session.peerId, networkInformation = session.getInformationOnLocalNetworkInfo())))
        session.assumeLeadership()
    }
    fun onOKMessage(okMessage: BullyOKMessage){
        okReceived.getAndSet(true)
        dropOut()
    }
    fun onCoordinatorMessage(coordinatorMessage: BullyCoordinatorMessage){
        if (!victoryMessageReceived.get()){
            victoryMessageReceived.set(true)
            session.setTheRingLeader(coordinatorMessage.networkInformation)
        }
        println("A leader ${coordinatorMessage.networkInformation} has been chosen")
    }

    private fun dropOut(){
        droppedOut.getAndSet(true)
    }

    private fun clear(){
        okReceived.getAndSet(false)
        droppedOut.getAndSet(false)
        victoryMessageReceived.getAndSet(false)
    }
}


data class BullyElectionMessage(
    val type: String = "bully_election",
    val peerId: Int
)

data class BullyOKMessage(
    val type: String = "bully_ok",
    val peerId: Int
)

data class BullyCoordinatorMessage(
    val type: String = "bully_coordinator",
    val peerId: Int,
    val networkInformation: NetworkInformation
)