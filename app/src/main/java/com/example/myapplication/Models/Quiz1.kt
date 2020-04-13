package com.example.myapplication.Models

import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

data class Quiz1(val quiz_id: UUID, val quiz_name: String, var questions: ArrayList<MultipleChoiceQuestion1>) {
    val type: String = "quiz" //Will always be this value

    /**
     * Generates a JSON-formatted string.  Usefor for serialization and deserialization.
     */
    override fun toString(): String{
        val gson = Gson()
        return gson.toJson(this);
    }
}
