package com.example.myapplication.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.Models.MultipleChoiceQuestion
import com.example.myapplication.R
import java.util.*


// https://www.youtube.com/watch?v=nlqtyfshUkc was used to help with this implementation as was https://www.androidly.net/309/android-alert-dialog-using-kotlin

class CreateQuestionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_question)
        val layout = findViewById<LinearLayout>(R.id.create_question)
        val addChoiceButton = findViewById<Button>(R.id.add_choice)
        val submitButton = findViewById<Button>(R.id.submit_question)
        val promptView = findViewById<EditText>(R.id.prompt)
        var numberOfChoices = 0
        val choicesEditText = arrayListOf<EditText>()
        var answer = ""

        addChoiceButton.setOnClickListener {
            if (numberOfChoices == 6) {
                Toast.makeText(applicationContext, "Only supports six choices", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val editText = EditText(this)
                layout.addView(editText)
                numberOfChoices++
                choicesEditText.add(editText)
            }
        }

        submitButton.setOnClickListener {
            val choices = arrayListOf<String>()
            for (editChoice in choicesEditText) {
                choices.add(editChoice.text.toString())
            }
            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_spinner, null)
            builder.setTitle("Please select the correct answer to the question")
            val spinner = view.findViewById<Spinner>(R.id.spinner)
            val spinnerArrayAdapter =
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, choices)
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = spinnerArrayAdapter
            builder.setPositiveButton("OK") { dialog, which ->
                answer = spinner.selectedItem as String
                val prompt_text = promptView.text.toString()
                val quizId = intent.getStringExtra("quizID")
                val question = MultipleChoiceQuestion(answer=answer, prompt = prompt_text, quiz_id = quizId, choices = choices, question_id = UUID.randomUUID().toString())
                dialog.dismiss()
                val returnIntent = Intent()
                returnIntent.putExtra("question", question)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
            builder.setNegativeButton("Dismiss"){dialog, which ->
                dialog.dismiss()
            }
            builder.setView(view)
            builder.create().show()
        }
    }
}
