package com.example.myapplication.Activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.DAOs.QuizDatabase
import com.example.myapplication.DAOs.RepositoryImpl
import com.example.myapplication.MyAdapter
import com.example.myapplication.R

class BrowseQuestions : AppCompatActivity() {


    // https://developer.android.com/guide/topics/ui/layout/recyclerview
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_questions)
        viewManager = LinearLayoutManager(this)
        val database = QuizDatabase.getDatabase(this)
        val repository = RepositoryImpl(database.questionDao(), database.responseDao(), database.userDao(), database.quizDao())
        val viewButton = findViewById<Button>(R.id.view_questions)
        val exitButton = findViewById<Button>(R.id.return_from_activity)
        val recyclerView = findViewById<RecyclerView>(R.id.question_browser).apply{
            layoutManager = viewManager
        }


        exitButton.setOnClickListener{
            val returnIntent = Intent()
            val adapter = recyclerView.adapter as MyAdapter
            returnIntent.putExtra("question", adapter.getActiveQuestion())
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
        viewButton.setOnClickListener {
                Thread(Runnable {
                    val questions = ArrayList(repository.getAllQuestions())
                    runOnUiThread {
                        recyclerView.adapter =
                            MyAdapter(questions)
                    }
                }).start()
        }


    }
}
