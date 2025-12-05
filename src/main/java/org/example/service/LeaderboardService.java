package org.example.service;

import org.example.model.LeaderboardEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LeaderboardService {
    private static final String LEADERBOARD_FILE = "leaderboard.json";
    private final ObjectMapper objectMapper;

    public LeaderboardService() {
        this.objectMapper = new ObjectMapper();
    }

    public void addResult(String playerName, long chatId, int correctAnswers, long timeSeconds, String category) {
        try {
            Map<String, List<LeaderboardEntry>> leaderboard = loadLeaderboard();

            List<LeaderboardEntry> categoryResults = leaderboard.getOrDefault(category, new ArrayList<>());

            LeaderboardEntry newEntry = new LeaderboardEntry(playerName, chatId, correctAnswers, timeSeconds, category);
            categoryResults.add(newEntry);

            selectionSort(categoryResults);

            if (categoryResults.size() > 100) {
                categoryResults = categoryResults.subList(0, 100);
            }

            leaderboard.put(category, categoryResults);
            saveLeaderboard(leaderboard);

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении результата в лидерборд: " + e.getMessage());
        }
    }

    private void selectionSort(List<LeaderboardEntry> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            int bestIndex = i;

            for (int j = i + 1; j < list.size(); j++) {
                LeaderboardEntry current = list.get(j);
                LeaderboardEntry best = list.get(bestIndex);

                if (current.getCorrectAnswers() > best.getCorrectAnswers()) {
                    bestIndex = j;
                }
                else if (current.getCorrectAnswers() == best.getCorrectAnswers()) {
                    if (current.getTimeSeconds() < best.getTimeSeconds()) {
                        bestIndex = j;
                    }
                }
            }

            if (bestIndex != i) {
                LeaderboardEntry temp = list.get(i);
                list.set(i, list.get(bestIndex));
                list.set(bestIndex, temp);
            }
        }
    }

    public List<LeaderboardEntry> getTopResults(String category, int limit) {
        try {
            Map<String, List<LeaderboardEntry>> leaderboard = loadLeaderboard();
            List<LeaderboardEntry> categoryResults = leaderboard.getOrDefault(category, new ArrayList<>());

            return categoryResults.stream()
                    .limit(limit)
                    .toList();

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке лидерборда: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public int getUserPosition(long chatId, String category) {
        try {
            Map<String, List<LeaderboardEntry>> leaderboard = loadLeaderboard();
            List<LeaderboardEntry> categoryResults = leaderboard.getOrDefault(category, new ArrayList<>());

            for (int i = 0; i < categoryResults.size(); i++) {
                if (categoryResults.get(i).getChatId() == chatId) {
                    return i + 1;
                }
            }
            return -1;

        } catch (IOException e) {
            System.err.println("Ошибка при получении позиции пользователя: " + e.getMessage());
            return -1;
        }
    }

    private Map<String, List<LeaderboardEntry>> loadLeaderboard() throws IOException {
        File file = new File(LEADERBOARD_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }

        return objectMapper.readValue(file, new TypeReference<Map<String, List<LeaderboardEntry>>>() {});
    }

    private void saveLeaderboard(Map<String, List<LeaderboardEntry>> leaderboard) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(LEADERBOARD_FILE), leaderboard);
    }
}