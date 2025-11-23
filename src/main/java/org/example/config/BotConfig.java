package org.example.config;

public class BotConfig {
    private final String botToken;
    private final String botUsername;

    public BotConfig(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }
}