package org.example;

import java.util.Arrays;
import java.util.List;

// Класс для хранения вопросов, отделен от логики диалога
public class QuestionRepository {
    private final List<QuizQuestion> questions;

    public QuestionRepository() {
        this.questions = Arrays.asList(
                new QuizQuestion("Столица Франции?",
                        Arrays.asList("Лондон", "Париж", "Берлин"), 1),
                new QuizQuestion("Сколько планет в Солнечной системе?",
                        Arrays.asList("8", "9", "10"), 0),
                new QuizQuestion("Самое большое млекопитающее?",
                        Arrays.asList("Слон", "Синий кит", "Жираф"), 1),
                new QuizQuestion("Кто написал 'Войну и мир'?",
                        Arrays.asList("Достоевский", "Толстой", "Чехов"), 1),
                new QuizQuestion("Химическая формула воды?",
                        Arrays.asList("CO2", "H2O", "O2"), 1)
        );
    }

    public List<QuizQuestion> getAllQuestions() {
        return questions;
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    // Внутренний класс для хранения вопроса
    public static class QuizQuestion {
        private final String question;
        private final List<String> options;
        private final int correctAnswerIndex;

        public QuizQuestion(String question, List<String> options, int correctAnswerIndex) {
            this.question = question;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
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

        public boolean isCorrectAnswer(int answerIndex) {
            return answerIndex == correctAnswerIndex;
        }
    }
}