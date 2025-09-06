package com.example.escaperoom.tohHanoi;

import java.util.Stack;
import java.util.ArrayList;

public class GameModel {
    private final Stack<Integer>[] towers;
    private int moves;
    private final int totalDisks;
    private ArrayList<Move> moveHistory;

    private static class Move {
        int from;
        int to;
        Move(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    @SuppressWarnings("unchecked")
    public GameModel(int disks) {
        this.totalDisks = disks;
        towers = new Stack[3];
        moveHistory = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            towers[i] = new Stack<>();
        }
        for (int i = disks; i > 0; i--) {
            towers[0].push(i);
        }
        moves = 0;
    }

    public Stack<Integer> getTower(int index) {
        if (index < 0 || index >= 3) {
            throw new IllegalArgumentException("Invalid tower index");
        }
        return towers[index];
    }

    public boolean moveDisk(int from, int to) {
        if (isValidMove(from, to)) {
            towers[to].push(towers[from].pop());
            moves++;
            moveHistory.add(new Move(from, to));
            return true;
        }
        return false;
    }

    public boolean undoMove() {
        if (moveHistory.isEmpty()) return false;

        Move lastMove = moveHistory.remove(moveHistory.size() - 1);
        towers[lastMove.from].push(towers[lastMove.to].pop());
        moves--;
        return true;
    }

    public boolean isValidMove(int from, int to) {
        if (from < 0 || from >= 3 || to < 0 || to >= 3) return false;
        if (from == to) return false;
        if (towers[from].isEmpty()) return false;
        if (towers[to].isEmpty()) return true;
        return towers[to].peek() > towers[from].peek();
    }

    public int getMoves() {
        return moves;
    }

    public void reset() {
        for (Stack<Integer> tower : towers) tower.clear();
        for (int i = totalDisks; i > 0; i--) towers[0].push(i);
        moves = 0;
        moveHistory.clear();
    }

    public boolean isGameWon() {
        return towers[2].size() == totalDisks;
    }
}