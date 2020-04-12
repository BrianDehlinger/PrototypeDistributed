package com.example.myapplication.Models

import com.google.gson.Gson

data class Quiz1(val quiz_id: String, val quiz_name: String, var questions: ArrayList<MultipleChoiceQuestion1>) {
    val type: String = "quiz" //Will always be this value

    /**
     * Generates a JSON-formatted string.  Usefor for serialization and deserialization.
     */
    override fun toString(): String{
        val gson = Gson()
        return gson.toJson(this);
    }
}
