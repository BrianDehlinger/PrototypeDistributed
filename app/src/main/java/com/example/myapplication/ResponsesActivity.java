package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListView;

import com.example.myapplication.Models.MultipleChoiceQuestion1;
import com.example.myapplication.Models.MultipleChoiceResponse;

import java.util.ArrayList;
import java.util.List;

public class ResponsesActivity extends AppCompatActivity {

    ListView listView;

    private List<MultipleChoiceResponse> allResponsesList = new ArrayList<MultipleChoiceResponse>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_responses);
    }
}
