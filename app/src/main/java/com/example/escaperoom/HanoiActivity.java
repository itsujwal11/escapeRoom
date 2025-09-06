package com.example.escaperoom;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Stack;

public class HanoiActivity extends AppCompatActivity {

    private LinearLayout[] pegs;
    private Stack<View>[] pegStacks;
    private int totalDisks = 3;
    private int moves = 0;
    private TextView moveCounter;
    private Stack<Move> moveHistory = new Stack<>();
    private boolean isSolving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hanoi);

        pegs = new LinearLayout[3];
        pegStacks = new Stack[3];
        moveCounter = findViewById(R.id.moveCounter);

        for (int i = 0; i < 3; i++) {
            pegStacks[i] = new Stack<>();
            int pegId = getResources().getIdentifier("peg" + (i + 1), "id", getPackageName());
            pegs[i] = findViewById(pegId);
            setupPegDragListener(pegs[i], i);
        }

        createInitialDisks();
    }

    private void createInitialDisks() {
        for (int i = totalDisks; i > 0; i--) {
            addDiskToPeg(0, i);
        }
    }

    private void addDiskToPeg(int pegIndex, int size) {
        View disk = createDisk(size);
        pegStacks[pegIndex].push(disk);
        pegs[pegIndex].addView(disk);
    }

    private View createDisk(int size) {
        View disk = new View(this);
        int width = 100 + (size * 40);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, 60);
        params.setMargins(0, 0, 0, 5);
        disk.setLayoutParams(params);
        disk.setBackgroundColor(Color.rgb(100 + size * 30, 50, 200));
        disk.setTag(size);

        disk.setOnLongClickListener(v -> {
            if (!isSolving) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(data, shadowBuilder, v, 0);
            }
            return true;
        });
        return disk;
    }

    private void setupPegDragListener(LinearLayout peg, int pegIndex) {
        peg.setOnDragListener((v, event) -> {
            if (isSolving) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DROP:
                    View disk = (View) event.getLocalState();
                    handleDrop(pegIndex, disk);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void handleDrop(int targetPegIndex, View disk) {
        int diskSize = (int) disk.getTag();
        Stack<View> targetStack = pegStacks[targetPegIndex];

        if (!targetStack.isEmpty() && diskSize > (int) targetStack.peek().getTag()) {
            Toast.makeText(this, "Invalid Move!", Toast.LENGTH_SHORT).show();
            return;
        }

        int sourcePeg = -1;
        for (int i = 0; i < 3; i++) {
            if (pegStacks[i].contains(disk)) {
                sourcePeg = i;
                break;
            }
        }

        if (sourcePeg != -1) {
            moveDisk(sourcePeg, targetPegIndex, disk);
            moveHistory.push(new Move(sourcePeg, targetPegIndex, diskSize));
            moves++;
            updateMoveCounter();
            checkWinCondition();
        }
    }

    private void moveDisk(int fromPeg, int toPeg, View disk) {
        pegStacks[fromPeg].remove(disk);
        pegs[fromPeg].removeView(disk);
        pegStacks[toPeg].push(disk);
        pegs[toPeg].addView(disk);
    }

    public void solvePuzzle(View view) {
        isSolving = true;
        new Thread(() -> {
            solve(totalDisks, 0, 2, 1);
            runOnUiThread(() -> isSolving = false);
        }).start();
    }

    private void solve(int n, int from, int to, int aux) {
        if (n == 0) return;
        solve(n - 1, from, aux, to);
        final int diskSize = n;
        runOnUiThread(() -> {
            View disk = findDiskInPeg(from, diskSize);
            if (disk != null) {
                moveDisk(from, to, disk);
                moveHistory.push(new Move(from, to, diskSize));
                moves++;
                updateMoveCounter();
            }
        });
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        solve(n - 1, aux, to, from);
    }

    public void restartGame(View view) {
        for (int i = 0; i < 3; i++) {
            pegStacks[i].clear();
            pegs[i].removeAllViews();
        }
        moveHistory.clear();
        moves = 0;
        updateMoveCounter();
        createInitialDisks();
    }

    public void restoreLastMove(View view) {
        if (!moveHistory.isEmpty()) {
            Move lastMove = moveHistory.pop();
            View disk = findDiskInPeg(lastMove.toPeg, lastMove.diskSize);
            if (disk != null) {
                moveDisk(lastMove.toPeg, lastMove.fromPeg, disk);
                moves--;
                updateMoveCounter();
            }
        }
    }

    private View findDiskInPeg(int pegIndex, int size) {
        for (View disk : pegStacks[pegIndex]) {
            if ((int) disk.getTag() == size) {
                return disk;
            }
        }
        return null;
    }

    private void updateMoveCounter() {
        runOnUiThread(() -> moveCounter.setText("Moves: " + moves));
    }

    private void checkWinCondition() {
        if (pegStacks[2].size() == totalDisks) {
            Toast.makeText(this, "You won in " + moves + " moves!", Toast.LENGTH_LONG).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("moves", moves);
            setResult(RESULT_OK, resultIntent);
            finish(); // Return to Room2Activity
        }
    }

    private static class Move {
        int fromPeg;
        int toPeg;
        int diskSize;

        Move(int from, int to, int size) {
            fromPeg = from;
            toPeg = to;
            diskSize = size;
        }
    }
}