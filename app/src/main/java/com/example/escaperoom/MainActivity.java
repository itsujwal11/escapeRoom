package com.example.escaperoom;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        // IMPORTANT: this ID must match activity_main.xml (it's "startButton" there)
        Button startButton = findViewById(R.id.startGameButton);
        Button howToPlayButton = findViewById(R.id.howToPlayButton);

        startButton.setOnClickListener(v -> showUsernameDialog());

        howToPlayButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, HowToPlayActivity.class));
            // DO NOT finish() — let users come back with Back
        });
    }

    private void showUsernameDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_username, null);
        EditText usernameInput = dialogView.findViewById(R.id.usernameInput);
        Button nextButton = dialogView.findViewById(R.id.nextButton);
        Button guestButton = dialogView.findViewById(R.id.guestButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)                // allow back button to close
                .create();
        dialog.setCanceledOnTouchOutside(true);     // allow tap outside to close

        nextButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save and go to RoomActivity
            prefs.edit().putString("USERNAME", username).apply();
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, RoomActivity.class);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
            // DO NOT finish(); so Back returns to this main screen
        });

        guestButton.setOnClickListener(v -> {
            prefs.edit().putString("USERNAME", "Guest").apply();
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, RoomActivity.class);
            intent.putExtra("USERNAME", "Guest");
            startActivity(intent);
            // DO NOT finish();
        });

        dialog.show();
    }
}
