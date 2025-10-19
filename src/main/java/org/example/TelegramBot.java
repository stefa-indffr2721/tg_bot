package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, есть ли сообщение и текст в нем
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Обрабатываем команды
            switch (messageText) {
                case "/start":
                    sendStartMessage(chatId);
                    break;
                case "/help":
                    sendHelpMessage(chatId);
                    break;
                default:
                    sendUnknownCommandMessage(chatId);
                    break;
            }
        }
    }

    private void sendStartMessage(long chatId) {
        String response = """
                🚀 Добро пожаловать в SimpleBot!
                
                Я простой телеграм бот, созданный для демонстрации.
                Рад приветствовать вас!
                
                Используйте команду /help чтобы узнать что я умею.""";

        sendMessage(chatId, response);
    }

    private void sendHelpMessage(long chatId) {
        String response = """
                📋 Справка по командам:
                
                /start - начать работу с ботом
                /help - показать эту справку
                
                Это базовая версия бота с минимальным функционалом.""";

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
}