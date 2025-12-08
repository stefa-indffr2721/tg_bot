package org.example;

import org.example.config.BotConfig;
import org.example.config.Token;
import org.example.controller.TelegramBotController;
import org.example.dispatcher.MessageDispatcher;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            BotConfig botConfig = new BotConfig(Token.BOT_TOKEN, Token.BOT_USERNAME);

            MessageDispatcher messageDispatcher = new MessageDispatcher();

            TelegramBotController bot = new TelegramBotController(botConfig, messageDispatcher);

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);

            System.out.println("Бот успешно запущен!");
            System.out.println("Имя бота: " + Token.BOT_USERNAME);

        } catch (TelegramApiException e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }
}