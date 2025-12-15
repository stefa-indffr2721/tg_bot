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
    private final DuelService duelService;

    public CategoryService(GameStateContainer gameStateContainer, DuelService duelService) {
        this.quizService = new QuizService();
        this.userSessionService = gameStateContainer.getSessionService();
        this.userNames = gameStateContainer.getUserNames();
        this.duelService = duelService;
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

        showGameModeSelection(chatId, categoryName, bot);
    }

    public void showGameModeSelection(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> singleRow = new ArrayList<>();
        InlineKeyboardButton singleButton = new InlineKeyboardButton();
        singleButton.setText("🎮 Начать игру");
        singleButton.setCallbackData("start_single_" + categoryName);
        singleRow.add(singleButton);
        rows.add(singleRow);

        List<InlineKeyboardButton> duelRow = new ArrayList<>();
        InlineKeyboardButton duelButton = new InlineKeyboardButton();
        duelButton.setText("⚔️ Дуэль");
        duelButton.setCallbackData("start_duel_" + categoryName);
        duelRow.add(duelButton);
        rows.add(duelRow);

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("↩️ Изменить категорию");
        backButton.setCallbackData("change_category");
        backRow.add(backButton);
        rows.add(backRow);

        keyboardMarkup.setKeyboard(rows);

        String messageText = "✅ Вы выбрали: " + categoryName + "\n\n" +
                "Выберите режим игры:\n" +
                "🎮 <b>Начать игру</b> - игра в одиночку\n" +
                "⚔️ <b>Дуэль</b> - игра против другого игрока";

        SendMessage message = createMessage(chatId, messageText);
        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }

    public void processGameModeSelection(long chatId, String callbackData, TelegramLongPollingBot bot) throws TelegramApiException {
        String categoryName;
        if (callbackData.startsWith("start_single_")) {
            categoryName = callbackData.substring("start_single_".length());
            showSingleGameStartConfirmation(chatId, categoryName, bot);
        } else if (callbackData.startsWith("start_duel_")) {
            categoryName = callbackData.substring("start_duel_".length());
            duelService.startDuelSearch(chatId, categoryName, bot);
        } else {
            bot.execute(createMessage(chatId, "❌ Неизвестный режим игры."));
        }
    }

    private void showSingleGameStartConfirmation(long chatId, String categoryName, TelegramLongPollingBot bot) throws TelegramApiException {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton startButton = new InlineKeyboardButton();
        startButton.setText("🚀 Начать игру!");
        startButton.setCallbackData("start_game_" + categoryName);
        row.add(startButton);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);

        String messageText = "🎮 <b>Одиночная игра</b>\n" +
                "Категория: " + categoryName + "\n\n" +
                "⏰ Как только вы нажмете кнопку, запустится таймер.\n" +
                "Вы хотите начать игру?";

        SendMessage message = createMessage(chatId, messageText);
        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }
}