package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myapplication.Models.MultipleChoiceResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponsesActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener  {

    ListView listView;
    Spinner promptsSpinner;

    //TODO Might be able to delete
    private List<MultipleChoiceResponse> allResponsesList
            = new ArrayList<MultipleChoiceResponse>();

    private List<String> allPrompts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_responses);

        allResponsesList = getIntent()
                .getParcelableArrayListExtra("EXTRA_ALL_RESPONSES_LIST");

        System.out.println("Received allResponsesList size: " + allResponsesList.size());
        System.out.println("Received allResponsesList contents: " + allResponsesList.toString());

        Map<String, MultipleChoiceResponse> promptsAndResponses
                = convertAllResponsesListToHashMap(allResponsesList);

        allPrompts = new ArrayList<String>(promptsAndResponses.keySet());

        //TODO Refactor, move out of this onCreate method.
        promptsSpinner = (Spinner)findViewById(R.id.responsesActivity_promptSelectionSpinner);

        ArrayAdapter<String> adapter
                = new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_item, allPrompts);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        promptsSpinner.setAdapter(adapter);
        promptsSpinner.setOnItemSelectedListener(this);

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
    private Map<String, MultipleChoiceResponse> convertAllResponsesListToHashMap(
            List<MultipleChoiceResponse> list) {

        Map<String, MultipleChoiceResponse> promptAndResponseMap
                = new HashMap<String, MultipleChoiceResponse>();

        for(int i = 0; i < list.size(); i++) {

            //Store the MultipleChoiceResponse object
            MultipleChoiceResponse multipleChoiceResponse = list.get(i);

            //Extract the prompt for the response at the current index. This will be
            //the HashMap Key
            String questionPrompt = multipleChoiceResponse.getPrompt();

            //Store the MultipleChoiceResponse object, associated with this userName
            promptAndResponseMap.put(questionPrompt,multipleChoiceResponse);
        }

        return promptAndResponseMap;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getApplicationContext(), "Selected User: "+ allPrompts.get(position) , Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
