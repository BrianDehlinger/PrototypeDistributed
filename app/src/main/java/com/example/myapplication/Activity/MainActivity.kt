package com.example.myapplication.Activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.DAOs.Cache
import com.example.myapplication.DAOs.QuizDatabase
import com.example.myapplication.DAOs.RepositoryImpl
import com.example.myapplication.GSONConverter
import com.example.myapplication.Models.*
import com.example.myapplication.Networking.NetworkInformation
import com.example.myapplication.Networking.UDPClient
import com.example.myapplication.Networking.UDPListener
import com.example.myapplication.Networking.UDPServer
import com.example.myapplication.QRCodeGenerator
import com.example.myapplication.R
import com.example.myapplication.ResponsesActivity
import com.google.gson.Gson
import com.google.zxing.WriterException
import java.io.Serializable
import java.util.*
import kotlin.concurrent.schedule

// https://demonuts.com/kotlin-generate-qr-code/ was used for the basis of  QRCode generation and used pretty much all of the code for the QR methods. Great thanks to the authors!
class MainActivity : AppCompatActivity(), UDPListener, HeartBeatListener {
    var userType: UserType? = null
    var quizName: String? = null
    var userName: String? = null
    var sessionId: String? = null
    private var bitmap: Bitmap? = null
    private var imageview: ImageView? = null
    private var generateConnectionQrButton: Button? = null

    var currentQuestionPromptTextView: TextView? = null
    var currentQuestionChoicesTextView: TextView? = null

    private var sessionIdTextView: TextView? = null
    private var activateQuizButton: Button? = null
    val converter = GSONConverter()
    var ip = "0.0.0.0"
    val gson = Gson()
    var networkInformation: NetworkInformation? = null
    var activeQuestion: MultipleChoiceQuestion? = null
    var currentActiveQuestion: MultipleChoiceQuestion1? = null
    val clientOne = NetworkInformation("10.0.2.2", 5023, "client")
    val clientTwo = NetworkInformation("10.0.2.2", 5000, "client")
    val clientThree = NetworkInformation("10.0.2.2", 5026, "client")

    var listOfQuizQuestions: ArrayList<MultipleChoiceQuestion1>? = null

    var CURRENT_QUESTION_INDEX = 0

    // The in memory object cache!
    private val questionRepo = Cache()

    // The actual persisted database!
    private var repository: RepositoryImpl? = null


    private var allResponsesList: ArrayList<MultipleChoiceResponse>? = ArrayList<MultipleChoiceResponse>()


    //For BullyAlgorithm
    var clientsMap : HashMap<Double, String> = HashMap<Double, String> () //userId(Key) & userName(Value)
    var userId = Math.random() //generates a random Double between 0 and 1
    var currentServerId: Double? = null
    var currentServerUserName: String? = null

    var heartbeatsReceivedInCurrentRound = 0
    val LIVENESS_THRESHOLD = 3

    /**
     * TODO: Document more thoroughly.
     * Will keep track of how many times each client in the session has emmitted a heartbeat.
     * This includes the Server. This will be helpful for determining whether a Server has
     * gone down, and for determining the Server's livenessStatus.
     * */
    var livenessCheckRound = 0

    /**
     * TODO: Document more thoroughly.
     * Will record the `livenessCheckRound` since the Server last emitted a heartbeat.
     * This will be used to calculate the Server's livenessStatus (which will be done
     * by all the non-Server clients).  This will also determine whether an election
     * for a new server needs to occur.
     * */
    var livenessCheckRoundSinceServerLastSeen = 0

    /**
     * TODO: Document more thoroughly.
     * GREEN as default. Will be reset to GREEN when a new server gets elected.
     * */
    var currentServerLivenessStatus = LivenessStatus.GREEN


    val clients = arrayListOf<NetworkInformation>().also{
        it.add(clientOne)
        it.add(clientTwo)
        it.add(clientThree)
    }

    val clientMonitor = hashMapOf<NetworkInformation, String>().also{
        for (client in clients){
            println("Adding the following client: " + client )
            it.put(client, "green")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        userType = intent.getSerializableExtra("EXTRA_USER_TYPE") as UserType?
        sessionIdTextView = findViewById<TextView>(R.id.mainActivity_sessionIdTextView)

        if(UserType.SERVER.equals(userType)) {
            quizName = getIntent().getStringExtra("EXTRA_QUIZ_NAME")

            val i = intent
            listOfQuizQuestions = intent.getSerializableExtra("EXTRA_LIST_OF_QUIZ_QUESTIONS") as ArrayList<MultipleChoiceQuestion1>

            println("received the following listOfQuestions size: ")
            println(listOfQuizQuestions!!.size)

            showServerButtons()

            //Generating a random int as sessionId.
            val randomSessionId = (Math.random() * 9000).toInt() + 1000
            sessionIdTextView?.setText("Session ID: " + randomSessionId.toString())
            sessionId = randomSessionId.toString()
        } else {
            hideServerButtons()
            sessionId = getIntent().getStringExtra("EXTRA_SESSION_ID")
            sessionIdTextView?.setText(sessionId)
        }


        //TODO: print statements are sloppy. Make a logger.
        //Extracting relevant values from previous activity -- CLIENT and SERVER
        userName = getIntent().getStringExtra("EXTRA_USER_NAME")

        println("username: " + userName)
        println("quizName: " + quizName)
        println("userType: " + userType)


        //specify the userType in the UI's label
        var userMetadataTextView: TextView = findViewById(R.id.userMetadata);
        userMetadataTextView.setText("quizName: " + quizName + "\n" + "Username: " + userName)

        //specify the active question in the UI (will be null initially)
        //currentActiveQuestionTextView.setText("Active Question: " + currentActiveQuestion)

        imageview = findViewById(R.id.iv)
        generateConnectionQrButton = findViewById(R.id.generate_connection_qr_button)
        networkInformation = NetworkInformation.NetworkInfoFactory.getNetworkInfo(this)
        val activateQuizButton = findViewById<Button>(R.id.activateQuizButton)
        val submitActiveQuestionAnswerButton = findViewById<Button>(R.id.mainActivity_submitAnswerButton)
        val activateNextQuestionButton = findViewById<Button>(R.id.activateNextQuestionButton)

        val dataaccess = QuizDatabase.getDatabase(this)
        val responseID = UUID.randomUUID().toString()
        var browseQuestionsButton = findViewById<Button>(R.id.browse_questions)
        repository = RepositoryImpl(dataaccess.questionDao(), dataaccess.responseDao(), dataaccess.userDao(), dataaccess.quizDao())

        currentQuestionPromptTextView = findViewById<TextView>(R.id.mainActivity_activeQuestionPromptTextView)
        currentQuestionChoicesTextView = findViewById<TextView>(R.id.mainActivity_activeQuestionChoicesTextView)

        Timer("Heartbeat", false).schedule(100, 10000){
            emitHeartBeat()
        }

        val quizId = UUID.randomUUID().toString()

        browseQuestionsButton.setOnClickListener{
            val intent = Intent(this, BrowseQuestions::class.java)
            startActivityForResult(intent, 3)
        }








        activateQuizButton.setOnClickListener{
            //Ony the server has the power to change the active question
            if(UserType.SERVER.equals(userType)) { //guarding against null userType values
                println("PERMISSION TO ACTIVATE QUESTION GRANTED, " + userName)
                activateQuestion(listOfQuizQuestions!!.get(CURRENT_QUESTION_INDEX))


                //Updating the UI:
                var newActiveQuestion = listOfQuizQuestions!!.get(CURRENT_QUESTION_INDEX)

                var newActiveQuestionPrompt = newActiveQuestion.prompt
                var newActiveQuestionChoicesList = newActiveQuestion.choices

                var choicesListAsString: String = ""

                for(choice in newActiveQuestionChoicesList) {
                    choicesListAsString += "- " + choice + "\n"
                }

                currentQuestionPromptTextView?.setText(newActiveQuestionPrompt)
                currentQuestionChoicesTextView?.setText(choicesListAsString)

                CURRENT_QUESTION_INDEX++

                //callActivateQuestion method
                activateQuestion(newActiveQuestion)

            } else {
                println("YOU DONT HAVE PERMISSION TO ACTIVATE A NEW QUESTION, " + userName)
            }
        }

        activateNextQuestionButton.setOnClickListener{
            //Ony the server has the power to change the active question
            var numberOfQuestions = listOfQuizQuestions?.size

            if(CURRENT_QUESTION_INDEX < numberOfQuestions!!) { //guarding against null userType values
               println("ACTIVATING THE NEXT QUESTION!")
                activateQuestion(listOfQuizQuestions!!.get(CURRENT_QUESTION_INDEX))

                //Updating the UI:
                var newActiveQuestion = listOfQuizQuestions!!.get(CURRENT_QUESTION_INDEX)

                var newActiveQuestionPrompt = newActiveQuestion.prompt
                var newActiveQuestionChoicesList = newActiveQuestion.choices

                var choicesListAsString: String = ""

                for(choice in newActiveQuestionChoicesList) {
                    choicesListAsString += "- " + choice + "\n"
                }

                currentQuestionPromptTextView?.setText(newActiveQuestionPrompt)
                currentQuestionChoicesTextView?.setText(choicesListAsString)

                CURRENT_QUESTION_INDEX++

                //callActivateQuestion method
                activateQuestion(newActiveQuestion)

            } else {
                println("NO MORE QUESTIONS TO DISPLAY! OPEN A NEW ACTIVITY TO SHOW RESULTS")

                /**
                 * Set the `nextActivity` to be the `CreateQuizActivity`
                 */
                //TODO: Define logic for when the user has finished creating all of the quiz's questions and is ready to start the session.
                val intent = Intent(applicationContext, ResponsesActivity::class.java)

                intent.putParcelableArrayListExtra("EXTRA_ALL_RESPONSES_LIST", allResponsesList);
                intent.putExtra(
                    "EXTRA_LIST_OF_QUIZ_QUESTIONS", listOfQuizQuestions as Serializable
                )
                println("Sending the following allResponsesList: " + allResponsesList);
                println("Sending the following questionsList: " + listOfQuizQuestions);
                startActivity(intent)
            }
        }



        submitActiveQuestionAnswerButton.setOnClickListener{
            val submittedAnswer = findViewById<EditText>(R.id.mainActivity_answerEditText).text.toString()
            println("USER SELECTED THE FOLLOWING ANSWER: " + submittedAnswer)

            var multipleChoiceResponse: MultipleChoiceResponse = MultipleChoiceResponse(
                "", "", submittedAnswer,
                userName.toString(), sessionId.toString(), currentQuestionPromptTextView!!.text.toString()
            )

            //propagating the new response
            propagateMultipleChoiceAnswer(multipleChoiceResponse)
        }










        /* We don't want to block the UI thread */
        val server = UDPServer()
        server.addListener(this)
        val udpDataListener = Thread(server)
        if (networkInformation!!.ip == "10.0.2.18"){
            server.setPort(5024)
            println("THIS IS TRUE")
        }
        udpDataListener.start()


        generateConnectionQrButton!!.setOnClickListener {
            try {
                Thread(Runnable {
                    bitmap =
                        QRCodeGenerator.generateInitialConnectionQRCode(
                            500,
                            500,
                            this
                        )
                    imageview!!.post {
                        imageview!!.setImageBitmap(bitmap)
                    }
                }).start()
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }



    }

    private fun hideServerButtons() {
        //activateNextQuestionButton?.setVisibility(View.GONE)
    }

    private fun showServerButtons() {
        //activateNextQuestionButton?.setVisibility(View.VISIBLE)
    }

    private fun activateQuestion(questionToActivate: MultipleChoiceQuestion1) {
        //Ony the server has the power to change the active question
        if(UserType.SERVER.equals(userType)) { //guarding against null userType values
            println("PERMISSION TO ACTIVATE QUESTION GRANTED, " + userName)
            userName?.let { activateQuestion(it, questionToActivate) }
        } else {
            println("YOU DONT HAVE PERMISSION TO ACTIVATE A NEW QUESTION, " + userName)
        }
    }


    private fun propagateMultipleChoiceAnswer(multipleChoiceResponse: MultipleChoiceResponse) {
        println("ABOUT TO PROPAGATE THE FOLLOWING ANSWER OBJECT: " + multipleChoiceResponse.toString())
        for(client in clients) {
            //spin up a new thread for each client connection
            val thread = Thread(Runnable {
                try {
                    //Propagate the newly-activated question for this specific client
                    UDPClient().propagateMultipleChoiceResponse(userName.toString(), client.ip, client.port, multipleChoiceResponse)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            thread.start()
        }
    }

    private fun propagateNewElectionNotification(electionNotification: ElectionNotification) {
        println("ABOUT TO PROPAGATE THE FOLLOWING ElectionNotification OBJECT: " + electionNotification.toString())
        for(client in clients) {
            //spin up a new thread for each client connection
            val thread = Thread(Runnable {
                try {
                    //Propagate the electionNotification for this specific client
                    UDPClient().propagateNewElectionNotification(electionNotification, client.ip, client.port)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            thread.start()
        }
    }

    private fun propagateNewServerNotification(newServerNotification: NewServerNotification) {
        println("ABOUT TO PROPAGATE THE FOLLOWING NewServerNotification OBJECT: " + newServerNotification.toString())
        for(client in clients) {
            //spin up a new thread for each client connection
            val thread = Thread(Runnable {
                try {
                    //Propagate the electionNotification for this specific client
                    UDPClient().propagateNewServerNotification(newServerNotification, client.ip, client.port)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            thread.start()
        }
    }


    /**
     * Handles the receipt and filtering of various message types
     */
    override fun onUDP(data: String) {
        //First, extract the 'type' from the data's payload to determine downward processing
        val type = gson.fromJson(data, Map::class.java)["type"] as String
        val message = converter.convertToClass(type, data)

        Thread(Runnable {
            println(data)
            runOnUiThread {
                if("hb" == type){
                    //Show a toast message on all device screens for a heartbeat message only
                    Toast.makeText(applicationContext, data, Toast.LENGTH_SHORT).show()
                }

                println("Current Active question is: " + currentActiveQuestion)
            }

            // Debug here. It prints out all questions in the database.
            println("Received the following data type: " + type)

            /**
             * Defines the logic for when the received data relates to a multiple choice question.
             * More specifically, this `if` block updates the `currentActiveQuestion` value for
             * all clients, and updates the clients' UI's to reflect this change.
             */
            if ("multiple_choice_question" == type){
                val multipleChoiceQuestion = gson.fromJson(data, MultipleChoiceQuestion1::class.java)
                currentActiveQuestion = multipleChoiceQuestion

                //currentActiveQuestionTextView.setText("Active Question: " + currentActiveQuestion)
                println("A new Multiple Choice question has been activated!")

                //UI updating logic goes here
                runOnUiThread{
                    println("UI updating should go here")
                    updateCurrentActiveQuestionUI(multipleChoiceQuestion)
                }
            }
            if ("hb" == type){
                onHeartBeat(message as HeartBeat)


                if(UserType.SERVER.equals(userType)) {
                    println("I AM THE SERVERRRRRRRRR")
                } else {
                    println("I AM A CLIENTTTTTTT!!")
                }

            }

            if("multiple_choice_response" == type) {
                val multipleChoiceResponse = gson.fromJson(data, MultipleChoiceResponse::class.java)

                println("RECEIVED A RESPONSE FROM A CLIENT: " + multipleChoiceResponse.toString())
                //store the multipleChoiceResponse
                allResponsesList?.add(multipleChoiceResponse)

                println("RESPONSES RECORDED COUNT: " + allResponsesList?.size)
            }

        }).start()
    }











    private fun updateCurrentActiveQuestionUI(newActiveQuestion1: MultipleChoiceQuestion1) {

        var newActiveQuestionPrompt = newActiveQuestion1.prompt
        var newActiveQuestionChoicesList = newActiveQuestion1.choices

        var choicesListAsString: String = ""

        for(choice in newActiveQuestionChoicesList) {
            choicesListAsString += "- " + choice + "\n"
        }

        currentQuestionPromptTextView?.setText(newActiveQuestionPrompt)
        currentQuestionChoicesTextView?.setText(choicesListAsString)
    }




    /**
     * Iterates through the list of UDP clients and emits a `heartbeat`
     * toast message in each of the individual devices.
     */
    private fun emitHeartBeat(){
        println("Emitting heartbeat")
        Thread(Runnable{
            for (client in clients){
                val heartbeat = userType?.let { HeartBeat(ip = networkInformation!!.ip, port = networkInformation!!.port.toString(),
                    userType = userType!!,  userName = userName!!, userId = userId) }

                UDPClient().sendMessage(gson.toJson(heartbeat), client.ip, client.port)
            }
        }).start()
    }


    /**
     * Will activate a specified question, prompting all clients to answer it.
     */
    private fun activateQuestion(instructorUserName: String, questionToActivate: MultipleChoiceQuestion1) {
        println("ABOUT TO ACTIVATE THE FOLLOWING QUESTION: " + questionToActivate)
        for(client in clients) {
            //spin up a new thread for each client connection
            val thread = Thread(Runnable {
                try {
                    //Propagate the newly-activated question for this specific client
                    UDPClient().activateQuestion(instructorUserName, client.ip, client.port, questionToActivate)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

            thread.start()
        }
    }












    override fun onHeartBeat(heartBeat: HeartBeat) {
        clientMonitor[NetworkInformation(heartBeat.ip, heartBeat.port.toInt(), "client")] = "Yellow"

        if(currentServerId == null && UserType.SERVER.equals(heartBeat.userType)) {
            /**
             * This device has just received a heartbeat message from the current Server. Identify its ID
             * as such.
             * */
            currentServerId = heartBeat.userId
            currentServerUserName = heartBeat.userName

            livenessCheckRound = 0
            livenessCheckRoundSinceServerLastSeen = 0
            currentServerLivenessStatus = LivenessStatus.GREEN

            println("A new Server has been elected.")
        }

        println("Current Server Metadata: \n"
                + "Username: " + currentServerUserName + "\n"
                + "ID: " + currentServerId)


        recordClientHeartBeat(heartBeat.userId, heartBeat.userName)

    }

    //for determining whether a new client has joined the session & managing the liveness data
    fun recordClientHeartBeat(userId: Double, userName: String) {
        if(!clientsMap.containsKey(userId)) {
            /**
             * The map does not contain the userId, so add it. Since adding a new client throws off
             * the liveness counts, reset the counts back to 0 so it doesn't erroneously affect the
             * currentServer's livenessStatus.
             * */
            clientsMap.put(userId, userName)

            //reset the livenessCheck data
            livenessCheckRound = 0
            livenessCheckRoundSinceServerLastSeen = 0
        } else if(userId.equals(currentServerId)) {
            /**
             * The map already contained the userId for which we received a heartbeat message.
             * If this heartbeatMessage belongs to the currentServer, update
             * the `livenessCheckRoundSinceServerLastSeen` value to the currentValue of the
             * `livenessCheckRound`.
             * */
            livenessCheckRoundSinceServerLastSeen = livenessCheckRound

            //reset the heartbeatsReceivedInCurrentRound count to begin a new livenessCheckRound
            heartbeatsReceivedInCurrentRound = 0

            //begin a new livenessCheckRound
            livenessCheckRound++
        } else if (LivenessStatus.RED.equals(currentServerLivenessStatus)) {
            /**
             * An election needs to be had. Initiate it.
             * */
            println("CALLING FOR A NEW LEADERRRRRRRRRRRRRRRRRRRRRRRRRRRRR!!!!")

            /***
             * All clients need to be notified that a new Server is about to be elected.
             * First, an `ElectionNotification` object needs to be created. This object
             * simply holds metadata on the client that is emitting this ElectionNotification.
             * This information will be sent to each client so that each client can reset their
             * `currentServer` value, and expect to receive a `NewServerNotification`.
             */
            var electionNotification: ElectionNotification = ElectionNotification(userName, userId)

            /**Notify all clients that a new Server is about to be elected.*/
            //propagateNewElectionNotification(electionNotification)

            //TODO: Inquire as to whether this client is the successor.
            if(iAmSuccessor()) {
                /**This client has determined that it holds the next highest userId.
                 * Set the currentServerUserName, serverId, and liveness stat values accordingly.
                 * */
                currentServerUserName = userName
                currentServerId = userId

                //reset the livenessCheck data
                livenessCheckRound = 0
                livenessCheckRoundSinceServerLastSeen = 0
                heartbeatsReceivedInCurrentRound = 0

                /**Finally, make it official*/
                userType = UserType.SERVER
                currentServerLivenessStatus = LivenessStatus.GREEN

                println("I AM THE SUCCESSOR!")

                println("----> currentServerUserName has been updated to: " + currentServerUserName + " serverId: " + currentServerId + "currentServerStatus: " + currentServerLivenessStatus)

                /**
                 * Now, the rest of the clients will be notified that a new server has identified
                 * itself when this client emits a heartbeat message.
                 * */

            } else {
                /**I am not the successor. Await a heartbeat from the person who is.*/
                currentServerUserName = null

                currentServerId = null
                //electionInProgress = true

                println("I AM NOT THE SUCCESSOR.")
            }

        }
        else if(heartbeatsReceivedInCurrentRound > clientsMap.size && currentServerUserName != null) {

            /**
             * The heartbeat did NOT belong to either a new client, nor the currentServer.
             * But the heartbeatCount has already recorded more heartbeats than the current
             * size of the registered clients, and the current serverLivenessStatus is NOT RED.
             *
             * This means a heartbeat from the Server has not reset the
             * `heartbeatsReceivedInCurrentRound` count because no server heartbeat has been
             * received.
             *
             * increment the `heartbeatsReceivedInCurrentRound`, then calculate the server's new
             * LivenessStatus accordingly.
             * */
            heartbeatsReceivedInCurrentRound++

            currentServerLivenessStatus = determineServerLivenessStatus()
        } else {
            /**
             * Nothing new, just received a heartbeat from an existing client,
             * and the server is not MIA.
             */
            heartbeatsReceivedInCurrentRound++
        }

        println("LivenessCheckRound: " + livenessCheckRound)
        println("livenessCheckRoundSinceServerLastSeen: " + livenessCheckRoundSinceServerLastSeen)
        println("heartbeatsReceivedInCurrentRound: " + heartbeatsReceivedInCurrentRound)
        println("clientsMap.size: " + clientsMap.size)
        println("currentServerLivenessStatus: " + currentServerLivenessStatus)
    }

    fun iAmSuccessor(): Boolean {

        /**
         * verdict:
         *
         * tue = I hold the next highest userId
         * false = someone else holds the next highest userId
         * */
        var verdict: Boolean = false

        /**First, remove the currentServer's userId from the map*/
        clientsMap.remove(currentServerId)

        /**Now, taking all of the remaining cliendId values and converting them into an Array*/
        val clientIds = clientsMap.keys

        println("list if clientIds: " + clientIds)

        /**Identify the highest Id, it will be the successor*/
        var successorId = clientIds.max()
        var successorUserName = clientsMap.get(successorId)

        println("THE SUCCESSOR SHOULD BE: \n ID: " + successorId + " \n UserName: " + successorUserName)

        /**If the successorId matches this client's userId, this userId is the election winner.*/
        if(userId.equals(successorId)) {
            verdict = true
        }

        return verdict
    }

    //calculates the appropriate liveness status fr the server
    private fun determineServerLivenessStatus(): LivenessStatus {
        var verdict: LivenessStatus? = null
        var roundsSinceServerLastSeen = livenessCheckRound - livenessCheckRoundSinceServerLastSeen

        //A full iteration of liveness checks have occured and the server did not send one.
        //Increment the `livenessCheckRound` count. eventually, this livenessCheckRound will
        //cross the LIVENESS_THRESHOLD, triggering a new election
        if(heartbeatsReceivedInCurrentRound % clientsMap.size == 0) {
            livenessCheckRound++
        }

        if(roundsSinceServerLastSeen < LIVENESS_THRESHOLD) {
            /**
             * The server has been MIA for a while, but not long enough to meet the LIVENESS_THRESHOLD.
             * Set the LivenessStatus to YELLOW to indicate that something's fishy, but not so fishy
             * as to warrant an election.
             * */
            verdict = LivenessStatus.YELLOW
        } else {
            /**
             * The server has been MIA for the max LIVENESS_THRESHOLD iterations limit. At this point, the
             * server is probably gone, and elections need to be had.
             * */
            verdict = LivenessStatus.RED
        }

        return verdict
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val question = data?.getParcelableExtra("question") as MultipleChoiceQuestion
                questionRepo.insertQuestion(question)
                Thread(Runnable {
                    repository?.insertQuestion(question)
                }).start()

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
                Thread(Runnable{
                    for (client in clients) {
                        UDPClient().sendMessage(json, client.ip, client.port)
                    }
                }).start()
            }
        }
        if (requestCode == 3){
            if (resultCode == Activity.RESULT_OK){
                val questionToActivate = data?.getParcelableExtra("question") as MultipleChoiceQuestion
                val jsonTree = gson.toJsonTree(questionToActivate).also{
                    it.asJsonObject.addProperty("type", "multiple_choice_question")
                }
                val json = gson.toJson(jsonTree)
                Thread(Runnable{
                    for (client in clients){
                        UDPClient().sendMessage(json, client.ip, client.port)
                    }
                }).start()
            }
        }
    }
}
