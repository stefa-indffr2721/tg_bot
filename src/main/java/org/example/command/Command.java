package org.example.command;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface Command {
    void execute(long chatId, TelegramLongPollingBot bot) throws TelegramApiException;
}