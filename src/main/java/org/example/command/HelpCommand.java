package org.example.command;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import static org.example.util.MessageUtils.createMessage;

public class HelpCommand implements Command {
    @Override
    public void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException {
        String response = """
                📋 Справка по командам:
                
                /start - начать работу с ботом
                /help - показать эту справку
                /play - выбрать категорию и начать викторину
                
                Во время игры выбирайте ответы с помощью кнопок.""";

        bot.execute(createMessage(chatId, response));
    }
}
