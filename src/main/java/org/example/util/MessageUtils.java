package org.example.util;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class MessageUtils {

    public static SendMessage createMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        return message;
    }

    public static SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        return message;
    }
}
