package org.example.model;

import java.util.List;

public class QuizQuestion {
    private final String question;
    private final List<String> options;
    private final int correctAnswerIndex;
    private final String image;

    public QuizQuestion(String question, List<String> options, int correctAnswerIndex, String image) {
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.image = image;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public String getImage() {
        return image;
    }

    public boolean isCorrectAnswer(int answerIndex) {
        return answerIndex == correctAnswerIndex;
    }
}