package org.example.model;

public class LeaderboardEntry {
    private String playerName;
    private long chatId;
    private int correctAnswers;
    private long timeSeconds;
    private String category;

    public LeaderboardEntry() {}

    public LeaderboardEntry(String playerName, long chatId, int correctAnswers, long timeSeconds, String category) {
        this.playerName = playerName;
        this.chatId = chatId;
        this.correctAnswers = correctAnswers;
        this.timeSeconds = timeSeconds;
        this.category = category;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public long getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(long timeSeconds) {
        this.timeSeconds = timeSeconds;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}