package com.example.escaperoom.tohHanoi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Stack;

public class HanoiView extends View {
    private Paint towerPaint, diskPaint;
    private int[] diskColors = {Color.RED, Color.GREEN, Color.BLUE};
    public GameModel gameModel;
    private int selectedTower = -1;
    private float dragX, dragY;
    private boolean isDragging = false;
    private ValueAnimator animator;
    private GameStateListener gameStateListener;

    private static final int PILLAR_TOP = 50;
    private static final int PILLAR_BOTTOM_OFFSET = 50;
    private static final int DISK_HEIGHT = 60;

    public HanoiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        towerPaint = new Paint();
        towerPaint.setColor(Color.DKGRAY);
        towerPaint.setStrokeWidth(20f);

        diskPaint = new Paint();
        diskPaint.setStyle(Paint.Style.FILL);
        gameModel = new GameModel(3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTowers(canvas);
        drawDisks(canvas);
        if (isDragging) drawDraggedDisk(canvas);
    }

    private void drawDraggedDisk(Canvas canvas) {
        if (!isDragging) return;

        int diskSize = gameModel.getTower(selectedTower).peek();
        diskPaint.setColor(diskColors[(diskSize - 1) % diskColors.length]);

        float diskWidth = 60 * diskSize;
        float left = dragX - diskWidth / 2;
        float top = dragY - DISK_HEIGHT / 2;
        canvas.drawRect(left, top, left + diskWidth, top + DISK_HEIGHT, diskPaint);
    }

    private void drawTowers(Canvas canvas) {
        int towerSpacing = getWidth() / 4;
        for (int i = 1; i <= 3; i++) {
            float x = i * towerSpacing;
            canvas.drawLine(x, PILLAR_TOP, x, getHeight() - PILLAR_BOTTOM_OFFSET, towerPaint);
        }
    }

    private void drawDisks(Canvas canvas) {
        int towerSpacing = getWidth() / 4;
        for (int towerIdx = 0; towerIdx < 3; towerIdx++) {
            Stack<Integer> tower = gameModel.getTower(towerIdx);
            float towerX = (towerIdx + 1) * towerSpacing;
            for (int i = 0; i < tower.size(); i++) {
                int diskSize = tower.get(i);
                diskPaint.setColor(diskColors[(diskSize - 1) % diskColors.length]);
                drawDisk(canvas, towerX, diskSize, i);
            }
        }
    }

    private void drawDisk(Canvas canvas, float x, int diskSize, int position) {
        float diskWidth = 60 * diskSize;
        float top = getHeight() - PILLAR_BOTTOM_OFFSET - 50 - (position * DISK_HEIGHT);
        canvas.drawRect(x - diskWidth / 2, top, x + diskWidth / 2, top + DISK_HEIGHT, diskPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                handleTouchMove(x, y);
                return true;

            case MotionEvent.ACTION_UP:
                handleTouchEnd(x, y);
                return true;
        }
        return true;
    }

    private void handleTouchDown(float x, float y) {
        selectedTower = findTouchedTower(x);
        if (selectedTower != -1 && !gameModel.getTower(selectedTower).isEmpty()) {
            isDragging = true;
            dragX = x;
            dragY = y - DISK_HEIGHT / 2;
            invalidate();
        }
    }

    private void handleTouchMove(float x, float y) {
        if (isDragging) {
            dragX = x;
            dragY = y - DISK_HEIGHT / 2;
            invalidate();
        }
    }

    private void handleTouchEnd(float x, float y) {
        if (!isDragging) return;

        int targetTower = findTouchedTower(x);
        if (targetTower != -1 && gameModel.isValidMove(selectedTower, targetTower)) {
            animateDiskMove(selectedTower, targetTower);
        } else {
            animateInvalidMove(selectedTower);
        }
        isDragging = false;
    }

    private void animateDiskMove(int from, int to) {
        float startX = dragX;
        float startY = dragY;
        float endX = (to + 1) * (getWidth() / 4f);
        float endY = calculateDiskY(to, gameModel.getTower(to).size());

        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            dragX = startX + (endX - startX) * fraction;
            dragY = startY + (endY - startY) * fraction;
            invalidate();
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                gameModel.moveDisk(from, to);
                if (gameStateListener != null) {
                    gameStateListener.onMove(gameModel.getMoves());
                    if (gameModel.isGameWon()) {
                        gameStateListener.onGameWon(gameModel.getMoves());
                    }
                }
                invalidate();
            }
        });
        animator.start();
    }

    private void animateInvalidMove(int towerIndex) {
        float targetX = (towerIndex + 1) * (getWidth() / 4f);
        ValueAnimator snapBack = ValueAnimator.ofFloat(dragX, targetX);
        snapBack.setDuration(200);
        snapBack.addUpdateListener(animation -> {
            dragX = (float) animation.getAnimatedValue();
            invalidate();
        });
        snapBack.start();
    }

    private float calculateDiskY(int towerIndex, int position) {
        return getHeight() - PILLAR_BOTTOM_OFFSET - 50 - (position * DISK_HEIGHT);
    }

    private int findTouchedTower(float x) {
        int towerSpacing = getWidth() / 4;
        for (int i = 0; i < 3; i++) {
            float towerX = (i + 1) * towerSpacing;
            if (x > towerX - 75 && x < towerX + 75) return i;
        }
        return -1;
    }

    public void resetGame() {
        gameModel.reset();
        if (gameStateListener != null) {
            gameStateListener.onMove(0);
        }
        invalidate();
    }

    public void undoMove() {
        if (gameModel.undoMove()) {
            if (gameStateListener != null) {
                gameStateListener.onMove(gameModel.getMoves());
            }
            invalidate();
        }
    }

    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }

    public interface GameStateListener {
        void onMove(int moves);
        void onGameWon(int moves);
    }
}