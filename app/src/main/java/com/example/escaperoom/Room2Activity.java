package com.example.escaperoom;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.escaperoom.tohHanoi.HanoiView;

public class Room2Activity extends AppCompatActivity implements HanoiView.GameStateListener {

    private ImageView overlayImage;
    private VideoView drawerVideo;
    private AlertDialog gameDialog;
    private boolean isLampZoomVisible = false;
    private AlertDialog winDialog;
    private HanoiView hanoiView;
    private TextView moveCounter;
    private View lampArea;
    private View keyClickArea;
    private TextView timerTextView;
    private TextView scoreText;
    private ImageView lampZoomOverlay;
    private TextView usernameText;
    private TextView progressText;
    private TextView instructionText;
    private Button backButton;
    private ImageView hintButton;
    private Button nextButton; // optional; may be null depending on your layout
    private Button resetButton;

    private SharedPreferences sharedPreferences;
    private CountDownTimer countDownTimer;
    private long timeElapsed;
    private int score;
    private String username;
    private boolean isImageSwapped = false;
    private boolean gameWon = false;

    // NEW: keep a reference so we can stop music when dialog closes
    private MediaPlayer winMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room2);

        try {
            // prefs & user
            sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);
            username = getIntent().getStringExtra("USERNAME");
            if (username == null) {
                username = sharedPreferences.getString("USERNAME", "Guest");
            }
            score = sharedPreferences.getInt("SCORE_" + username, 0);
            gameWon = sharedPreferences.getBoolean("GAME_WON_" + username, false);

            // bind views
            overlayImage = findViewById(R.id.overlayImage1);
            drawerVideo = findViewById(R.id.drawerVideo);
            scoreText = findViewById(R.id.scoreText);
            usernameText = findViewById(R.id.usernameText);
            progressText = findViewById(R.id.progressText);
            instructionText = findViewById(R.id.instructionText);
            backButton = findViewById(R.id.backButton);
            hintButton = findViewById(R.id.hintButton);
            lampZoomOverlay = findViewById(R.id.lampZoomOverlay);
            keyClickArea = findViewById(R.id.keyClickArea);
            lampArea = findViewById(R.id.lampArea);
            resetButton = findViewById(R.id.resetButton);
            nextButton = findViewById(R.id.nextButton); // may be null (not in your XML)

            // ui init
            if (overlayImage != null) overlayImage.setVisibility(View.GONE);
            if (nextButton != null) nextButton.setEnabled(gameWon);
            if (usernameText != null) usernameText.setText(getString(R.string.welcome_user, username));
            if (scoreText != null) scoreText.setText(getString(R.string.score_label, score));
            if (progressText != null) progressText.setText("Room 2/2");
            if (instructionText != null) instructionText.setText("Note: Search the key from the room and click on glowing drawer.");

            if (gameWon) {
                disablePuzzleRoom2();
                Toast.makeText(this, "You've already completed Room 2!", Toast.LENGTH_SHORT).show();
            }

            View clickableArea = findViewById(R.id.KeyClickableArea);
            if (clickableArea != null) {
                clickableArea.setOnClickListener(v -> {
                    if (!isImageSwapped && overlayImage != null && !gameWon) {
                        playDrawerAnimation();
                    }
                });
            }

            if (lampArea != null) {
                lampArea.setOnClickListener(v -> {
                    if (lampZoomOverlay != null) lampZoomOverlay.setVisibility(View.VISIBLE);
                    if (keyClickArea != null) keyClickArea.setVisibility(View.VISIBLE);
                    isLampZoomVisible = true;
                });
            }

            if (keyClickArea != null) {
                keyClickArea.setOnClickListener(v -> showAlertDialog("Clue Found", "You found the code: 1234"));
            }

            if (overlayImage != null) {
                overlayImage.setOnClickListener(v -> showCodeInputDialog());
            }

            if (backButton != null) {
                backButton.setOnClickListener(v -> onBackPressed());
            }

            if (hintButton != null) {
                hintButton.setOnClickListener(v -> Toast.makeText(this, "The code is 1234", Toast.LENGTH_SHORT).show());
            }

            if (nextButton != null) {
                nextButton.setOnClickListener(v -> {
                    if (!gameWon) {
                        Toast.makeText(this, "Complete the Tower of Hanoi to proceed!", Toast.LENGTH_SHORT).show();
                    } else {
                        navigateToCompletion();
                    }
                });
            }

            if (resetButton != null) {
                resetButton.setOnClickListener(v -> resetGameState());
            }

        } catch (Exception e) {
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void disablePuzzleRoom2() {
        View lampArea = findViewById(R.id.lampArea);
        View keyArea = findViewById(R.id.keyClickArea);
        if (lampArea != null) lampArea.setEnabled(false);
        if (keyArea != null) keyArea.setEnabled(false);
        if (resetButton != null) resetButton.setEnabled(false);
    }

    private void playDrawerAnimation() {
        try {
            if (drawerVideo != null) {
                drawerVideo.setZOrderOnTop(true);
                drawerVideo.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                drawerVideo.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.room2animate));
                drawerVideo.setVisibility(View.VISIBLE);
                drawerVideo.start();
                drawerVideo.setOnCompletionListener(mp -> {
                    drawerVideo.setVisibility(View.GONE);
                    if (overlayImage != null) overlayImage.setVisibility(View.VISIBLE);
                    isImageSwapped = true;
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Video error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCodeInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Vault Code");
        final EditText codeInput = new EditText(this);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(codeInput);

        builder.setPositiveButton("UNLOCK", (dialog, which) -> {
            String enteredCode = codeInput.getText().toString().trim();
            if ("1234".equals(enteredCode)) {
                if (overlayImage != null) overlayImage.setVisibility(View.GONE);
                isImageSwapped = false;
                showHanoiGameDialog();
            } else {
                Toast.makeText(this, "ACCESS DENIED", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showHanoiGameDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_hanoi, null);
            builder.setView(dialogView);

            hanoiView = dialogView.findViewById(R.id.hanoiView);
            moveCounter = dialogView.findViewById(R.id.moveCounter);
            timerTextView = dialogView.findViewById(R.id.timerTextView);

            Button restartButton = dialogView.findViewById(R.id.restartButton);
            Button undoButton = dialogView.findViewById(R.id.undoButton);

            if (restartButton != null) {
                restartButton.setOnClickListener(v -> {
                    if (hanoiView != null) hanoiView.resetGame();
                    if (moveCounter != null) moveCounter.setText("Moves: 0 (Min: 7)");
                    if (countDownTimer != null) countDownTimer.cancel();
                    startTimer();
                });
            }

            if (undoButton != null) {
                undoButton.setOnClickListener(v -> {
                    if (hanoiView != null) hanoiView.undoMove();
                });
            }

            if (hanoiView != null) {
                hanoiView.setGameStateListener(this);
            }

            gameDialog = builder.create();
            gameDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            gameDialog.show();

            startTimer();
        } catch (Exception e) {
            Toast.makeText(this, "Game dialog error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        final long totalTime = 60000;
        countDownTimer = new CountDownTimer(totalTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (timerTextView != null) {
                    timerTextView.setText("Time Left: " + (millisUntilFinished / 1000) + "s");
                }
                timeElapsed = totalTime - millisUntilFinished;
            }

            @Override
            public void onFinish() {
                if (timerTextView != null) timerTextView.setText("Time Over!");
                Toast.makeText(Room2Activity.this, "Game Over! Time's up.", Toast.LENGTH_SHORT).show();
                if (gameDialog != null && gameDialog.isShowing()) gameDialog.dismiss();
                resetGameState();
            }
        }.start();
    }

    @Override
    public void onMove(int moves) {
        if (moveCounter != null) moveCounter.setText("Moves: " + moves + " (Min: 7)");
    }

    @Override
    public void onGameWon(int moves) {
        if (gameDialog != null && gameDialog.isShowing()) gameDialog.dismiss();
        if (countDownTimer != null) countDownTimer.cancel();

        gameWon = true;
        score += 10;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("SCORE_" + username, score);
        editor.putBoolean("GAME_WON_" + username, true);
        editor.apply();

        updateScore();
        showWinDialog(moves);
    }

    private void showWinDialog(int moves) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_win, null);
            builder.setView(dialogView);

            TextView winMessage = dialogView.findViewById(R.id.winMessage);
            Button okButton = dialogView.findViewById(R.id.winOkButton);

            long secondsElapsed = timeElapsed / 1000;
            if (winMessage != null) {
                winMessage.setText("Moves: " + moves + "\nTime: " + secondsElapsed + "s\nScore: " + score);
            }

            winDialog = builder.create();
            winDialog.setCancelable(false);
            winDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            if (okButton != null) {
                okButton.setOnClickListener(v -> {
                    stopWinMusic(); // stop when dialog is closed
                    if (winDialog != null && winDialog.isShowing()) {
                        winDialog.dismiss();
                    }
                    if (nextButton != null) {
                        nextButton.setEnabled(true);
                    }
                });
            }

            // play and keep reference
            winMediaPlayer = MediaPlayer.create(this, R.raw.success);
            if (winMediaPlayer != null) {
                winMediaPlayer.setOnCompletionListener(mp -> stopWinMusic());
                winMediaPlayer.start();
            }

            winDialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Win dialog error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            navigateToCompletion();
        }
    }

    private void stopWinMusic() {
        if (winMediaPlayer != null) {
            try {
                if (winMediaPlayer.isPlaying()) {
                    winMediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) { }
            winMediaPlayer.release();
            winMediaPlayer = null;
        }
    }

    private void navigateToCompletion() {
        try {
            Intent intent = new Intent(this, GameCompleteActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("SCORE", score);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateScore() {
        if (scoreText != null) scoreText.setText(getString(R.string.score_label, score));
        sharedPreferences.edit().putInt("SCORE_" + username, score).apply();
    }

    private void resetGameState() {
        if (gameDialog != null && gameDialog.isShowing()) gameDialog.dismiss();
        if (countDownTimer != null) countDownTimer.cancel();

        score = sharedPreferences.getInt("SCORE_" + username, 0);
        gameWon = false;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("GAME_WON_" + username, false);
        editor.apply();

        updateScore();
        if (nextButton != null) nextButton.setEnabled(false);
        if (overlayImage != null) overlayImage.setVisibility(View.GONE);
        isImageSwapped = false;
        if (moveCounter != null) moveCounter.setText("Moves: 0 (Min: 7)");
        Toast.makeText(this, "Game reset! Try again.", Toast.LENGTH_SHORT).show();
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onBackPressed() {
       if (lampZoomOverlay != null && isLampZoomVisible) {
            lampZoomOverlay.setVisibility(View.GONE);
            if (keyClickArea != null) keyClickArea.setVisibility(View.GONE);
            isLampZoomVisible = false;
        } else if (overlayImage != null && overlayImage.getVisibility() == View.VISIBLE) {
            overlayImage.setVisibility(View.GONE);
            isImageSwapped = false;
        } else {
            Intent intent = new Intent(this, RoomActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("SCORE", score);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (gameDialog != null && gameDialog.isShowing()) gameDialog.dismiss();
        if (winDialog != null && winDialog.isShowing()) winDialog.dismiss();
        stopWinMusic(); // make sure music is fully stopped
    }
}
