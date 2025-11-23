package org.example.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.QuizQuestion;
import java.io.InputStream;
import java.util.*;

public class QuestionRepository {
    private final Map<String, List<QuizQuestion>> questionsByCategory;

    public QuestionRepository() {
        this.questionsByCategory = new HashMap<>();
        initializeQuestions();
    }

    private void initializeQuestions() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("DataQuiz.json");

            if (inputStream == null) {
                throw new RuntimeException("Read JSON error");
            }

            JsonData jsonData = mapper.readValue(inputStream, JsonData.class);

            for (Map.Entry<String, List<JsonQuestion>> entry : jsonData.categories.entrySet()) {
                List<QuizQuestion> quizQuestions = new ArrayList<>();
                for (JsonQuestion jsonQuestion : entry.getValue()) {
                    quizQuestions.add(new QuizQuestion(
                            jsonQuestion.question,
                            jsonQuestion.options,
                            jsonQuestion.correctAnswerIndex
                    ));
                }
                questionsByCategory.put(entry.getKey(), quizQuestions);
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки вопросов из JSON: " + e.getMessage(), e);
        }
    }

    private static class JsonData {
        public Map<String, List<JsonQuestion>> categories;
    }

    private static class JsonQuestion {
        public String question;
        public List<String> options;
        public int correctAnswerIndex;
    }

    public List<String> getAvailableCategories() {
        return new ArrayList<>(questionsByCategory.keySet());
    }

    public List<QuizQuestion> getQuestionsByCategory(String category) {
        return questionsByCategory.getOrDefault(category, new ArrayList<>());
    }
}
