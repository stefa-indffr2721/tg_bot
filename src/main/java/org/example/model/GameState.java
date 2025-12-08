package org.example.model;

import java.util.List;

public class GameState {
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private String selectedCategory;
    private List<QuizQuestion> questions;
    private long startTime;
    private long endTime;
    private boolean isDuelMode = false;
    private long opponentChatId = -1;

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void incrementCorrectAnswers() {
        this.correctAnswers++;
    }

    public void incrementQuestionIndex() {
        this.currentQuestionIndex++;
    }

    public long getGameDuration() {
        if (startTime > 0 && endTime > 0) {
            return (endTime - startTime) / 1000;
        }
        return 0;
    }

    public boolean isDuelMode() {
        return isDuelMode;
    }

    public void setDuelMode(boolean duelMode) {
        isDuelMode = duelMode;
    }

    public long getOpponentChatId() {
        return opponentChatId;
    }

    public void setOpponentChatId(long opponentChatId) {
        this.opponentChatId = opponentChatId;
    }
}