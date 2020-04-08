package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import androidx.lifecycle.Lifecycle
import com.example.myapplication.DAOs.Cache
import com.example.myapplication.DAOs.QuizDatabase
import com.example.myapplication.DAOs.RepositoryImpl
import com.example.myapplication.Models.*
import com.example.myapplication.Networking.*
import com.google.gson.Gson
import com.google.zxing.WriterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.schedule


// https://demonuts.com/kotlin-generate-qr-code/ was used for the basis of  QRCode generation and used pretty much all of the code for the QR methods. Great thanks to the authors!
class MainActivity : AppCompatActivity(), UDPListener, HeartBeatListener {
    val converter = GSONConverter()
    val gson = Gson()

    // If in a state of debugging
    val debug = true
    val clientOne = NetworkInformation("10.0.2.2", 5000, "client")
    val clientTwo = NetworkInformation("10.0.2.2", 5023, "server")
    val clientThree = NetworkInformation("10.0.2.2", 5026, "client")
    val clientFour = NetworkInformation("10.0.2.2", 5029, "client")


    // if the client is a Ringleader.
    var isRingLeader = false

    // If the client also has partial server functionality. (Can accept responses)
    var isPeer = false

    // The current information of the network. (What's my IP, what's my port, what's my server type)
    var networkInformation: NetworkInformation? = null


    // The current active question if any
    var activeQuestion: MultipleChoiceQuestion? = null


    // Other server replicas
    val peerMonitor = ClientMonitor(arrayListOf(clientOne, clientTwo, clientThree))
    val clientMonitor = ClientMonitor(arrayListOf(clientFour))


    // Thread pools
    val messageSenders: ExecutorService = Executors.newFixedThreadPool(15)
    val userInterfaceThreads: ExecutorService = Executors.newFixedThreadPool(2)

    // Propagation Queue initialized here.

    // The in memory object cache!
    private val questionRepo = Cache()

    // The actual persisted database!
    private var repository: RepositoryImpl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val multicastLock = wifiManager.createMulticastLock("DOG")
        multicastLock.acquire()
        val myLock = wifiManager.createWifiLock("lock")
        myLock.acquire()


        //TODO: print statements are sloppy. Make a logger.
        var typeOfUser = intent.getSerializableExtra("EXTRA_USER_TYPE").toString()
        var userName = getIntent().getStringExtra("EXTRA_USER_NAME")
        println("username: " + userName)
        println("userType: " + typeOfUser)
        //specify the userType in the UI's label
        var userMetadataTextView: TextView = findViewById(R.id.userMetadata);
        userMetadataTextView.setText("userType: " + typeOfUser + "\n" + "Username: " + userName)

        var bitmap: Bitmap?
        var imageview = findViewById<ImageView>(R.id.iv)
        val generateConnectionQrButton = findViewById<Button>(R.id.generate_connection_qr_button)

        val create_question_button = findViewById<Button>(R.id.create_question)
        val answerQuestionButton = findViewById<Button>(R.id.answer_active)
        val browseQuestionsButton = findViewById<Button>(R.id.browse_questions)

        browseQuestionsButton.setOnClickListener {
            val intent = Intent(this, BrowseQuestions::class.java)
            startActivityForResult(intent, 3)
        }

        val responseID = UUID.randomUUID().toString()

        answerQuestionButton.setOnClickListener {
            userInterfaceThreads.execute(Thread(Runnable {
                Intent(this, AnswerQuestionActivity::class.java).also {
                    if (activeQuestion == null) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "No active question",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val user =
                            User(
                                nickname = "Brian",
                                user_id = "5bca90f1-d5a4-46c5-8394-0b5cebbe1945"
                            )
                        val quiz = Quiz(activeQuestion!!.quiz_id, "BobMarley")
                        it.putExtra("active_question", activeQuestion)
                        repository!!.insertUser(user)
                        repository!!.insertQuiz(quiz)
                        it.putExtra("user_id", user.user_id)
                        it.putExtra("response_id", responseID)
                        it.putExtra("quiz_id", quiz.quiz_id)
                        startActivityForResult(it, 2)
                    }
                }
            }))
        }



        generateConnectionQrButton!!.setOnClickListener {
            try {
                userInterfaceThreads.execute(Thread(Runnable {
                    bitmap = QRCodeGenerator.generateInitialConnectionQRCode(500, 500, this)
                    imageview!!.post {
                        imageview!!.setImageBitmap(bitmap)
                    }
                }))
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }

        val quizId = UUID.randomUUID().toString()
        create_question_button.setOnClickListener {
            val intent = Intent(this, CreateQuestionActivity::class.java).also {
                it.putExtra("quizID", quizId)
            }
            startActivityForResult(intent, 1)
        }


        /* We don't want to block the UI thread */
        val server = TCPServer()
        server.addListener(this)
        val TCPDataListener = Thread(server)
        networkInformation = NetworkInformation.getNetworkInfo(this)

        val udpServer = UDPServer()
        udpServer.addListener(this)
        udpServer.setPort(5009)
        val UDPDataListener = Thread(udpServer)

        if (debug == true) {
            if (networkInformation!!.ip == "10.0.2.18") {
                isRingLeader = true
                server.setPort(5024)
                networkInformation!!.port = 5024
                networkInformation!!.peer_type = "server"
            }
        }
        if (isRingLeader) {
            if (networkInformation!!.ip == "10.0.2.16") {
                server.setPort(6000)
            }
            networkInformation!!.peer_type = "server"
        }

        UDPDataListener.start()
        TCPDataListener.start()

        val dataaccess = QuizDatabase.getDatabase(this)
                repository = RepositoryImpl(
            dataaccess.questionDao(),
            dataaccess.responseDao(),
            dataaccess.userDao(),
            dataaccess.quizDao()
        )


        // Timed heartbeat
        Timer("Heartbeat", false).schedule(100, 15000) {
            emitHeartBeat()
        }


    }

    override fun onUDP(data: String) {
        println("RECEIVED FINE")
        messageSenders.execute(Thread(Runnable {
            println(data)
            // Debug here. It prints out all questions in the database.
            /*
            runOnUiThread{
                Toast.makeText(applicationContext, data, Toast.LENGTH_SHORT).show()
            }
             */
            val type = gson.fromJson(data, Map::class.java)["type"] as String
            val message = converter.convertToClass(type, data)
            if (type == "multiple_choice_question") {
                activeQuestion = message as MultipleChoiceQuestion
                println("Activating a question!")
            }
            if (type == "hb") {
                onHeartBeat(message as HeartBeat)
                runOnUiThread {
                    Toast.makeText(applicationContext, peerMonitor.toString(), Toast.LENGTH_LONG)
                        .show()
                }
            }
            if (type == "failure_detected") {
                println("failure!!!!")
                val failedClient = message as NetworkInformation
                peerMonitor.getClient(failedClient).also {
                    it!!.other_client_failure_count.getAndIncrement()
                }
            }
            if (type == "restored_connection") {
                val restoredClient = message as NetworkInformation
                peerMonitor.getClient(restoredClient).also {
                    it!!.other_client_failure_count.getAndDecrement()
                }
            }
        }))
    }

    // This is the scheduled function. Ideally this can also be packaged with the onHeartBeat etc.
    private fun emitHeartBeat() {
        println("Emitting heartbeat")
        val clients = peerMonitor.getClients()
        for (client in clients) {
            var portToSend = client.port

            val status = peerMonitor.getClient(client)
            println("STATUS: $status")
            if (status?.other_client_failure_count!!.get() >= peerMonitor.getClients().size / 2) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failover initiated for $client",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val heartbeat = HeartBeat(
                ip = networkInformation!!.ip,
                port = networkInformation!!.port.toString(),
                peer_type = networkInformation!!.peer_type
            )
            /*
                messageSenders.execute(Thread(Runnable {
                    TCPClient().sendMessage(gson.toJson(heartbeat), client.ip, portToSend)
                }))

                 */
            GlobalScope.launch {
                sendMessage(gson.toJson(heartbeat), client.ip, portToSend)
            }


            if (status?.last_received!!.get() == 2) {
                status.color = "yellow"
            } else if (status.last_received.get() > 2) {
                if (status.color != "red") {
                    status.color = "red"
                    val data = hashMapOf<String, String>()
                    data.put("type", "failure_detected")
                    data.put("ip", client.ip)
                    data.put("port", client.port.toString())
                    data.put("peer_type", client.peer_type)
                    val message = gson.toJson(data)
                    //UDPClient().broadcast(message, 5008, this)
                    for (clientMonitored in clients) {
                        println("Client is $clientMonitored")
                        /*
                            messageSenders.execute(Thread(Runnable {
                                val clientToSendDataTo = TCPClient()
                                clientToSendDataTo.sendMessage(
                                    message,
                                    clientMonitored.ip,
                                    clientMonitored.port
                                )
                            }))
                             */
                        GlobalScope.launch {
                            GlobalScope.launch {
                                TCPClient().sendMessage(
                                    message,
                                    clientMonitored.ip,
                                    clientMonitored.port
                                )
                            }
                        }
                    }
                }


                // Show there was a failure
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failure detected at $client",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            status.last_received.getAndIncrement()
            println(status)
        }
    }

    // This is the listener function. It can be packaged together with the clientMonitor functionality.
    override fun onHeartBeat(heartBeat: HeartBeat) {
        println("Heartbeat $heartBeat")
        var ip = heartBeat.ip
        var port = heartBeat.port.toInt()
        val peerType = heartBeat.peer_type
        if (debug) {
            ip = "10.0.2.2"
            port = 5000
            if (heartBeat.ip == "10.0.2.18") {
                    port = 5023
                }
            }
            val client = peerMonitor.getClient(NetworkInformation(ip, port, peerType))
            if (client != null) {
                client.color = "green"
                client.last_received.getAndSet(0)
                if (client.other_client_failure_count.toInt() > 0) {
                    val data = hashMapOf<String, String>()
                    for (clientTwo in peerMonitor.getClients()) {
                        data.put("type", "connection_restored")
                        data.put("ip", heartBeat.ip)
                        data.put("port", heartBeat.port)
                        data.put("type", heartBeat.type)
                        Thread(Runnable {
                            TCPClient().sendMessage(gson.toJson(data), clientTwo.ip, clientTwo.port)
                        })
                    }
                } }else {
                    println("CLIENT IS NULL")
            }
        }

    suspend fun sendMessage(json: String, ip: String, port: Int){
        withContext(Dispatchers.IO) {
            TCPClient().sendMessage(json, ip, port)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val question = data?.getParcelableExtra("question") as MultipleChoiceQuestion
                questionRepo.insertQuestion(question)
                messageSenders.execute(Thread(Runnable {
                    repository?.insertQuestion(question)
                }))

            }
        }
        if (requestCode == 2){
            if (resultCode == Activity.RESULT_OK){
                val response = data?.getParcelableExtra("response") as MultipleChoiceResponse
                val jsonTree = gson.toJsonTree(response).also {
                    it.asJsonObject.addProperty("type", "multiple_choice_response")
                }
                val json = gson.toJson(jsonTree)
                questionRepo.insertResponse(response)
                    for (client in peerMonitor.getClients()) {
                        messageSenders.execute(Thread(Runnable{
                        TCPClient().sendMessage(json, client.ip, client.port)
                    }))
                }
            }
        }
        if (requestCode == 3){
            if (resultCode == Activity.RESULT_OK && data?.getParcelableExtra<MultipleChoiceQuestion?>("question") != null){
                val questionToActivate = data?.getParcelableExtra("question") as MultipleChoiceQuestion
                val jsonTree = gson.toJsonTree(questionToActivate).also{
                    it.asJsonObject.addProperty("type", "multiple_choice_question")
                }
                val json = gson.toJson(jsonTree)

                    for (client in peerMonitor.getClients()){
                        messageSenders.execute(Thread(Runnable {
                            TCPClient().sendMessage(json, client.ip, client.port)
                        }))
                    }
            }
        }
    }
}
