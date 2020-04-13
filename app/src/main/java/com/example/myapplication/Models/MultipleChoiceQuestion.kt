package com.example.myapplication.Models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Parcelize
@Entity
data class MultipleChoiceQuestion (
    @PrimaryKey val question_id: String,
    val answer: String,
    val choices: List<String>,
    val prompt: String,
    val quiz_id: String,
    @Ignore val type: String = "multiple_choice_question"
) : Parcelable{
    constructor(question_id: String, answer: String, choices: List<String>, prompt: String, quiz_id: String): this(question_id, answer, choices, prompt, quiz_id, type="multiple_choice_question")
}