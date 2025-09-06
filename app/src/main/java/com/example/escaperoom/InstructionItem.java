package com.example.escaperoom;

public class InstructionItem {
    private String title;
    private String description;

    public InstructionItem(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}