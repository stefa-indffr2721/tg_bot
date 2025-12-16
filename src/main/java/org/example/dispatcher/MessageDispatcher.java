package org.example.dispatcher;

import org.example.command.*;
import org.example.model.GameState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

import static org.example.util.MessageUtils.createMessage;

public class MessageDispatcher {
    private final Map<String, Command> commands;
    private final PlayCommand playCommand;
    private final CategoryService categoryService;
    private final GameStateContainer gameStateContainer;
    private final DuelService duelService;

    public MessageDispatcher() {
        this.commands = new HashMap<>();
        this.gameStateContainer = new GameStateContainer();
        this.duelService = new DuelService(gameStateContainer);
        this.categoryService = new CategoryService(gameStateContainer, duelService);
        this.playCommand = new PlayCommand(gameStateContainer);

        initializeCommands();
    }

    private void initializeCommands() {
        commands.put("/start", new StartCommand());
        commands.put("/help", new HelpCommand());
        commands.put("/play", playCommand);
    }

    public void dispatch(Update update, TelegramLongPollingBot bot) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update, bot);
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update, bot);
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("query is too old") &&
                    !e.getMessage().contains("query ID is invalid")) {
                System.err.println("[ERR]: " + e.getMessage());
            }
        }
    }

    private void handleCallbackQuery(Update update, TelegramLongPollingBot bot) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackQueryId = update.getCallbackQuery().getId();

        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            bot.execute(answer);
        } catch (TelegramApiException e) {
            if (!e.getMessage().contains("query is too old") &&
                    !e.getMessage().contains("query ID is invalid")) {
                throw e;
            }
        }

        if (callbackData.startsWith("category_")) {
            String categoryName = callbackData.substring(9);
            categoryService.processCategorySelection(chatId, categoryName, update, bot);
        } else if (callbackData.startsWith("start_single_") || callbackData.startsWith("start_duel_")) {
            categoryService.processGameModeSelection(chatId, callbackData, bot);
        } else if (callbackData.startsWith("start_game_")) {
            playCommand.startGameWithTimer(chatId, bot);
        } else if (callbackData.startsWith("answer_")) {
            int answerIndex = Integer.parseInt(callbackData.substring(7));

            GameState gameState = gameStateContainer.getSessionService().getGameState(chatId);

            if (gameState != null && gameState.isDuelMode()) {
                duelService.processDuelAnswer(chatId, answerIndex, update, bot);
            } else {
                playCommand.processAnswer(chatId, answerIndex, update, bot);
            }
        } else if (callbackData.equals("cancel_duel")) {
            handleDuelCancel(chatId, bot);
        } else if (callbackData.equals("change_category")) {
            categoryService.showCategorySelection(chatId, bot);
        }
    }

    private void handleDuelCancel(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        String category = duelService.getPendingDuelCategory(chatId);
        duelService.cancelDuel(chatId, bot, true);

        if (category != null) {
            categoryService.showGameModeSelection(chatId, category, bot);
        }
    }

    private void handleMessage(Update update, TelegramLongPollingBot bot) throws TelegramApiException {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        Command command = commands.get(messageText);
        if (command != null) {
            command.execute(chatId, bot);
        } else {
            bot.execute(createMessage(chatId,
                    "❌ Неизвестная команда.\nИспользуйте /help для просмотра доступных команд."));
        }
    }
}