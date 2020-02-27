package com.example.myapplication.DAOs

import android.content.Context
import com.example.myapplication.*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Followed the tutorial on https://codelabs.developers.google.com/codelabs/android-room-with-a-view-kotlin/#6

@Database(entities = arrayOf(MultipleChoiceQuestion::class, MultipleChoiceResponse::class, Quiz::class, User::class), version = 1)
@TypeConverters(DataTypeConverters::class)
abstract class QuizDatabase: RoomDatabase(){

    abstract fun questionDao(): QuestionDao
    abstract fun responseDao(): ResponseDao
    abstract fun userDao(): UserDao

    companion object {

        @Volatile
        private var INSTANCE: QuizDatabase? = null

        fun getDatabase(context: Context): QuizDatabase{
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_database").build()
                INSTANCE = instance
                return instance
            }
        }
    }
}