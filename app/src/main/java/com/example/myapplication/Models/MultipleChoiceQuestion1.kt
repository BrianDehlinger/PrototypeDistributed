package com.example.myapplication.Models

import com.google.gson.Gson
import java.util.*

data class MultipleChoiceQuestion1 (
    val quiz_id: UUID, //TODO: Should be a long or UUID
    val question_id: UUID, //TODO: Should be a long or UUID
    val prompt: String,
    val choices: List<String>,
    val answer: String
) : java.io.Serializable {

    val type: String = "multiple_choice_question" //Will always be this value

    override fun toString(): String{
        val gson = Gson()
        return gson.toJson(this);
    }
}