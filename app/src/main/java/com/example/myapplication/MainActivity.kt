package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import com.example.myapplication.DAOs.Cache
import com.example.myapplication.DAOs.QuizDatabase
import com.example.myapplication.DAOs.RepositoryImpl
import com.example.myapplication.Models.*
import com.example.myapplication.Networking.*
import com.google.gson.Gson
import com.google.zxing.WriterException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import kotlin.concurrent.schedule


// https://demonuts.com/kotlin-generate-qr-code/ was used for the basis of  QRCode generation and used pretty much all of the code for the QR methods. Great thanks to the authors!
class MainActivity : AppCompatActivity(), UDPListener, HeartBeatListener {
    val converter = GSONConverter()
    val gson = Gson()
    val debug = true
    var isServer = false
    var networkInformation: NetworkInformation?= null
    var activeQuestion: MultipleChoiceQuestion? = null
    val clientOne = NetworkInformation("10.0.2.2", 5000, "client")
    val clientTwo = NetworkInformation("10.0.2.2", 5023, "server")
    val clientThree = NetworkInformation("10.0.2.2", 5026, "client");
    val clientMonitor = ClientMonitor(arrayListOf(clientOne, clientTwo, clientThree))
    val executor = Executors.newCachedThreadPool()

    // The in memory object cache!
    private val questionRepo = Cache()

    // The actual persisted database!
    private var repository: RepositoryImpl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

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

        browseQuestionsButton.setOnClickListener{
            val intent = Intent(this, BrowseQuestions::class.java)
            startActivityForResult(intent, 3)
        }

        val responseID = UUID.randomUUID().toString()

        answerQuestionButton.setOnClickListener{
            executor.execute(Thread(Runnable {
                Intent(this, AnswerQuestionActivity::class.java).also{
                    if (activeQuestion == null) {
                        runOnUiThread{
                            Toast.makeText(applicationContext,"No active question", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else {
                        val user =
                            User(nickname = "Brian", user_id = "5bca90f1-d5a4-46c5-8394-0b5cebbe1945")
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
                executor.execute(Thread(Runnable {
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
        create_question_button.setOnClickListener{
            val intent = Intent(this, CreateQuestionActivity::class.java).also{
                it.putExtra("quizID", quizId)
            }
            startActivityForResult(intent, 1)
        }


        /* We don't want to block the UI thread */
        val server = TCPServer()
        server.addListener(this)
        val TCPDataListener = Thread(server)
        networkInformation = NetworkInformation.getNetworkInfo(this)


        if (debug == true) {
            if (networkInformation!!.ip == "10.0.2.18") {
                isServer = true
                server.setPort(5024)
                networkInformation!!.port = 5024
                networkInformation!!.peer_type = "server"
            }
        }
        if (isServer){
            networkInformation!!.peer_type = "server"
        }
        TCPDataListener.start()


        val dataaccess = QuizDatabase.getDatabase(this)
        repository = RepositoryImpl(dataaccess.questionDao(), dataaccess.responseDao(), dataaccess.userDao(), dataaccess.quizDao())

        Timer("Heartbeat", false).schedule(100, 5000){
            emitHeartBeat()
        }


    }

    override fun onUDP(data: String) {
        println("RECEIVED FINE")
        executor.execute(Thread(Runnable {
            println(data)
            runOnUiThread {
                Toast.makeText(applicationContext, data, Toast.LENGTH_SHORT).show()
            }
            // Debug here. It prints out all questions in the database.
            val type = gson.fromJson(data, Map::class.java)["type"] as String
            val message = converter.convertToClass(type, data)
            if (type == "multiple_choice_question"){
                activeQuestion = message as MultipleChoiceQuestion
                println("Activating a question!")
            }
            if (type == "hb"){
                onHeartBeat(message as HeartBeat)
            }
            if (type == "failure_detected"){
                println("failure!!!!")
                val failedClient =  message as NetworkInformation
                clientMonitor.getClient(failedClient).also {
                    it!!.other_client_failure_count.getAndIncrement()
                }
            }
            if (type == "restored_connection"){
                val restoredClient = message as NetworkInformation
                clientMonitor.getClient(restoredClient).also{
                    it!!.other_client_failure_count.getAndDecrement()
                }
            }
        }))
    }

    // This is the scheduled function. Ideally this can also be packaged with the onHeartBeat etc.
    private fun emitHeartBeat(){
        println("Emitting heartbeat")
        executor.execute(Thread(Runnable{
            val clients = clientMonitor.getClients()
            for (client in clients) {
                var portToSend = client.port
                if (debug){
                    portToSend = 5023
                }


                val status = clientMonitor.getClient(client)
                if (status?.other_client_failure_count!!.get() >= clientMonitor.getClients().size/2){
                    println("FAILOVER PROTOCOL INITIATED")
                }

                val heartbeat = HeartBeat(
                    ip = networkInformation!!.ip,
                    port = networkInformation!!.port.toString(),
                    peer_type = networkInformation!!.peer_type
                )

                // Send the heartbeat
                if (debug == true) {
                    if (client == clientTwo) {
                        TCPClient().sendMessage(gson.toJson(heartbeat), client.ip, portToSend)
                    }
                }
                else {
                    TCPClient().sendMessage(gson.toJson(heartbeat), client.ip, portToSend)
                }


                if (status?.last_received!!.get() == 2) {
                    status.color = "yellow"
                } else if (status.last_received.get() > 2) {
                    if (status.color != "red"){
                        status.color = "red"
                        val data = hashMapOf<String, String>()
                        data.put("type", "failure_detected")
                        data.put("ip", client.ip)
                        data.put("port", client.port.toString())
                        data.put("peer_type", client.peer_type)
                        val message = gson.toJson(data)
                        for (clientMonitored in clients){
                            println("Client is $clientMonitored")
                            executor.execute(Thread(Runnable {
                                val clientToSendDataTo = TCPClient()
                                clientToSendDataTo.sendMessage(
                                    message,
                                    clientMonitored.ip,
                                    clientMonitored.port
                                )
                            }))
                        }

                        // Show there was a failure
                        runOnUiThread{
                            Toast.makeText(applicationContext,"Failure detected at $client", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                status.last_received.getAndIncrement()
            }
        }))
    }

    // This is the listener function. It can be packaged together with the clientMonitor functionality.
    override fun onHeartBeat(heartBeat: HeartBeat) {
        println("Heartbeat received!")
        var ip = heartBeat.ip
        var port = heartBeat.port.toInt()
        var peerType = heartBeat.peer_type
        if (debug) {
            ip = "10.0.2.2"
            port = 5000
            if (heartBeat.ip == "10.0.2.18") {
                port = 5023
            }
        }
        println("$ip, $port, $peerType")
        val client = clientMonitor.getClient(NetworkInformation(ip, port, peerType))


        if (client != null) {
            println("Client is not null")
            client.color = "green"
            client.last_received.getAndSet(0)
            if (client.other_client_failure_count.toInt() > 0) {
                val data = hashMapOf<String, String>()
                executor.execute(Thread(Runnable {
                    for (clientTwo in clientMonitor.getClients()) {
                        data.put("type", "connection_restored")
                        data.put("ip", heartBeat.ip)
                        data.put("port", heartBeat.port)
                        data.put("type", heartBeat.type)
                    }
                    TCPClient().sendMessage(gson.toJson(data), clientTwo.ip, clientTwo.port)
                }))
            }
        }
        else {
            println("CLIENT IS NULL")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val question = data?.getParcelableExtra("question") as MultipleChoiceQuestion
                questionRepo.insertQuestion(question)
                executor.execute(Thread(Runnable {
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
                executor.execute(Thread(Runnable{
                    for (client in clientMonitor.getClients()) {
                        TCPClient().sendMessage(json, client.ip, client.port)
                    }
                }))
            }
        }
        if (requestCode == 3){
            if (resultCode == Activity.RESULT_OK){
                val questionToActivate = data?.getParcelableExtra("question") as MultipleChoiceQuestion
                val jsonTree = gson.toJsonTree(questionToActivate).also{
                    it.asJsonObject.addProperty("type", "multiple_choice_question")
                }
                val json = gson.toJson(jsonTree)
                executor.execute(Thread(Runnable{
                    for (client in clientMonitor.getClients()){
                        TCPClient().sendMessage(json, client.ip, client.port)
                    }
                }))
            }
        }
    }
}
