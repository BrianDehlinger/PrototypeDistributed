package com.example.quizlash.service.network

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


class Bully(val session: ReplicaSession){

    var started: AtomicBoolean = AtomicBoolean(false)
    var okReceived: AtomicBoolean = AtomicBoolean(false)
    var droppedOut: AtomicBoolean = AtomicBoolean(false)
    var term: AtomicInteger = AtomicInteger(1)
    var victoryMessageReceived: AtomicBoolean = AtomicBoolean(false)

    suspend fun start(){
        println("STARTING ELECTION")
        val clientsWithHigherIds = session.getPeersWithHigherIds()
        if (clientsWithHigherIds == null){
            session.RingLeader = session.getPeerWithId(session.peerId)
            session.sendToReplicas(BullyCoordinatorMessage(peerId = session.peerId).toString())
        }
        else{
            for (replica in clientsWithHigherIds) {
                session.sendMessage(BullyElectionMessage(peerId = session.peerId).toString(), replica)
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
                session.RingLeader = session.getPeerWithId(session.peerId)
                session.sendToReplicas(BullyCoordinatorMessage(peerId = session.peerId).toString())
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
                catch(e:CancellationException){
                    this.clear()
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
            session.RingLeader = session.getPeerWithId(session.peerId)
            session.sendToReplicas(BullyCoordinatorMessage(peerId = session.peerId).toString())
        }
        else {
            session.sendMessage(BullyOKMessage(peerId = session.peerId).toString(), session.getPeerWithId(electionMessage.peerId))
            if (electionMessage.peerId < session.peerId){
                CoroutineScope(Dispatchers.IO).launch {
                    bully.clear()
                    bully.start()
                }
            }

        }
    }
    fun onOKMessage(okMessage: BullyOKMessage){
        okReceived.getAndSet(true)
        dropOut()
    }
    fun onCoordinatorMessage(coordinatorMessage: BullyCoordinatorMessage){
        println("A leader has been chosen")
        session.RingLeader = session.getPeerWithId(coordinatorMessage.peerId)
    }

    private fun dropOut(){
        droppedOut.getAndSet(true)
        println("DROPPED OUT OF ELECTION ${term.get()}")
    }

    private fun newTerm(){
        okReceived.getAndSet(false)
        droppedOut.getAndSet(false)
        victoryMessageReceived.getAndSet(false)
        term.getAndIncrement()
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
    val peerId: Int
)