package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Network
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.DAOs.Cache
import com.example.myapplication.DAOs.QuizDatabase
import com.example.myapplication.DAOs.RepositoryImpl
import com.example.myapplication.Models.*
import com.example.myapplication.Networking.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.WriterException
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_user_identification.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.schedule


// https://demonuts.com/kotlin-generate-qr-code/ was used for the basis of  QRCode generation and used pretty much all of the code for the QR methods. Great thanks to the authors!
class MainActivity : AppCompatActivity() {
    lateinit var session: Session
    var userName: String? = null
    val converter = GSONConverter()
    val gson = Gson()
    val debug = true
    val contextReference: Context = this
    val debugProviders: DebugProviders = DebugProviders(this)


    lateinit var ringLeader: NetworkInformation
    lateinit var otherReplicas: MutableList<NetworkInformation>

    // The current active question if any

    // Thread pools
    val messageSenders: ExecutorService = Executors.newFixedThreadPool(15)
    val userInterfaceThreads: ExecutorService = Executors.newFixedThreadPool(2)

    // The in memory object cache!
    private val questionRepo = Cache()

    // The actual persisted database!
    private var repository: RepositoryImpl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //TODO: print statements are sloppy. Make a logger.
        var typeOfUser = intent.getSerializableExtra("EXTRA_USER_TYPE").toString()
        var userName = intent.getStringExtra("EXTRA_USER_NAME")

        // BRIAN ADD
        var isReplica = intent.getBooleanExtra("IS_REPLICA", false)
        var ringLeaderJson = intent.getStringExtra("RING_LEADER")
        var otherReplicasJSON = intent.getStringExtra("OTHER_REPLICAS")
        var peerId = getIntent().getIntExtra("PEER_ID", 0)
        if (debug == true){
            ringLeaderJson = gson.toJson(debugProviders.provideRingLeader())
            isReplica = debugProviders.provideIsReplica(this)
            otherReplicasJSON = gson.toJson(debugProviders.provideOtherReplicas(this))
            peerId = debugProviders.providePeerId(this)
        }
        ringLeader = gson.fromJson(ringLeaderJson, NetworkInformation::class.java)
        val customType = object : TypeToken<MutableList<NetworkInformation>>() {}.type
        otherReplicas = gson.fromJson(otherReplicasJSON, customType)

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
        val connectWithQRButton = findViewById<Button>(R.id.connect_qr)

        connectWithQRButton.setOnClickListener {
            println("HERE")
            val integrator = IntentIntegrator(this)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        browseQuestionsButton.setOnClickListener {
            val intent = Intent(this, BrowseQuestions::class.java)
            startActivityForResult(intent, 3)
        }

        val responseID = UUID.randomUUID().toString()

        answerQuestionButton.setOnClickListener {
            userInterfaceThreads.execute(Thread(Runnable {
                Intent(this, AnswerQuestionActivity::class.java).also {
                    if (session.activeQuestion == null) {
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
                        val quiz = Quiz(session.activeQuestion!!.quiz_id, "BobMarley")
                        it.putExtra("active_question", session.activeQuestion)
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

        val dataaccess = QuizDatabase.getDatabase(this)
        repository = RepositoryImpl(
            dataaccess.questionDao(),
            dataaccess.responseDao(),
            dataaccess.userDao(),
            dataaccess.quizDao()
        )

        // Session is created based on previous activity. This could be refactored to a factory method.
        if (typeOfUser == "INSTRUCTOR") {
            println("HERE")
            session = ReplicaSession(contextReference,
                ringLeader,
                peerId = peerId,
                threadPool = messageSenders,
                sessionReplicas = CopyOnWriteArrayList( otherReplicas)
            ).also{
                it.startHB(contextReference)
                it.isRingLeader = true
            }
        } else if (typeOfUser == "STUDENT") {
            if (isReplica) {
                session = ReplicaSession(
                    contextReference,
                    ringLeader,
                    peerId = peerId,
                    threadPool = messageSenders,
                    sessionReplicas = CopyOnWriteArrayList(otherReplicas)
                ).also {
                    it.startHB(contextReference)
                    println("STARTING HB")
                }
            } else {
                session =
                    Session(
                        contextReference,
                        ringLeader,
                        threadPool = messageSenders,
                        sessionReplicas = CopyOnWriteArrayList(otherReplicas)
                    )
            }
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
        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                val response = data?.getParcelableExtra("response") as MultipleChoiceResponse
                questionRepo.insertResponse(response)
                session.sendMessage(gson.toJson(response), ringLeader)
            }
        }
        if (requestCode == 3) {
            if (resultCode == Activity.RESULT_OK && data?.getParcelableExtra<MultipleChoiceQuestion?>(
                    "question"
                ) != null
            ) {
                val questionToActivate =
                    data?.getParcelableExtra("question") as MultipleChoiceQuestion
                println(gson.toJson(questionToActivate))
                if (session.isRingLeader) {
                    session.activateQuestion(questionToActivate)
                }
            }
        }
            if (requestCode == 49374) {
                val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
                val data = scanResult.contents
                println(data)
                if (data != null) {
                    val type = gson.fromJson(data, Map::class.java)["type"] as String
                    if (type == "network_info") {
                        val ringLeader = converter.convertToClass(type, data)
                        println(ringLeader)
                        if (ringLeader!!.javaClass.name == NetworkInformation::class.java.name) {
                            println("HERE")
                            session.setTheRingLeader(ringLeader as NetworkInformation)
                            session.sendMessage(
                                message = gson.toJson(
                                    JoinRequest(
                                        information = DebugProviders(this).provideNetworkInformation(this), peer_type = "replica"
                                    )
                                ), recipient = ringLeader
                            )
                        }
                    }
                }
            }
        }

    override fun onDestroy(){
        super.onDestroy()
        session.close()
    }
}


