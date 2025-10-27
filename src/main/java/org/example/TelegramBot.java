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
                startGame(chatId);
            } else if (userGameStates.containsKey(chatId) &&
                    (messageText.equals("1") || messageText.equals("2") || messageText.equals("3"))) {
                processAnswer(chatId, Integer.parseInt(messageText));
            } else {
                switch (messageText) {
                    case "/start":
                        sendStartMessage(chatId);
                        break;
                    case "/help":
                        sendHelpMessage(chatId);
                        break;
                    default:
                        if (userGameStates.containsKey(chatId)) {
                            sendMessage(chatId, "Для ответа введите 1, 2 или 3");
                        } else {
                            sendUnknownCommandMessage(chatId);
                        }
                        break;
                }
            }
        }
    }

    private void startGame(long chatId) {
        userGameStates.put(chatId, new GameState());
        sendNextQuestion(chatId);
    }

    private void sendNextQuestion(long chatId) {
        GameState gameState = userGameStates.get(chatId);

        if (gameState.currentQuestionIndex < questionRepository.getTotalQuestions()) {
            QuestionRepository.QuizQuestion currentQuestion =
                    questionRepository.getAllQuestions().get(gameState.currentQuestionIndex);

            String questionText = "Вопрос " + (gameState.currentQuestionIndex + 1) + "/5:\n" +
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

    // Добавлено: Обработка ответа
    private void processAnswer(long chatId, int userAnswer) {
        GameState gameState = userGameStates.get(chatId);
        QuestionRepository.QuizQuestion currentQuestion =
                questionRepository.getAllQuestions().get(gameState.currentQuestionIndex);

        boolean isCorrect = (userAnswer - 1) == currentQuestion.getCorrectAnswerIndex();

        if (isCorrect) {
            gameState.correctAnswers++;
            sendMessage(chatId, "✅ Правильно!");
        } else {
            String correctAnswer = currentQuestion.getOptions().get(currentQuestion.getCorrectAnswerIndex());
            sendMessage(chatId, "❌ Неправильно! Правильный ответ: " + correctAnswer);
        }

        gameState.currentQuestionIndex++;

        if (gameState.currentQuestionIndex < questionRepository.getTotalQuestions()) {
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
        String result = "🎉 Викторина завершена! :)\n\n" +
                "Ваш результат: " + gameState.correctAnswers + "/5 правильных ответов!";
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
                /play - начать викторину (5 вопросов)
                
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

    // Добавлено: Внутренний класс для состояния игры
    private static class GameState {
        int currentQuestionIndex = 0;
        int correctAnswers = 0;
    }
}