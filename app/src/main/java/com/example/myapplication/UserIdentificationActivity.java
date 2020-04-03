package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.myapplication.Models.UserType;

public class UserIdentificationActivity extends AppCompatActivity {

    //Listing relevant Activity buttons and input values
    private UserType userType;
    private EditText userNameEditText;
    private String userName;
    private Button instructorButton, studentButton, submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_identification);

        /**
        * Mapping member variable buttons to the appropriate UI Button components.
        * The `userName` and `userType` variable values will be set when the
        * `submitButton` gets clicked; so their default values are left as null
        * for now.
        * */
        instructorButton = (Button)findViewById(R.id.instructorButton);
        studentButton = (Button)findViewById(R.id.studentButton);
        submitButton = (Button)findViewById(R.id.submitUserDetailsButton);
        userNameEditText = (EditText)findViewById(R.id.usernameEditText);

        //setup onClick listeners for all buttons
        setupOnClickListeners();
    }

    /**
     * A wrapper function for configuring all of the
     * button clicks for this activity.
     */
    private void setupOnClickListeners() {

        /**
         * onClick configuration for the `instructorButton`.
         * When the user clicks on this button, the button's
         * color will change to turquoise to indicate that it has
         * been selected.
         */
        instructorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /**
                 * First, check to see if a `userType` has been set to STUDENT.
                 * If so, remove the turquoise coloring of the `studentButton`.
                 * */
                if(UserType.STUDENT.equals(userType)) {
                    studentButton.setBackgroundResource(android.R.drawable.btn_default);
                }

                //Now, set the instructorButton color to turquoise
                instructorButton.setBackgroundColor(getResources().getColor(R.color.turquoise));

                //Finally, set the value of `userType` to UserType.INSTRUCTOR
                userType = UserType.INSTRUCTOR;
            }
        });


        /**
         * onClick configuration for the `studentButton`.
         *
         * When the user clicks on this button, the button's
         * color will change to turquoise to indicate that it
         * has been selected.
         */
        studentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /**
                 * First, check to see if a `userType` has been set to INSTRUCTOR.
                 * If so, remove the turquoise coloring of the `instructorButton`.
                 * */
                if(UserType.INSTRUCTOR.equals(userType)) {
                    instructorButton.setBackgroundResource(android.R.drawable.btn_default);
                }

                //Now, set the color of the studentButton to turquoise
                studentButton.setBackgroundColor(getResources().getColor(R.color.turquoise));

                //Finally, set the value of `userType` to UserType.STUDENT
                userType = UserType.STUDENT;
            }
        });

        /**
         * Extracts the user's desired username from the `userNameEditText` component.
         */
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userName = userNameEditText.getText().toString();
            }
        });
    }
}
