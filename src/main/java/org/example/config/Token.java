package org.example.config;

public class Token {
    public static String getBotToken() {
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("❌ BOT_TOKEN не установлен в переменных окружения!");
        }
        return token;
    }

    public static String getBotUsername() {
        String username = System.getenv("BOT_USERNAME");
        return username != null ? username : "QuizBot";
    }
}