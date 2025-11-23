package org.example.model;

import java.util.List;

public class GameState {
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private String selectedCategory;
    private List<QuizQuestion> questions;

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
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

    public void incrementCorrectAnswers() {
        this.correctAnswers++;
    }

    public void incrementQuestionIndex() {
        this.currentQuestionIndex++;
    }
}
