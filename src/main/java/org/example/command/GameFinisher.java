package org.example.command;

import org.example.model.GameState;
import org.example.model.LeaderboardEntry;
import org.example.service.LeaderboardService;
import org.example.service.SessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;

import static org.example.util.MessageUtils.createMessage;

public class GameFinisher {
    private final LeaderboardService leaderboardService;

    public GameFinisher() {
        this.leaderboardService = new LeaderboardService();
    }

    public void finishGame(long chatId, TelegramLongPollingBot bot, SessionService userSessionService, Map<Long, String> userNames) throws TelegramApiException {
        GameState gameState = userSessionService.getGameState(chatId);

        if (gameState == null) {
            bot.execute(createMessage(chatId, "⚠️ Игра не найдена. Начните новую игру с помощью /play"));
            return;
        }

        gameState.setEndTime(System.currentTimeMillis());

        long durationSeconds = gameState.getGameDuration();
        String timeString = formatTime(durationSeconds);

        saveResultToLeaderboard(chatId, gameState, userNames);

        String leaderboardText = getLeaderboardText(chatId, gameState);

        String result = "🎉 Викторина завершена!\n" +
                "Ваш результат: " + gameState.getCorrectAnswers() + "/" + gameState.getQuestions().size() + " правильных ответов!\n" +
                "⏱ Время: " + timeString + "\n\n" +
                leaderboardText + "\n\n" +
                "Хотите сыграть ещё раз?\n" +
                "/play\n" +
                "Или используйте команду /help чтобы узнать что я умею.";

        bot.execute(createMessage(chatId, result));
        userSessionService.removeGameState(chatId);
        userNames.remove(chatId);
    }

    private void saveResultToLeaderboard(long chatId, GameState gameState, Map<Long, String> userNames) {
        try {
            String playerName = userNames.getOrDefault(chatId, "Игрок " + chatId);

            leaderboardService.addResult(
                    playerName,
                    chatId,
                    gameState.getCorrectAnswers(),
                    gameState.getGameDuration(),
                    gameState.getSelectedCategory()
            );
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении в лидерборд: " + e.getMessage());
        }
    }

    private String getLeaderboardText(long chatId, GameState gameState) {
        String category = gameState.getSelectedCategory();
        List<LeaderboardEntry> topResults = leaderboardService.getTopResults(category, 10);
        int userPosition = leaderboardService.getUserPosition(chatId, category);
        int totalQuestions = gameState.getQuestions().size();

        StringBuilder leaderboardText = new StringBuilder();
        leaderboardText.append("🏆 ТОП-10 - ").append(category).append("\n\n");

        for (int i = 0; i < Math.min(topResults.size(), 10); i++) {
            LeaderboardEntry entry = topResults.get(i);
            String medal = getMedal(i);
            String timeFormatted = formatTime(entry.getTimeSeconds());

            leaderboardText.append(medal)
                    .append(" ").append(entry.getCorrectAnswers()).append("/").append(totalQuestions)
                    .append(" ⏱ ").append(timeFormatted)
                    .append(" - ").append(entry.getPlayerName())
                    .append("\n");
        }

        leaderboardText.append("\n");

        if (userPosition > 0) {
            if (userPosition <= 10) {
                leaderboardText.append("🎉 Вы в топ-10! Поздравляем!");
            } else {
                leaderboardText.append("📊 Ваша позиция: **").append(userPosition).append("**");
            }
        } else {
            leaderboardText.append("📊 Ваш результат сохранен!");
        }

        return leaderboardText.toString();
    }

    private String getMedal(int position) {
        return switch (position) {
            case 0 -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            default -> (position + 1) + ".";
        };
    }

    public String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0) {
            return String.format("%d мин. %d сек.", minutes, seconds);
        } else {
            return String.format("%d сек.", seconds);
        }
    }
}