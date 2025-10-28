package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {

    private final QuestionRepository questionRepository;

    private final Map<Long, GameState> userGameStates;

    public TelegramBot() {
        this.questionRepository = new QuestionRepository();
        this.userGameStates = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/play")) {
                showCategorySelection(chatId);
            } else if (userGameStates.containsKey(chatId)) {
                GameState gameState = userGameStates.get(chatId);

                if (gameState.waitingForCategory) {
                    processCategorySelection(chatId, messageText);
                } else if (messageText.equals("1") || messageText.equals("2") || messageText.equals("3")) {
                    //по хорошему надо переписать так, чтоб работало с любым количеством ответов
                    processAnswer(chatId, Integer.parseInt(messageText));
                } else {
                    sendMessage(chatId, "Для ответа введите 1, 2 или 3");
                }
            } else {
                switch (messageText) {
                    case "/start":
                        sendStartMessage(chatId);
                        break;
                    case "/help":
                        sendHelpMessage(chatId);
                        break;
                    default:
                        sendUnknownCommandMessage(chatId);
                        break;
                }
            }
        }
    }

    private void showCategorySelection(long chatId) {
        List<String> categories = questionRepository.getAvailableCategories();
        StringBuilder message = new StringBuilder("🎯 Выберите категорию:\n\n");

        for (int i = 0; i < categories.size(); i++) {
            message.append(i + 1).append(". ").append(categories.get(i)).append("\n");
        }

        message.append("\nВведите номер категории (1-5):");

        GameState gameState = new GameState();
        gameState.waitingForCategory = true;
        userGameStates.put(chatId, gameState);

        sendMessage(chatId, message.toString());
    }

    private void processCategorySelection(long chatId, String categoryInput) {
        if (categoryInput.equals("1") || categoryInput.equals("2") || categoryInput.equals("3") ||
                categoryInput.equals("4") || categoryInput.equals("5")) {
            //по хорошему надо переписать так, чтоб работало с любым количеством категорий

            int categoryIndex = Integer.parseInt(categoryInput) - 1;
            List<String> categories = questionRepository.getAvailableCategories();
            String selectedCategory = categories.get(categoryIndex);
            GameState gameState = userGameStates.get(chatId);

            gameState.waitingForCategory = false;
            gameState.selectedCategory = selectedCategory;
            gameState.questions = questionRepository.getQuestionsByCategory(selectedCategory);

            sendMessage(chatId, "✅ Вы выбрали: " + selectedCategory + "\nНачинаем викторину!");
            sendNextQuestion(chatId);

        } else {
            sendMessage(chatId, "❌ Пожалуйста, введите число от 1 до 5");
        }
    }

    private void startGame(long chatId) {
        sendNextQuestion(chatId);
    }

    private void sendNextQuestion(long chatId) {
        GameState gameState = userGameStates.get(chatId);

        if (gameState.currentQuestionIndex < gameState.questions.size()) {
            QuizQuestion currentQuestion = gameState.questions.get(gameState.currentQuestionIndex);

            String questionText = "📝 Категория: " + gameState.selectedCategory + "\n" +
                    "Вопрос " + (gameState.currentQuestionIndex + 1) + "/5:\n" +
                    currentQuestion.getQuestion() + "\n\n" +
                    "1. " + currentQuestion.getOptions().get(0) + "\n" +
                    "2. " + currentQuestion.getOptions().get(1) + "\n" +
                    "3. " + currentQuestion.getOptions().get(2) + "\n\n" +
                    "Выберите ответ (1, 2 или 3):";

            sendMessage(chatId, questionText);
        } else {
            finishGame(chatId);
        }
    }

    private void processAnswer(long chatId, int userAnswer) {
        GameState gameState = userGameStates.get(chatId);
        QuizQuestion currentQuestion = gameState.questions.get(gameState.currentQuestionIndex);

        boolean isCorrect = (userAnswer - 1) == currentQuestion.getCorrectAnswerIndex();

        if (isCorrect) {
            gameState.correctAnswers++;
            sendMessage(chatId, "✅ Правильно!");
        } else {
            String correctAnswer = currentQuestion.getOptions().get(currentQuestion.getCorrectAnswerIndex());
            sendMessage(chatId, "❌ Неправильно! Правильный ответ: " + correctAnswer);
        }

        gameState.currentQuestionIndex++;

        if (gameState.currentQuestionIndex < gameState.questions.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendNextQuestion(chatId);
        } else {
            finishGame(chatId);
        }
    }

    private void finishGame(long chatId) {
        GameState gameState = userGameStates.get(chatId);
        String result = "🎉 Викторина завершена!\n" +
                "Ваш результат: " + gameState.correctAnswers + "/5 правильных ответов!\n\n" +
                "Хотите сыграть ещё раз?\n" +
                "/play\n" +
                "Или используйте команду /help чтобы узнать что я умею.";

        sendMessage(chatId, result);
        userGameStates.remove(chatId);
    }

    private void sendStartMessage(long chatId) {
        String response = """
                🚀 Добро пожаловать в QuizBot!
                
                Я бот для проведения викторин.
                
                Используйте /play чтобы начать игру!
                
                Используйте команду /help чтобы узнать что я умею.""";

        sendMessage(chatId, response);
    }

    private void sendHelpMessage(long chatId) {
        String response = """
                📋 Справка по командам:
                
                /start - начать работу с ботом
                /help - показать эту справку
                /play - выбрать категорию и начать викторину (5 вопросов)
                
                Во время игры выбирайте ответы 1, 2 или 3.""";

        sendMessage(chatId, response);
    }

    private void sendUnknownCommandMessage(long chatId) {
        String response = "❌ Неизвестная команда.\n" +
                "Используйте /help для просмотра доступных команд.";

        sendMessage(chatId, response);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return Token.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return Token.BOT_TOKEN;
    }

    private static class GameState {
        int currentQuestionIndex = 0;
        int correctAnswers = 0;
        boolean waitingForCategory = true;
        String selectedCategory;
        List<QuizQuestion> questions;
    }
}