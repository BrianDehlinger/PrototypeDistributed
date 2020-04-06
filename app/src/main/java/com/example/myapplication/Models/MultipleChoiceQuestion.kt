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
    val quiz_id: String
) : Parcelable