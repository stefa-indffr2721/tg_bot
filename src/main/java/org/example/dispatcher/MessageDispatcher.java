package org.example.dispatcher;

import org.example.command.Command;
import org.example.command.HelpCommand;
import org.example.command.PlayCommand;
import org.example.command.StartCommand;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

public class MessageDispatcher {
    private final Map<String, Command> commands;
    private final PlayCommand playCommand;

    public MessageDispatcher() {
        this.commands = new HashMap<>();
        this.playCommand = new PlayCommand();

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
            System.err.println("[ERR]: " + e.getMessage());
        }
    }

    private void handleCallbackQuery(Update update, TelegramLongPollingBot bot) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackQueryId = update.getCallbackQuery().getId();

        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        bot.execute(answer);

        if (callbackData.startsWith("category_")) {
            String categoryName = callbackData.substring(9);
            playCommand.processCategorySelection(chatId, categoryName, bot);
        } else if (callbackData.startsWith("answer_")) {
            int answerIndex = Integer.parseInt(callbackData.substring(7));
            playCommand.processAnswer(chatId, answerIndex, bot);
        }
    }

    private void handleMessage(Update update, TelegramLongPollingBot bot) throws TelegramApiException {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        Command command = commands.get(messageText);
        if (command != null) {
            command.execute(chatId, bot);
        } else {
            bot.execute(org.example.util.MessageUtils.createMessage(chatId,
                    "❌ Неизвестная команда.\nИспользуйте /help для просмотра доступных команд."));
        }
    }
}