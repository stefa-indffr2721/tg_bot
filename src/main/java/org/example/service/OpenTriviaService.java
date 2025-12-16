package org.example.service;

import org.example.model.QuizQuestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenTriviaService {

    private static final String BASE_URL = "https://opentdb.com/api.php?amount=10&type=multiple";

    public List<QuizQuestion> getQuestions(String categoryName) {
        try {
            String urlString = BASE_URL;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            InputStream inputStream = conn.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inputStream);
            JsonNode results = root.get("results");

            List<QuizQuestion> questions = new ArrayList<>();
            for (JsonNode node : results) {
                String question = node.get("question").asText();
                String correctAnswer = node.get("correct_answer").asText();

                List<String> options = new ArrayList<>();
                for (JsonNode option : node.get("incorrect_answers")) {
                    options.add(option.asText());
                }
                options.add(correctAnswer);
                Collections.shuffle(options);

                int correctIndex = options.indexOf(correctAnswer);

                questions.add(new QuizQuestion(question, options, correctIndex, null));
            }

            return questions;

        } catch (Exception e) {
            System.err.println("[ERR OpenTrivia]: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
