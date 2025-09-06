package com.example.escaperoom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameCompleteActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_complete);

        sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        String username = getIntent().getStringExtra("USERNAME");
        int finalScore = getIntent().getIntExtra("SCORE", 0);

        TextView completionMessage = findViewById(R.id.completionMessage);
        Button playAgainButton = findViewById(R.id.playAgainButton);

        completionMessage.setText(String.format(
                "Congratulations, %s!\nYou escaped all rooms!\nFinal Score: %d",
                username, finalScore
        ));

        playAgainButton.setOnClickListener(v -> {
            // Reset game progress
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("PUZZLE_SOLVED_" + username, false);
            editor.putInt("SCORE_" + username, 0);
            editor.apply();

            // Return to main menu
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Optional: Prevent going back to the game
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}