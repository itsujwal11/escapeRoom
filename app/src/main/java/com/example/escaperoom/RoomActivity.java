package com.example.escaperoom;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class RoomActivity extends AppCompatActivity {
    private TextView scoreText;
    private TextView usernameText;
    private TextView progressText;
    private TextView instructionText;
    private ImageView overlayImage;
    private View clickableArea;
    private Button backButton;
    private ImageView hintButton;
    private Button nextButton;
    private Button resetButton;
    private GridLayout puzzleGrid;
    private ArrayList<Integer> puzzleOrder;
    private ArrayList<ImageView> puzzlePieces = new ArrayList<>();
    private CountDownTimer countDownTimer;
    private long timeElapsed;
    private boolean isImageSwapped = false;
    private boolean puzzleSolvedFlag = false;
    private int score = 0;
    private String username;
    private SharedPreferences sharedPreferences;
    private Dialog puzzleDialog;
    private AlertDialog winDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        // Get username from intent or SharedPreferences
        username = getIntent().getStringExtra("USERNAME");
        if (username == null) {
            username = sharedPreferences.getString("USERNAME", "Guest");
        }


        // Load saved state
        score = sharedPreferences.getInt("SCORE_" + username, 0);
        puzzleSolvedFlag = sharedPreferences.getBoolean("PUZZLE_SOLVED_" + username, false);

        // Initialize views
        overlayImage = findViewById(R.id.overlayImage);
        clickableArea = findViewById(R.id.clickableArea);
        scoreText = findViewById(R.id.scoreText);
        usernameText = findViewById(R.id.usernameText);
        progressText = findViewById(R.id.progressText);
        instructionText = findViewById(R.id.instructionText);
        backButton = findViewById(R.id.backButton);
        hintButton = findViewById(R.id.hintButton);
        nextButton = findViewById(R.id.nextButton);
        resetButton = findViewById(R.id.resetButton);

        // Set initial UI state
        usernameText.setText(getString(R.string.welcome_user, username));
        scoreText.setText(getString(R.string.score_label, score));
        progressText.setText("Room 1/2");
        instructionText.setText("Note: Click on the frame to unlock the puzzle");
        nextButton.setEnabled(puzzleSolvedFlag && score > 0);

        if (overlayImage != null) {
            overlayImage.setVisibility(View.GONE);
        }
        isImageSwapped = false;

        backButton.setOnClickListener(v -> onBackPressed());

        clickableArea.setOnClickListener(v -> {
            if (!isImageSwapped && overlayImage != null) {
                overlayImage.setVisibility(View.VISIBLE);
                overlayImage.setImageResource(R.drawable.zoomed);
                isImageSwapped = true;
            }
        });


        overlayImage.setOnClickListener(v -> {
            if (puzzleSolvedFlag) {
                resetPuzzleOnly();
                showPuzzleDialog();
            } else {
                showPuzzleDialog();
            }
        });

        hintButton.setOnClickListener(v -> Toast.makeText(RoomActivity.this, R.string.hint_message, Toast.LENGTH_SHORT).show());

        nextButton.setOnClickListener(v -> {
            if (!puzzleSolvedFlag) {
                Toast.makeText(RoomActivity.this, R.string.puzzle_not_solved, Toast.LENGTH_SHORT).show();
            } else if (score <= 0) {
                Toast.makeText(RoomActivity.this, "You need a score greater than 0 to proceed!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(RoomActivity.this, Room2Activity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("SCORE", score);
                startActivity(intent);
                finish();
            }
        });

        resetButton.setOnClickListener(v -> resetPuzzleState());
    }

    @Override
    public void onBackPressed() {
        if (puzzleDialog != null && puzzleDialog.isShowing()) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            for (ImageView piece : puzzlePieces) {
                piece.setOnTouchListener(null);
                piece.setOnDragListener(null);
                piece.setImageDrawable(null);
            }
            puzzlePieces.clear();
            if (puzzleGrid != null) {
                puzzleGrid.removeAllViews();
                puzzleGrid.setVisibility(View.GONE);
            }
            puzzleDialog.dismiss();
            puzzleDialog = null;
            puzzleGrid = null;
        } else if (isImageSwapped) {
            if (overlayImage != null) {
                overlayImage.setVisibility(View.GONE);
                overlayImage.invalidate();
                isImageSwapped = false;
            }
        } else {
            Intent intent = new Intent(RoomActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("SCORE", score);
            startActivity(intent);
            finish();
        }
    }

    private void showPuzzleDialog() {
        puzzleDialog = new Dialog(this);
        puzzleDialog.setContentView(R.layout.dialog_puzzle);
        puzzleDialog.setCancelable(false);
        puzzleDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        puzzleGrid = puzzleDialog.findViewById(R.id.puzzleGrid);
        TextView dialogTimerTextView = puzzleDialog.findViewById(R.id.timerTextView);
        Button closeButton = puzzleDialog.findViewById(R.id.cancelButton);

        closeButton.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            for (ImageView piece : puzzlePieces) {
                piece.setOnTouchListener(null);
                piece.setOnDragListener(null);
                piece.setImageDrawable(null);
            }
            puzzlePieces.clear();
            if (puzzleGrid != null) {
                puzzleGrid.removeAllViews();
                puzzleGrid.setVisibility(View.GONE);
            }
            if (puzzleDialog != null && puzzleDialog.isShowing()) {
                puzzleDialog.dismiss();
                puzzleDialog = null;
            }
            puzzleGrid = null;
        });

        puzzleDialog.show();
        setupPuzzle();
        startTimer(puzzleDialog, dialogTimerTextView);
    }

    private void startTimer(Dialog puzzleDialog, TextView timerTextView) {
        final long totalTime = 60000;
        countDownTimer = new CountDownTimer(totalTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText(getString(R.string.time_left, millisUntilFinished / 1000));
                timeElapsed = totalTime - millisUntilFinished;
            }

            @Override
            public void onFinish() {
                timerTextView.setText(R.string.time_over);
                Toast.makeText(RoomActivity.this, R.string.game_over, Toast.LENGTH_SHORT).show();
                puzzleDialog.dismiss();
                puzzleSolvedFlag = false;
                score = 0;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("SCORE_" + username, score);
                editor.putBoolean("PUZZLE_SOLVED_" + username, puzzleSolvedFlag);
                editor.apply();

                updateScore(0);
                nextButton.setEnabled(false);

                Intent intent = new Intent(RoomActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }.start();
    }

    private void setupPuzzle() {
        if (puzzleGrid != null) {
            puzzleGrid.removeAllViews();
            puzzleGrid.setVisibility(View.VISIBLE);
        }
        puzzlePieces.clear();
        puzzleOrder = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        Collections.shuffle(puzzleOrder);

        for (int i = 0; i < 4; i++) {
            ImageView puzzlePiece = new ImageView(this);
            int resId = getResources().getIdentifier("piece" + puzzleOrder.get(i), "drawable", getPackageName());
            puzzlePiece.setImageResource(resId);
            puzzlePiece.setTag(puzzleOrder.get(i));
            puzzlePiece.setOnTouchListener(new PuzzleTouchListener());
            puzzlePiece.setOnDragListener(new PuzzleDragListener());
            if (puzzleGrid != null) {
                puzzleGrid.addView(puzzlePiece);
            }
            puzzlePieces.add(puzzlePiece);
        }
    }

    private class PuzzleTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (puzzleSolvedFlag) {
                Toast.makeText(RoomActivity.this, "You have already completed the puzzle! Reset to try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startDragAndDrop(null, new View.DragShadowBuilder(v), v, 0);
                return true;
            }
            return false;
        }
    }

    private class PuzzleDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (puzzleSolvedFlag) {
                Toast.makeText(RoomActivity.this, "You have already completed the puzzle! Reset to try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (event.getAction() == DragEvent.ACTION_DROP) {
                View draggedView = (View) event.getLocalState();
                Drawable draggedDrawable = ((ImageView) draggedView).getDrawable();
                Drawable targetDrawable = ((ImageView) v).getDrawable();
                ((ImageView) draggedView).setImageDrawable(targetDrawable);
                ((ImageView) v).setImageDrawable(draggedDrawable);
                Object tempTag = draggedView.getTag();
                draggedView.setTag(v.getTag());
                v.setTag(tempTag);
                checkIfPuzzleSolved();
            }
            return true;
        }
    }

    private void checkIfPuzzleSolved() {
        if (puzzleSolvedFlag) {
            return;
        }
        boolean isSolved = true;
        for (int i = 0; i < puzzleGrid.getChildCount(); i++) {
            if ((int) puzzleGrid.getChildAt(i).getTag() != i + 1) {
                isSolved = false;
                break;
            }
        }
        if (isSolved) {
            puzzleSolvedFlag = true;
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            // Save game state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("SCORE_" + username, score + 10);
            editor.putBoolean("PUZZLE_SOLVED_" + username, true);
            editor.apply();

            updateScore(10);
            nextButton.setEnabled(true);

            runOnUiThread(() -> {
                for (ImageView piece : puzzlePieces) {
                    piece.setOnTouchListener(null);
                    piece.setOnDragListener(null);
                    piece.setImageDrawable(null);
                }
                puzzlePieces.clear();
                if (puzzleGrid != null) {
                    puzzleGrid.removeAllViews();
                    puzzleGrid.setVisibility(View.GONE);
                }
                if (overlayImage != null) {
                    overlayImage.setVisibility(View.GONE);
                    overlayImage.invalidate();
                    isImageSwapped = false;
                }
                View rootView = findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.invalidate();
                }
            });

            new android.os.Handler().postDelayed(() -> {
                if (puzzleDialog != null && puzzleDialog.isShowing()) {
                    puzzleDialog.dismiss();
                    puzzleDialog = null;
                }
                puzzleGrid = null;
                showWinDialog();
            }, 100);
        }
    }

    private void showWinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_win, null);
        builder.setView(dialogView);

        TextView winTitle = dialogView.findViewById(R.id.winTitle);
        TextView winMessage = dialogView.findViewById(R.id.winMessage);
        ImageView winIcon = dialogView.findViewById(R.id.winIcon);
        Button okButton = dialogView.findViewById(R.id.winOkButton);

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.success);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> mp.release());
            mediaPlayer.start();
        }

        if (winTitle != null) {
            winTitle.setText("PUZZLE SOLVED!");
        }
        if (winMessage != null) {
            long secondsElapsed = timeElapsed / 1000;
            winMessage.setText("Time: " + secondsElapsed + "s\nScore: " + score);
        }
        if (winIcon != null) {
            winIcon.setImageResource(android.R.drawable.checkbox_on_background);
        }

        winDialog = builder.create();
        winDialog.setCancelable(false);
        winDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (okButton != null) {
            okButton.setOnClickListener(v -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                if (winDialog != null && winDialog.isShowing()) {
                    winDialog.dismiss();
                    winDialog = null;
                }
            });
        }

        if (!isFinishing()) {
            winDialog.show();
        }
    }

    private void updateScore(int points) {
        score += points;
        scoreText.setText(getString(R.string.score_label, score));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("SCORE_" + username, score);
        editor.apply();
        nextButton.setEnabled(puzzleSolvedFlag && score > 0);
    }

    private void resetPuzzleOnly() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        for (ImageView piece : puzzlePieces) {
            piece.setOnTouchListener(null);
            piece.setOnDragListener(null);
            piece.setImageDrawable(null);
        }
        puzzlePieces.clear();
        if (puzzleDialog != null && puzzleDialog.isShowing()) {
            if (puzzleGrid != null) {
                puzzleGrid.removeAllViews();
                puzzleGrid.setVisibility(View.GONE);
            }
            puzzleDialog.dismiss();
            puzzleDialog = null;
        }
        puzzleGrid = null;
        if (overlayImage != null) {
            overlayImage.setVisibility(View.GONE);
            overlayImage.invalidate();
            isImageSwapped = false;
        }
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.invalidate();
        }
        Toast.makeText(this, "Puzzle reset! You can try again.", Toast.LENGTH_SHORT).show();
    }

    private void resetPuzzleState() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        score = 0;
        puzzleSolvedFlag = false;


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("SCORE_" + username, score);
        editor.putBoolean("PUZZLE_SOLVED_" + username, puzzleSolvedFlag);
        editor.apply();

        updateScore(0);
        nextButton.setEnabled(false);

        for (ImageView piece : puzzlePieces) {
            piece.setOnTouchListener(null);
            piece.setOnDragListener(null);
            piece.setImageDrawable(null);
        }
        puzzlePieces.clear();
        if (puzzleDialog != null && puzzleDialog.isShowing()) {
            if (puzzleGrid != null) {
                puzzleGrid.removeAllViews();
                puzzleGrid.setVisibility(View.GONE);
            }
            puzzleDialog.dismiss();
            puzzleDialog = null;
        }
        puzzleGrid = null;
        if (overlayImage != null) {
            overlayImage.setVisibility(View.GONE);
            overlayImage.invalidate();
            isImageSwapped = false;
        }
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.invalidate();
        }
        Toast.makeText(this, "Puzzle and score reset! You can try again.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        for (ImageView piece : puzzlePieces) {
            piece.setOnTouchListener(null);
            piece.setOnDragListener(null);
            piece.setImageDrawable(null);
        }
        puzzlePieces.clear();
        if (puzzleDialog != null && puzzleDialog.isShowing()) {
            if (puzzleGrid != null) {
                puzzleGrid.removeAllViews();
                puzzleGrid.setVisibility(View.GONE);
            }
            puzzleDialog.dismiss();
            puzzleDialog = null;
        }
        puzzleGrid = null;
        if (winDialog != null && winDialog.isShowing()) {
            winDialog.dismiss();
            winDialog = null;
        }
    }
}