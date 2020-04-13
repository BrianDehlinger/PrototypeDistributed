package com.example.myapplication

import android.net.Network
import com.example.myapplication.Models.HeartBeat
import com.example.myapplication.Models.MultipleChoiceQuestion
import com.example.myapplication.Models.MultipleChoiceResponse
import com.example.myapplication.Models.User
import com.example.myapplication.Networking.JoinRequest
import com.example.myapplication.Networking.NetworkInformation
import com.google.gson.Gson

class GSONConverter(val gson: Gson = Gson()){
    // Store the JSON directly is probably more efficient. We might want to do speed tests.
    fun convertToClass(type: String, json_string: String): Any?{
        val returnValue = null
        when(type){
            "multiple_choice_question" -> return gson.fromJson(json_string, MultipleChoiceQuestion::class.java)
            "multiple_choice_response" -> return gson.fromJson(json_string, MultipleChoiceResponse::class.java)
            "user" -> return gson.fromJson(json_string, User::class.java)
            "hb" -> return gson.fromJson(json_string, HeartBeat::class.java)
            "failure_detected" -> return gson.fromJson(json_string, NetworkInformation::class.java)
            "network_info" -> return gson.fromJson(json_string, NetworkInformation::class.java)
            "join_request" -> return gson.fromJson(json_string, JoinRequest::class.java)
        }
        return returnValue
    }
}