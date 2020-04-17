package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.Activity.CreateQuizActivity;
import com.example.myapplication.Models.MultipleChoiceQuestion;
import com.example.myapplication.Models.MultipleChoiceQuestion1;
import com.example.myapplication.Models.MultipleChoiceResponse;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponsesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener  {

    ListView allAnswersListView;
    Spinner promptsSpinner;
    TextView questionReferenceTextView;

    //TODO Might be able to delete
    private List<MultipleChoiceResponse> allResponsesList
            = new ArrayList<MultipleChoiceResponse>();

    private List<MultipleChoiceQuestion1> listOfQuizQuestions;

    private List<String> allPrompts;
    private List<String> userNamesAndAnswers = new ArrayList<String>();

    //---- Setting up listView items:
    private ArrayAdapter<String> userNamesAndResponsesAdapter;

    private Map<String, List<MultipleChoiceResponse>> promptsAndResponses = new HashMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_responses);

        allResponsesList = getIntent()
                .getParcelableArrayListExtra("EXTRA_ALL_RESPONSES_LIST");

        listOfQuizQuestions = (List<MultipleChoiceQuestion1>) getIntent().getSerializableExtra("EXTRA_LIST_OF_QUIZ_QUESTIONS");

        System.out.println("Consumed the following allResponsesList: " + allResponsesList);
        System.out.println("Consumed the following listOfQuestions: " + listOfQuizQuestions);

        promptsAndResponses = convertAllResponsesListToHashMap(allResponsesList);

        allPrompts = new ArrayList<String>(promptsAndResponses.keySet());

        //TODO Refactor, move out of this onCreate method.
        promptsSpinner = (Spinner)findViewById(R.id.responsesActivity_promptSelectionSpinner);
        allAnswersListView = (ListView)findViewById(R.id.responsesActivity_listView);
        questionReferenceTextView = (TextView)findViewById(R.id.responsesActivity_selectedPromptTextView);

        ArrayAdapter<String> adapter
                = new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_item, allPrompts);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        promptsSpinner.setAdapter(adapter);
        promptsSpinner.setOnItemSelectedListener(this);

        userNamesAndResponsesAdapter = new ArrayAdapter<>(ResponsesActivity.this,
                        android.R.layout.simple_list_item_1, userNamesAndAnswers);

        allAnswersListView.setAdapter(userNamesAndResponsesAdapter);
    }

    /**
     * Converts the List<MultipleChoiceResponse> and converts it to a HashMap.  The reasoning
     * for this is to simplify the organization of the received list.  The resulting HashMap
     * with hold the question prompt as the key.  The HashMap values will be
     * the entire MultipleChoiceResponse objects that correspond to the same prompt (key).
     *
     * @param list
     * @return
     */
    private Map<String, List<MultipleChoiceResponse>> convertAllResponsesListToHashMap(
            List<MultipleChoiceResponse> list) {

        System.out.println("Received the following list: " + list.toString());

        Map<String, List<MultipleChoiceQuestion>> promptAndResponseMap
                = new HashMap<String, List<MultipleChoiceQuestion>>();

        for(int i = 0; i < list.size(); i++) {
            //iterate through each MultipleChoiceResponse.
            //For each MultipleChoiceResponse, store a list of
            MultipleChoiceResponse currentMultipleChoiceResponse = list.get(i);
            String currentPrompt = currentMultipleChoiceResponse.getPrompt();

            //check if the `promptsAndResponses` map already has a key entry list
            //for the currentPrompt, if so, add it to the value list, if not, create
            //a new list and set the currentPrompt as the key value.
            if(promptsAndResponses.containsKey(currentPrompt)) {
                promptsAndResponses.get(currentPrompt).add(currentMultipleChoiceResponse);
            } else {
                //key does not yet exist in the `promptsAndResponse` map.
                //Create a new List<MultipleChoiceResponse>, add the current
                //MultipleChoiceResponse to it.
                List<MultipleChoiceResponse> multipleChoiceResponses
                        = new ArrayList<MultipleChoiceResponse>();
                multipleChoiceResponses.add(currentMultipleChoiceResponse);
                promptsAndResponses.put(currentPrompt, multipleChoiceResponses);
            }
        }

        System.out.println("Returning the following map: " + promptsAndResponses.toString());

        return promptsAndResponses;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getApplicationContext(), "Selected Question: "+ allPrompts.get(position),
                Toast.LENGTH_SHORT).show();
        String selectedPrompt = allPrompts.get(position);
        //populate the listView with all of the answers to the specified prompt:
        populateListViewForPrompt(selectedPrompt);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void populateListViewForPrompt(String prompt) {
        //First, clear any existing contents in the `userNamesAndAnswers` -- residuals
        //from any previous loading of the listView.
        userNamesAndAnswers.clear();

        //Find the key with the indicated prompt from the `promptsAndResponses` map,
        //and extract its list of MultipleChoiceResponse object.
        List<MultipleChoiceResponse> allResponsesForPrompt = promptsAndResponses.get(prompt);

        //iterate through ech MultipleChoiceResponse, extracting the userName and the answer
        for(int i = 0; i < allResponsesForPrompt.size(); i++) {
            MultipleChoiceResponse currentMultipleChoiceResponse = allResponsesForPrompt.get(i);
            String concatenatedUserNameAndAnswer
                    = "Username: " + currentMultipleChoiceResponse.getUser_id()
                    + "\n" + "Answered: " + currentMultipleChoiceResponse.getAnswer();

            //store username and answer in the `userNamesAndAnswers` arraylist, since this is what the
            //listview adapter is listening fr changes in:
            userNamesAndAnswers.add(concatenatedUserNameAndAnswer);
        }

        //trigger a refresh of the listview so that the updated data is shown:
        userNamesAndResponsesAdapter.notifyDataSetChanged();
        setQuestionReferenceTextView(prompt);
        System.out.println("listview should now contain the following: " + userNamesAndAnswers);
    }

    private void setQuestionReferenceTextView(String prompt) {
        //find the MultipleChoiceQuestion1 object that matches the prompt:
        for(int i = 0; i < listOfQuizQuestions.size(); i++) {
            MultipleChoiceQuestion1 currentMultipleChoiceQuestion = listOfQuizQuestions.get(i);
            String currentMultipleChoiceQuestionPrompt = currentMultipleChoiceQuestion.getPrompt();
            if(prompt.equals(currentMultipleChoiceQuestionPrompt)) { //match found
                //set the text content of the `questionReferenceTextView`
                String choicesListAsString = "";
                for(int j = 0; j <  currentMultipleChoiceQuestion.getChoices().size(); j++) {
                    String choice = currentMultipleChoiceQuestion.getChoices().get(j);
                    choicesListAsString += "- " + choice + "\n";
                }
                questionReferenceTextView.setText(choicesListAsString);
            }
        }
    }
}
