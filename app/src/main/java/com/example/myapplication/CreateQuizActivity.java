package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CreateQuizActivity extends AppCompatActivity {

    //Instantiating, but not initializing, member variables
    private String userName, quizName;

    //UI Component references
    private EditText quizNameEditText;
    private Button createQuizButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        //Initialize all variables
        initializeMemberVariables();

        //set up all onClick listeners
        setupOnClickListeners();
    }

    /**
     * Initializing the values for all relevant member variables,
     * including all relevant UI components.
     */
    private void initializeMemberVariables() {
        //The userName is passed in from the previous activity as "EXTRA_USER_NAME"
        userName = getIntent().getStringExtra("EXTRA_USER_NAME");

        //initializing the references to all relevant UI components
        quizNameEditText = (EditText)findViewById(R.id.createQuizActivity_quizNameEditText);
        createQuizButton = (Button)findViewById(R.id.createQuizActivity_createQuizButton);
    }

    /**
     * A wrapper method for neatly encapsulating all onClick listener
     * configurations for all of the buttons belonging to this activity.
     */
    private void setupOnClickListeners() {

        /**
         * OnClick configuration for the `createQuizButton`.
         */
        createQuizButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Store the quizName -- as defined by the user
                quizName = extractQuizName();

                /**
                 * Set the `nextActivity` to be the `CreateQuizActivity`
                 */
                Intent nextActivity = new Intent(CreateQuizActivity.this,
                        CreateQuestionActivity.class);

                //Passing in "extra" data to the nextActivity
                nextActivity.putExtra("EXTRA_USER_NAME", userName);
                nextActivity.putExtra("EXTRA_QUIZ_NAME", quizName);

                //start the nextActivity
                startActivity(nextActivity);
            }
        });
    }

    /**
     * A method to extracting the user-defined quizName.
     * TODO: Sanitation and non-null logic required.
     *
     * @return The quizName, as specified by the user.
     */
    private String extractQuizName() {
        return quizNameEditText.getText().toString();
    }
}
