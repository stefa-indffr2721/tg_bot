package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Создаем экземпляр API телеграм ботов
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Регистрируем нашего бота
            botsApi.registerBot(new TelegramBot());

            System.out.println("Бот успешно запущен!");
            System.out.println("Имя бота: " + Token.BOT_USERNAME);

        } catch (TelegramApiException e) {
            System.err.println("[ERR]: " + e.getMessage());
        }
    }
}