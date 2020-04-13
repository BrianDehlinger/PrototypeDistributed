package com.example.myapplication.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.myapplication.Models.UserType;
import com.example.myapplication.R;

import java.io.Serializable;

public class JoinQuizActivity extends AppCompatActivity {

    private static String userName;
    private String sessionId;

    private Button submitButton;
    private TextView sessionIdEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_quiz);

        userName = getIntent().getStringExtra("EXTRA_USER_NAME");
        initializeViewComponents();
        setupOnClickListeners();
    }

    private void initializeViewComponents() {
        sessionIdEditText = findViewById(R.id.joinQuizActivity_sessionIdEditText);
        submitButton = findViewById(R.id.joinQuizActivity_submitButton);
    }

    private void setupOnClickListeners() {
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //store the sessionID provided by the user
                sessionId = sessionIdEditText.getText().toString();
                launchMainActivity();
            }
        });
    }

    private void launchMainActivity() {
        Intent nextActivity = new Intent(JoinQuizActivity.this,
                MainActivity.class);

        //Passing in "extra" data to the nextActivity
        nextActivity.putExtra("EXTRA_SESSION_ID", sessionId);
        nextActivity.putExtra("EXTRA_USER_NAME", userName);
        nextActivity.putExtra("EXTRA_USER_TYPE", UserType.CLIENT);

        //start the nextActivity
        startActivity(nextActivity);
    }
}
