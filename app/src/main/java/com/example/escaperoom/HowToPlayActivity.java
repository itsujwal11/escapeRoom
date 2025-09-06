package com.example.escaperoom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HowToPlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);

        RecyclerView recyclerView = findViewById(R.id.instructionRecyclerView);
        Button backButton = findViewById(R.id.backButton);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<InstructionItem> instructionList = new ArrayList<>();
        instructionList.add(new InstructionItem("Getting Started", "Click 'START GAME' to begin your escape room adventure."));
        instructionList.add(new InstructionItem("Room 1", "Solve the puzzle by dragging pieces into the correct order within 60 seconds to earn points."));
        instructionList.add(new InstructionItem("Room 2", "Find the key code from the room and solve the Tower of Hanoi puzzle to proceed."));
        instructionList.add(new InstructionItem("Scoring", "Earn 10 points for each solved puzzle and see your final score upon completion."));
        instructionList.add(new InstructionItem("Completion", "Finish both rooms to view your score and play again!"));

        InstructionAdapter adapter = new InstructionAdapter(instructionList);
        recyclerView.setAdapter(adapter);

        // Back button action
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(HowToPlayActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}