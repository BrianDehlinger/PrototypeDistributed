package com.example.myapplication.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.example.myapplication.Models.MultipleChoiceQuestion;
import com.example.myapplication.Models.MultipleChoiceQuestion1;
import com.example.myapplication.Models.Quiz1;
import com.example.myapplication.R;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateQuizActivity extends AppCompatActivity {

    //Instantiating, but not initializing, member variables
    private static String userName, quizName;

    //UI Component references
    private EditText quizNameEditText;
    private Button createQuizButton;

    ListView listview;
    Button addNewQuestionButton;

    private static ArrayList<MultipleChoiceQuestion1> listOfQuizQuestions
            = new ArrayList<MultipleChoiceQuestion1>();

    private static List<String> listOfQuestionPrompts
            = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        //Initialize all variables
        initializeMemberVariables();

        findViewsByIds();

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>
                (CreateQuizActivity.this, android.R.layout.simple_list_item_1, listOfQuestionPrompts);

        MultipleChoiceQuestion1 newMultipleChoiceQuestion1;

        if(quizName != null) {
            quizNameEditText.setText(quizName);
        }

        if(getIntent().getStringExtra("EXTRA_USER_NAME") != null) {
            //The userName is passed in from the previous activity as "EXTRA_USER_NAME"
            userName = getIntent().getStringExtra("EXTRA_USER_NAME");
            System.out.println("Username set to: " + userName);
        }

        if(getIntent().getStringExtra("newQuestion") != null) {
            Gson gson = new Gson();
            newMultipleChoiceQuestion1 = gson.fromJson(getIntent().getStringExtra("newQuestion"),
                    MultipleChoiceQuestion1.class);

            listOfQuizQuestions.add(newMultipleChoiceQuestion1);
            System.out.println("List of questions is of size: " + listOfQuizQuestions.size());

            //store the new prompt to be displayed in the ListView
            listOfQuestionPrompts.add(newMultipleChoiceQuestion1.getPrompt());

            //store the entire multipleChoiceQuestion to be used when the quiz is activated.
            listOfQuizQuestions.add(newMultipleChoiceQuestion1);

            //notify the ListView that a new item (question prompt) has been added to the list
            arrayAdapter.notifyDataSetChanged();
        }

        //Linking the arrayAdapter to the ListView
        listview.setAdapter(arrayAdapter);

        //set up all onClick listeners
        setupOnClickListeners();
    }

    /**
     * Initializing the values for all relevant member variables,
     * including all relevant UI components.
     */
    private void initializeMemberVariables() {
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

                /**
                 * Set the `nextActivity` to be the `CreateQuizActivity`
                 */
                //TODO: Define logic for when the user has finished creating all of the quiz's questions and is ready to start the session.

                Intent nextActivity = new Intent(CreateQuizActivity.this,
                        MainActivity.class);

                //Passing in "extra" data to the nextActivity
                nextActivity.putExtra("EXTRA_USER_NAME", userName);
                nextActivity.putExtra("EXTRA_QUIZ_NAME", quizName);
                nextActivity.putExtra("EXTRA_LIST_OF_QUIZ_QUESTIONS", (Serializable) listOfQuizQuestions);

                //start the nextActivity
                startActivity(nextActivity);
            }
        });


        addNewQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quizName = extractQuizName();
                Intent intent = new Intent(getApplicationContext(), CreateQuestionActivity.class);
                startActivity(intent);
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

    private void findViewsByIds() {
        listview = findViewById(R.id.listView);
        addNewQuestionButton = findViewById(R.id.addNewQuestionButton);
    }


    private void checkForNewReceivedQuestion() {
        MultipleChoiceQuestion1 newMultipleChoiceQuestion1;

        if(getIntent().getStringExtra("newQuestion") != null) {
            Gson gson = new Gson();
            newMultipleChoiceQuestion1 = gson.fromJson(getIntent().getStringExtra("newQuestion"),
                    MultipleChoiceQuestion1.class);

            listOfQuizQuestions.add(newMultipleChoiceQuestion1);
            System.out.println("List of questions is of size: " + listOfQuizQuestions.size());

            //store the new prompt to be displayed in the ListView
            listOfQuestionPrompts.add(newMultipleChoiceQuestion1.getPrompt());

            //store the entire multipleChoiceQuestion to be used when the quiz is activated.
            listOfQuizQuestions.add(newMultipleChoiceQuestion1);
        }
    }

}
