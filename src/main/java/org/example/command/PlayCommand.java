package org.example.command;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import org.example.model.LeaderboardEntry;
import org.example.service.QuizService;
import org.example.service.SessionService;
import org.example.service.LeaderboardService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
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
    private final LeaderboardService leaderboardService;
    private final java.util.Map<Long, String> userNames = new java.util.HashMap<>();

    public PlayCommand() {
        this.quizService = new QuizService();
        this.userSessionService = new SessionService();
        this.leaderboardService = new LeaderboardService();
    }

    @Override
    public void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        showCategorySelection(chatId, bot);
    }

    private void showCategorySelection(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
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

    public void processCategorySelection(long chatId, String categoryName, Update update, TelegramLongPollingBot bot) throws TelegramApiException {
        if (update != null && update.hasCallbackQuery()) {
            User user = update.getCallbackQuery().getFrom();
            String userName = user.getFirstName();
            if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                userName += " " + user.getLastName();
            }
            userNames.put(chatId, userName);
        }

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

    public void startGameWithTimer(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState == null) {
            bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Давайте начнем сначала!"));
            showCategorySelection(chatId, bot);
            return;
        }

        gameState.setStartTime(System.currentTimeMillis());

        bot.execute(createMessage(chatId, "⏱ Таймер запущен! Начинаем викторину!"));
        sendNextQuestion(chatId, bot);
    }

    private void sendNextQuestion(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState == null) {
            bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Начните новую игру с помощью /play"));
            return;
        }

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

    public void processAnswer(long chatId, int answerIndex, Update update, TelegramLongPollingBot bot) {
        try {
            if (update != null && update.hasCallbackQuery()) {
                User user = update.getCallbackQuery().getFrom();
                String userName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    userName += " " + user.getLastName();
                }
                userNames.put(chatId, userName);
            }

            GameState gameState = userSessionService.getGameState(chatId);

            if (gameState == null) {
                bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Начните новую игру с помощью /play"));
                return;
            }

            if (gameState.getCurrentQuestionIndex() >= gameState.getQuestions().size()) {
                finishGame(chatId, bot);
                return;
            }

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

        if (gameState == null) {
            bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Начните новую игру с помощью /play"));
            return;
        }

        gameState.setEndTime(System.currentTimeMillis());

        long durationSeconds = gameState.getGameDuration();
        String timeString = formatTime(durationSeconds);

        saveResultToLeaderboard(chatId, gameState);

        String leaderboardText = getLeaderboardText(chatId, gameState);

        String result = "🎉 Викторина завершена!\n" +
                "Ваш результат: " + gameState.getCorrectAnswers() + "/" + gameState.getQuestions().size() + " правильных ответов!\n" +
                "⏱ Время: " + timeString + "\n\n" +
                leaderboardText + "\n\n" +
                "Хотите сыграть ещё раз?\n" +
                "/play\n" +
                "Или используйте команду /help чтобы узнать что я умею.";

        bot.execute(createMessage(chatId, result));
        userSessionService.removeGameState(chatId);
        userNames.remove(chatId);
    }

    private void saveResultToLeaderboard(long chatId, GameState gameState) {
        try {
            String playerName = userNames.getOrDefault(chatId, "Игрок " + chatId);

            leaderboardService.addResult(
                    playerName,
                    chatId,
                    gameState.getCorrectAnswers(),
                    gameState.getGameDuration(),
                    gameState.getSelectedCategory()
            );
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении в лидерборд: " + e.getMessage());
        }
    }

    private String getLeaderboardText(long chatId, GameState gameState) {
        String category = gameState.getSelectedCategory();
        List<LeaderboardEntry> topResults = leaderboardService.getTopResults(category, 10);
        int userPosition = leaderboardService.getUserPosition(chatId, category);
        int totalQuestions = gameState.getQuestions().size();

        StringBuilder leaderboardText = new StringBuilder();
        leaderboardText.append("🏆 **Лидерборд - ").append(category).append("**\n\n");

        if (topResults.isEmpty()) {
            leaderboardText.append("Пока нет результатов в этой категории.\n");
            leaderboardText.append("Вы первый! 🎉");
        } else {
            for (int i = 0; i < Math.min(topResults.size(), 10); i++) {
                LeaderboardEntry entry = topResults.get(i);
                String medal = getMedal(i);
                String timeFormatted = leaderboardService.formatTime(entry.getTimeSeconds());

                leaderboardText.append(medal)
                        .append(" **").append(entry.getCorrectAnswers()).append("/").append(totalQuestions)
                        .append("** ⏱ ").append(timeFormatted)
                        .append(" - ").append(entry.getPlayerName())
                        .append("\n");
            }

            leaderboardText.append("\n");

            if (userPosition > 0) {
                if (userPosition <= 10) {
                    leaderboardText.append("🎉 Вы в топ-10! Поздравляем!");
                } else {
                    leaderboardText.append("📊 Ваша позиция: **").append(userPosition).append("**");
                }
            } else {
                leaderboardText.append("📊 Ваш результат сохранен!");
            }
        }

        return leaderboardText.toString();
    }

    private String getMedal(int position) {
        return switch (position) {
            case 0 -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            default -> (position + 1) + ".";
        };
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