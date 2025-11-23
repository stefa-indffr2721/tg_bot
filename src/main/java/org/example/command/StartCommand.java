package org.example.command;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import static org.example.util.MessageUtils.createMessage;

public class StartCommand implements Command {
    @Override
    public void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        String response = """
                🚀 Добро пожаловать в QuizBot!
                
                Я бот для проведения викторин.
                
                Используйте /play чтобы начать игру!
                
                Используйте команду /help чтобы узнать что я умею.""";

        bot.execute(createMessage(chatId, response));
    }
}