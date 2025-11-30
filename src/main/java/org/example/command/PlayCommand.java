package org.example.command;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import org.example.service.QuizService;
import org.example.service.SessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.example.util.MessageUtils.createMessage;

public class PlayCommand implements Command {
    private final QuizService quizService;
    private final SessionService userSessionService;

    public PlayCommand() {
        this.quizService = new QuizService();
        this.userSessionService = new SessionService();
    }

    @Override
    public void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        showCategorySelection(chatId, bot);
    }

    public void showCategorySelection(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        List<String> categories = quizService.getAvailableCategories();

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

        SendMessage message = createMessage(chatId, "🎯 Выберите категорию:");
        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }

    public void processCategorySelection(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        List<QuizQuestion> questions = quizService.getQuestionsByCategory(categoryName);
        userSessionService.startNewGame(chatId, categoryName, questions);

        showStartConfirmation(chatId, categoryName, bot);
    }

    private void showStartConfirmation(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton startButton = new InlineKeyboardButton();
        startButton.setText("🚀 Начать игру!");
        startButton.setCallbackData("start_game_" + categoryName);
        row.add(startButton);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);

        String messageText = "✅ Вы выбрали: " + categoryName + "\n\n" +
                "⏰ Как только вы нажмете кнопку, запустится таймер.\n" +
                "Вы хотите начать игру?";

        SendMessage message = createMessage(chatId, messageText);
        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }

    public void startGameWithTimer(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);
        gameState.setStartTime(System.currentTimeMillis());

        bot.execute(createMessage(chatId, "⏱ Таймер запущен! Начинаем викторину!"));
        sendNextQuestion(chatId, bot);
    }

    private void sendNextQuestion(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState.getCurrentQuestionIndex() < gameState.getQuestions().size()) {
            QuizQuestion currentQuestion = gameState.getQuestions().get(gameState.getCurrentQuestionIndex());

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

            String questionText = "📝 Категория: " + gameState.getSelectedCategory() + "\n" +
                    "Вопрос " + (gameState.getCurrentQuestionIndex() + 1) + "/" + gameState.getQuestions().size() + ":\n" +
                    currentQuestion.getQuestion();

            if (currentQuestion.getImage() != null && !currentQuestion.getImage().isEmpty()) {
                sendPhotoQuestion(chatId, questionText, currentQuestion.getImage(), keyboardMarkup, bot);
            } else {
                SendMessage message = createMessage(chatId, questionText);
                message.setReplyMarkup(keyboardMarkup);
                bot.execute(message);
            }
        } else {
            finishGame(chatId, bot);
        }
    }

    private void sendPhotoQuestion(long chatId, String caption, String imageName, InlineKeyboardMarkup keyboardMarkup, TelegramLongPollingBot bot)
            throws TelegramApiException {
        try {
            String resourcesPath = "src/main/resources/";
            File imageFile = new File(resourcesPath + imageName);

            if (imageFile.exists()) {
                SendPhoto photoMessage = new SendPhoto();
                photoMessage.setChatId(String.valueOf(chatId));
                photoMessage.setPhoto(new InputFile(imageFile, imageName));
                photoMessage.setCaption(caption);
                photoMessage.setReplyMarkup(keyboardMarkup);
                bot.execute(photoMessage);
            } else {
                InputStream imageStream = getClass().getClassLoader().getResourceAsStream(imageName);
                if (imageStream != null) {

                    File tempFile = File.createTempFile("telegram_bot_", "_" + imageName);
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = imageStream.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    SendPhoto photoMessage = new SendPhoto();
                    photoMessage.setChatId(String.valueOf(chatId));
                    photoMessage.setPhoto(new InputFile(tempFile, imageName));
                    photoMessage.setCaption(caption);
                    photoMessage.setReplyMarkup(keyboardMarkup);
                    bot.execute(photoMessage);

                    tempFile.deleteOnExit();
                } else {
                    SendMessage message = createMessage(chatId, caption + "\n\n[Изображение недоступно]");
                    message.setReplyMarkup(keyboardMarkup);
                    bot.execute(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending photo: " + e.getMessage());
            e.printStackTrace();
            SendMessage message = createMessage(chatId, caption + "\n\n[Ошибка загрузки изображения]");
            message.setReplyMarkup(keyboardMarkup);
            bot.execute(message);
        }
    }

    public void processAnswer(long chatId, int answerIndex, TelegramLongPollingBot bot) {
        try {
            GameState gameState = userSessionService.getGameState(chatId);
            QuizQuestion currentQuestion = gameState.getQuestions().get(gameState.getCurrentQuestionIndex());

            boolean isCorrect = quizService.validateAnswer(currentQuestion, answerIndex);

            if (isCorrect) {
                gameState.incrementCorrectAnswers();
                bot.execute(createMessage(chatId, "✅ Правильно!"));
            } else {
                String correctAnswer = quizService.getCorrectAnswer(currentQuestion);
                bot.execute(createMessage(chatId, "❌ Неправильно! Правильный ответ: " + correctAnswer));
            }

            gameState.incrementQuestionIndex();

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    if (gameState.getCurrentQuestionIndex() < gameState.getQuestions().size()) {
                        sendNextQuestion(chatId, bot);
                    } else {
                        finishGame(chatId, bot);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (TelegramApiException e) {
                    System.err.println("[ERR]: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }

    private void finishGame(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        gameState.setEndTime(System.currentTimeMillis());

        long durationSeconds = gameState.getGameDuration();
        String timeString = formatTime(durationSeconds);

        String result = "🎉 Викторина завершена!\n" +
                "Ваш результат: " + gameState.getCorrectAnswers() + "/" + gameState.getQuestions().size() + " правильных ответов!\n" +
                "⏱ Время: " + timeString + "\n\n" +
                "Хотите сыграть ещё раз?\n" +
                "/play\n" +
                "Или используйте команду /help чтобы узнать что я умею.";

        bot.execute(createMessage(chatId, result));
        userSessionService.removeGameState(chatId);
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0) {
            return String.format("%d мин. %d сек.", minutes, seconds);
        } else {
            return String.format("%d сек.", seconds);
        }
    }
}