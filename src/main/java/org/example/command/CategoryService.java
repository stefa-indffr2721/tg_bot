package org.example.command;

import org.example.model.QuizQuestion;
import org.example.service.QuizService;
import org.example.service.SessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.example.util.MessageUtils.createMessage;

public class CategoryService {
    private final QuizService quizService;
    private final SessionService userSessionService;
    private final Map<Long, String> userNames;

    public CategoryService(GameStateContainer gameStateContainer) {
        this.quizService = new QuizService();
        this.userSessionService = gameStateContainer.getSessionService();
        this.userNames = gameStateContainer.getUserNames();
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
}