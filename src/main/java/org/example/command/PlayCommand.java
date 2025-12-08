package org.example.command;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class PlayCommand implements Command {
    private final CategoryService categoryService;
    private final GameManager gameManager;
    private final GameStateContainer gameStateContainer;

    public PlayCommand(GameStateContainer gameStateContainer) {
        this.gameStateContainer = gameStateContainer;
        this.categoryService = new CategoryService(gameStateContainer, new DuelService(gameStateContainer));
        this.gameManager = new GameManager(gameStateContainer);
    }

    public PlayCommand() {
        this.gameStateContainer = new GameStateContainer();
        this.categoryService = new CategoryService(gameStateContainer, new DuelService(gameStateContainer));
        this.gameManager = new GameManager(gameStateContainer);
    }

    @Override
    public void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        categoryService.showCategorySelection(chatId, bot);
    }

    public void processCategorySelection(long chatId, String categoryName, Update update, TelegramLongPollingBot bot) throws TelegramApiException {
        categoryService.processCategorySelection(chatId, categoryName, update, bot);
    }

    public void startGameWithTimer(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        gameManager.startGameWithTimer(chatId, bot);
    }

    public void processAnswer(long chatId, int answerIndex, Update update, TelegramLongPollingBot bot) {
        gameManager.processAnswer(chatId, answerIndex, update, bot);
    }
}