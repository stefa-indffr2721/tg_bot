package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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

            switch (messageText) {
                case "/start":
                    sendStartMessage(chatId);
                    break;
                case "/play":
                    showCategorySelection(chatId);
                    break;
                case "/help":
                    sendHelpMessage(chatId);
                    break;
                default:
                    sendUnknownCommandMessage(chatId);
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackQueryId = update.getCallbackQuery().getId();

            answerCallbackQuery(callbackQueryId);

            if (callbackData.startsWith("category_")) {
                String categoryName = callbackData.substring(9);
                processCategorySelection(chatId, categoryName);
            } else if (callbackData.startsWith("answer_")) {
                int answerIndex = Integer.parseInt(callbackData.substring(7));
                processAnswer(chatId, answerIndex);
            }
        }
    }

    private void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }

    private void showCategorySelection(long chatId) {
        List<String> categories = questionRepository.getAvailableCategories();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(category);
            button.setCallbackData("category_" + category);
            row.add(button);
            rows.add(row);
        }

        keyboardMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🎯 Выберите категорию:");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }

    private void processCategorySelection(long chatId, String categoryName) {
        GameState gameState = new GameState();
        gameState.selectedCategory = categoryName;
        gameState.questions = questionRepository.getQuestionsByCategory(categoryName);
        userGameStates.put(chatId, gameState);

        sendMessage(chatId, "✅ Вы выбрали: " + categoryName + "\nНачинаем викторину!");
        sendNextQuestion(chatId);
    }

    private void sendNextQuestion(long chatId) {
        GameState gameState = userGameStates.get(chatId);

        if (gameState.currentQuestionIndex < gameState.questions.size()) {
            QuizQuestion currentQuestion = gameState.questions.get(gameState.currentQuestionIndex);

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<String> options = currentQuestion.getOptions();
            for (int i = 0; i < options.size(); i++) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(options.get(i));
                button.setCallbackData("answer_" + i);
                row.add(button);
                rows.add(row);
            }

            keyboardMarkup.setKeyboard(rows);

            String questionText = "📝 Категория: " + gameState.selectedCategory + "\n" +
                    "Вопрос " + (gameState.currentQuestionIndex + 1) + "/" + gameState.questions.size() + ":\n" +
                    currentQuestion.getQuestion();

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(questionText);
            message.setReplyMarkup(keyboardMarkup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("[ERR]: " + e.getMessage());
            }
        } else {
            finishGame(chatId);
        }
    }

    private void processAnswer(long chatId, int answerIndex) {
        GameState gameState = userGameStates.get(chatId);
        QuizQuestion currentQuestion = gameState.questions.get(gameState.currentQuestionIndex);

        boolean isCorrect = answerIndex == currentQuestion.getCorrectAnswerIndex();

        if (isCorrect) {
            gameState.correctAnswers++;
            sendMessage(chatId, "✅ Правильно!");
        } else {
            String correctAnswer = currentQuestion.getOptions().get(currentQuestion.getCorrectAnswerIndex());
            sendMessage(chatId, "❌ Неправильно! Правильный ответ: " + correctAnswer);
        }

        gameState.currentQuestionIndex++;

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (gameState.currentQuestionIndex < gameState.questions.size()) {
                sendNextQuestion(chatId);
            } else {
                finishGame(chatId);
            }
        }).start();
    }

    private void finishGame(long chatId) {
        GameState gameState = userGameStates.get(chatId);
        String result = "🎉 Викторина завершена!\n" +
                "Ваш результат: " + gameState.correctAnswers + "/" + gameState.questions.size() + " правильных ответов!\n\n" +
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
                /play - выбрать категорию и начать викторину
                
                Во время игры выбирайте ответы с помощью кнопок.""";

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
        String selectedCategory;
        List<QuizQuestion> questions;
    }
}