package com.example.myapplication.Activity

import android.app.ActionBar
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import android.widget.RadioGroup
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
//    private var activateQuizButton: Button? = null
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
        if(UserType.CLIENT.equals(userType)) {
            findViewById<Button>(R.id.generate_connection_qr_button).setVisibility(View.GONE)
            findViewById<Button>(R.id.browse_questions).setVisibility(View.GONE)
//            findViewById<Button>(R.id.activateQuizButton).setVisibility(View.GONE)
            findViewById<Button>(R.id.activateNextQuestionButton).setVisibility(View.GONE)
        }
        else {
            findViewById<Button>(R.id.mainActivity_submitAnswerButton).setVisibility(View.GONE)
        }
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

        generateConnectionQrButton = findViewById(R.id.generate_connection_qr_button)
        networkInformation = NetworkInformation.NetworkInfoFactory.getNetworkInfo(this)
//        val activateQuizButton = findViewById<Button>(R.id.activateQuizButton)
        val submitActiveQuestionAnswerButton = findViewById<Button>(R.id.mainActivity_submitAnswerButton)
        val activateNextQuestionButton = findViewById<Button>(R.id.activateNextQuestionButton)
        val browseQuestionsBtn = findViewById<Button>(R.id.browse_questions)

        val dataaccess = QuizDatabase.getDatabase(this)
        val responseID = UUID.randomUUID().toString()
        repository = RepositoryImpl(dataaccess.questionDao(), dataaccess.responseDao(), dataaccess.userDao(), dataaccess.quizDao())

        currentQuestionPromptTextView = findViewById<TextView>(R.id.mainActivity_activeQuestionPromptTextView)

        Timer("Heartbeat", false).schedule(100, 30000){
            emitHeartBeat()
        }

        val quizId = UUID.randomUUID().toString()

//        activateQuizButton.setOnClickListener{
//            //Ony the server has the power to change the active question
//            if(UserType.SERVER.equals(userType)) { //guarding against null userType values
//                println("PERMISSION TO ACTIVATE QUESTION GRANTED, " + userName)
//                activateQuestion(listOfQuizQuestions!!.get(CURRENT_QUESTION_INDEX))
//                //Updating the UI:
//                var newActiveQuestion = listOfQuizQuestions!!.get(CURRENT_QUESTION_INDEX)
//
//                var newActiveQuestionPrompt = newActiveQuestion.prompt
//                var newActiveQuestionChoicesList = newActiveQuestion.choices
//
//                val rgp = findViewById<View>(R.id.choicesRadioGroup) as RadioGroup
//                var rprms: RadioGroup.LayoutParams
//                for(choice in newActiveQuestionChoicesList) {
//                    val rbn = RadioButton(this)
//                    rbn.setText(choice)
//                    rbn.id = View.generateViewId()
//                    rprms = RadioGroup.LayoutParams(
//                        ActionBar.LayoutParams.WRAP_CONTENT,
//                        ActionBar.LayoutParams.WRAP_CONTENT
//                    )
//                    rgp.addView(rbn, rprms)
//                }
//
//                currentQuestionPromptTextView?.setText(newActiveQuestionPrompt)
//
//                CURRENT_QUESTION_INDEX++
//
//                //callActivateQuestion method
//                activateQuestion(newActiveQuestion)
//
//            } else {
//                println("YOU DONT HAVE PERMISSION TO ACTIVATE A NEW QUESTION, " + userName)
//            }
//        }

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

                val rgp = findViewById<View>(R.id.choicesRadioGroup) as RadioGroup
                rgp.removeAllViews()
                var rprms: RadioGroup.LayoutParams
                for(choice in newActiveQuestionChoicesList) {
                    val rbn = RadioButton(this)
                    rbn.setText(choice)
                    rbn.id = View.generateViewId()
                    rprms = RadioGroup.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT
                    )
                    rgp.addView(rbn, rprms)
                }

                currentQuestionPromptTextView?.setText(newActiveQuestionPrompt)

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

                //intent.putExtra("EXTRA_ALL_RESPONSES_LIST", allResponsesList)
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
            val rgp = findViewById<RadioGroup>(R.id.choicesRadioGroup)
            val submittedAnswer = findViewById<EditText>(rgp.checkedRadioButtonId).text.toString()
//            val submittedAnswer = "hello"
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
//                    updateCurrentActiveQuestionUI(multipleChoiceQuestion)
                }
            }
            if ("hb" == type){
                onHeartBeat(message as HeartBeat)
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
    }




    /**
     * Iterates through the list of UDP clients and emits a `heartbeat`
     * toast message in each of the individual devices.
     */
    private fun emitHeartBeat(){
        println("Emitting heartbeat")
        Thread(Runnable{
            for (client in clients){
                val heartbeat = HeartBeat(ip = networkInformation!!.ip, port = networkInformation!!.port.toString())
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
        println(clientMonitor)
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
