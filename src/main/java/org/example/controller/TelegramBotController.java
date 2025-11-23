package org.example.controller;

import org.example.config.BotConfig;
import org.example.dispatcher.MessageDispatcher;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramBotController extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private final MessageDispatcher messageDispatcher;

    public TelegramBotController(BotConfig botConfig, MessageDispatcher messageDispatcher) {
        this.botConfig = botConfig;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onUpdateReceived(Update update) {
        messageDispatcher.dispatch(update, this);
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }
}
