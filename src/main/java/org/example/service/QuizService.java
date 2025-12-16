package org.example.service;

import org.example.model.QuizQuestion;
import org.example.repository.QuestionRepository;

import java.util.List;

public class QuizService {
    private final QuestionRepository questionRepository;
    private final OpenTriviaService openTriviaService;

    public QuizService() {
        this.questionRepository = new QuestionRepository();
        this.openTriviaService = new OpenTriviaService();
    }

    public List<String> getAvailableCategories() {
        List<String> localCategories = questionRepository.getAvailableCategories();
        localCategories.add("OpenTrivia");
        return localCategories;
    }

    public List<QuizQuestion> getQuestionsByCategory(String category) {
        if ("OpenTrivia".equalsIgnoreCase(category)) {
            return openTriviaService.getQuestions(category);
        }
        return questionRepository.getQuestionsByCategory(category);
    }

    public boolean validateAnswer(QuizQuestion question, int answerIndex) {
        return question.isCorrectAnswer(answerIndex);
    }

    public String getCorrectAnswer(QuizQuestion question) {
        return question.getOptions().get(question.getCorrectAnswerIndex());
    }
}
