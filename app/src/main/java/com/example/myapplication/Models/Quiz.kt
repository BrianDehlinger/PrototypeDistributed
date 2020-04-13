package com.example.myapplication.Models

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

// Defined a non used secondary constructor since this is needed by Room.
// Ideally, in a future release of Room we can remove the secondary constructor
@Parcelize
@Entity(indices = [Index(value=["quiz_name"], unique = true), Index(value=["quiz_id"], unique = true)])
data class Quiz(
    @PrimaryKey val quiz_id: String,
    val quiz_name : String,
    @Ignore var questions: List<MultipleChoiceQuestion> = emptyList(),
    @Ignore var type: String = "quiz"
): Parcelable{
    constructor(quiz_id: String, quiz_name: String) : this(quiz_id, quiz_name, arrayListOf<MultipleChoiceQuestion>())
}

