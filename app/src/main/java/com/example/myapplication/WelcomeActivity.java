package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.Models.UserType;

public class WelcomeActivity extends AppCompatActivity {

    //to be used to determine which button the user selected
    boolean isJoinSessionButtonSelected = false;
    boolean isCreateSessionButtonSelected = false;

    //Listing relevant Activity buttons and variables
    private EditText userNameEditText;
    private String userName;
    private Button createSessionButton, joinSessionButton, submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        /**
         * Mapping member variable buttons to the appropriate UI Button components.
         * The `userName` variable will be set when the `submitButton` gets clicked;
         * so their default values are left as null for now.
         * */
        createSessionButton = (Button)findViewById(R.id.welcomeActivity_createSessionButton);
        joinSessionButton = (Button)findViewById(R.id.welcomeActivity_joinSessionButton);
        submitButton = (Button)findViewById(R.id.welcomeActivity_submitButton);
        userNameEditText = (EditText)findViewById(R.id.welcomeActivity_usernameEditText);

        //setup onClick listeners for all buttons
        setupOnClickListeners();
    }

    /**
     * A wrapper function for configuring all of the
     * button clicks for this activity.
     */
    private void setupOnClickListeners() {

        /**
         * onClick configuration for the `createSessionButton`.
         * When the user clicks on this button, the button's
         * color will change to turquoise to indicate that it has
         * been selected.
         */
        createSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /**
                 * First, check to see if a `isJoinSessionButtonSelected` has been selected.
                 * If so, remove the turquoise coloring of the `joinSessionButton`.
                 * */
                if(isJoinSessionButtonSelected) {
                    joinSessionButton.setBackgroundResource(android.R.drawable.btn_default);
                }

                /***
                 * Now, set the `createSessionButton` color to turquoise as a visual cue that
                 * this button has been selected.
                 **/
                createSessionButton.setBackgroundColor(getResources().getColor(R.color.turquoise));

                //Finally, set the value of `isCreateSessionButtonSelected` to true.
                isCreateSessionButtonSelected = true;
            }
        });


        /**
         * onClick configuration for the `joinSessionButton`.
         *
         * When the user clicks on this button, the button's
         * color will change to turquoise to indicate that it
         * has been selected.
         */
        joinSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /**
                 * First, check to see if a `isCreateSessionButtonSelected` set to true.
                 * If so, remove the turquoise coloring of the `createSessionButton`.
                 * */
                if(isCreateSessionButtonSelected) {
                    createSessionButton.setBackgroundResource(android.R.drawable.btn_default);
                }

                //Now, set the color of the `joinSessionButton` to turquoise
                joinSessionButton.setBackgroundColor(getResources().getColor(R.color.turquoise));

                //Finally, set the value of `isJoinSessionButtonSelected` to true
                isJoinSessionButtonSelected = true;
            }
        });

        /**
         * Extracts the user's desired username from the `userNameEditText` component.
         * Depending on which button the user selected (joinSessionButton, or
         * createSessionButton), the appropriate Activity will be opened.
         */
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userName = userNameEditText.getText().toString();
                Intent nextActivity;

                //Ensure a non-null username and userType have been created:
                if(isAllFieldsPopulated()) {
                    Toast.makeText(getApplicationContext(),"Welcome, " + userName,Toast.LENGTH_SHORT).show();

                    /**
                     * Identify which activity needs to be opened. This is determined by
                     * looking at what button the user selected (`joinSessionButton` or
                     * `createSessionButton`)
                     */
                    if(isJoinSessionButtonSelected) {

                        /**
                         * TODO: On the line below, 'MainActivity.class' needs to be replaced by a
                         *  soon-to-be created Activity for joining a new session.
                         */
                        nextActivity = new Intent(WelcomeActivity.this, MainActivity.class);

                        //Passing in "extra" data to the mainActivity for use by the MainActivity
                        nextActivity.putExtra("EXTRA_USER_NAME", userName);
                    } else {

                        /**
                         * TODO: On the line below, 'MainActivity.class' needs to be replaced by a
                         *  soon-to-be created Activity for creating a new session.
                         */
                        nextActivity = new Intent(WelcomeActivity.this, MainActivity.class);

                        //Passing in "extra" data to the mainActivity for use by the MainActivity
                        nextActivity.putExtra("EXTRA_USER_NAME", userName);
                    }

                    //Start the nextActivity
                    startActivity(nextActivity);
                } else {
                    //Let the user know they screwed up.
                    Toast.makeText(getApplicationContext(),"Whoops! Please specify your User Type, and provide a non-empty username.",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Ensures that the user populated enough information to derive
     * a `userName` and has selected either the `createSessionButton`
     * or the `joinSessionButton`.
     *
     * @return true when non-null `username` has been provided, and
     * either the joinSession or createSession button has been selected.
     */
    public boolean isAllFieldsPopulated() {
        boolean verdict = false;

        if(userName != null) {
            if(isJoinSessionButtonSelected || isCreateSessionButtonSelected) {
                verdict = true;
            }
        }

        return verdict;
    }
}
