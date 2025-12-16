package org.example.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.QuizQuestion;
import org.example.service.ImageUrlFinder;
import java.io.InputStream;
import java.util.*;

public class QuestionRepository {
    private final Map<String, List<QuizQuestion>> questionsByCategory;
    private final ImageUrlFinder imageUrlFinder;

    public QuestionRepository() {
        this.questionsByCategory = new HashMap<>();
        this.imageUrlFinder = new ImageUrlFinder();
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
                String category = entry.getKey();
                List<QuizQuestion> quizQuestions = new ArrayList<>();

                for (JsonQuestion jsonQuestion : entry.getValue()) {
                    String imageUrl = jsonQuestion.image;

                    if ("true".equals(imageUrl)) {
                        try {
                            String correctAnswerText = jsonQuestion.options.get(jsonQuestion.correctAnswerIndex);
                            imageUrl = imageUrlFinder.findImageUrl(correctAnswerText);
                        } catch (Exception e) {
                            System.err.println("❌ Ошибка парсинга изображения: " + jsonQuestion.options.get(jsonQuestion.correctAnswerIndex));
                            System.err.println("   Причина: " + e.getMessage());
                            imageUrl = null;
                        }
                    }

                    quizQuestions.add(new QuizQuestion(
                            jsonQuestion.question,
                            jsonQuestion.options,
                            jsonQuestion.correctAnswerIndex,
                            imageUrl
                    ));
                }
                questionsByCategory.put(category, quizQuestions);
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
        public String image;
    }

    public List<String> getAvailableCategories() {
        return new ArrayList<>(questionsByCategory.keySet());
    }

    public List<QuizQuestion> getQuestionsByCategory(String category) {
        return questionsByCategory.getOrDefault(category, new ArrayList<>());
    }
}