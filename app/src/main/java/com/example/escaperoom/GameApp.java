package com.example.escaperoom;

import android.app.Application;

public class GameApp extends Application {
    private static int points = 0;

    public static int getPoints() {
        return points;
    }

    public static void setPoints(int newPoints) {
        points = newPoints;
    }
}
