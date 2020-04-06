package com.example.myapplication.Models

import androidx.room.PrimaryKey

data class MultipleChoiceQuestion1 (
    val quiz_id: String, //TODO: Should be a long or UUID
    val question_id: String, //TODO: Should be a long or UUID
    val prompt: String,
    val choices: List<String>,
    val answer: String
) : java.io.Serializable {

    val type: String = "multiple_choice_question" //Will always be this value

    override fun toString(): String{
        //TODO: This can/should be replaced by the GSON library's toString() for converting an
        // object to a JSON-representation, since we're already using GSON
        val multipleChoiceQuestionAsString
                = "{type: ${type}, quizId: ${quiz_id}, choices: ${choices}," +
                "questionId: ${question_id}, prompt: '${prompt}', answer: '${answer}'}"
        return multipleChoiceQuestionAsString
    }
}