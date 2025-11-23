package org.example.service;

import org.example.model.QuizQuestion;
import org.example.repository.QuestionRepository;
import java.util.List;

public class QuizService {
    private final QuestionRepository questionRepository;

    public QuizService() {
        this.questionRepository = new QuestionRepository();
    }

    public List<String> getAvailableCategories() {
        return questionRepository.getAvailableCategories();
    }

    public List<QuizQuestion> getQuestionsByCategory(String category) {
        return questionRepository.getQuestionsByCategory(category);
    }

    public boolean validateAnswer(QuizQuestion question, int answerIndex) {
        return question.isCorrectAnswer(answerIndex);
    }

    public String getCorrectAnswer(QuizQuestion question) {
        return question.getOptions().get(question.getCorrectAnswerIndex());
    }
}
