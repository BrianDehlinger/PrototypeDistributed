package com.example.myapplication.Models

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewServerNotification(
    val newServerUserName: String,
    val newServerUserId: Double,
    val triggeringClientUserName: String,
    val triggeringClientUserId: Double
) : Parcelable {
    val type: String = "new_server_notification"
    /**
     * Generates a JSON-formatted string.  Usefor for serialization and deserialization.
     */
    override fun toString(): String{
        return Gson().toJson(this);
    }
}