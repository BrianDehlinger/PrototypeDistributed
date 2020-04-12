package com.example.myapplication.Activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.example.myapplication.Models.MultipleChoiceQuestion
import com.example.myapplication.Models.MultipleChoiceResponse
import com.example.myapplication.R

class AnswerQuestionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_question)
        val promptView = findViewById<TextView>(R.id.active_question_prompt)
        val spinner = findViewById<Spinner>(R.id.active_question_spinner)
        val submitButton = findViewById<Button>(R.id.record_response)

        val question = intent.getParcelableExtra<MultipleChoiceQuestion>("active_question")
        promptView.text = question?.prompt
        val choices = question!!.choices
        val choicesAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, choices)
        choicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = choicesAdapter

        submitButton.setOnClickListener{
            val answer = spinner.selectedItem as String
            val returnIntent = Intent()
            val response = MultipleChoiceResponse(response_id = intent.getStringExtra("response_id") as String, parent_question_id = question.question_id, quiz_id = intent.getStringExtra("quiz_id") as String, user_id = intent.getStringExtra("user_id") as String, answer = answer)
            returnIntent.putExtra("response", response)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }
}
