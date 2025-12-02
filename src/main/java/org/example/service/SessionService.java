package org.example.service;

import org.example.model.GameState;
import org.example.model.QuizQuestion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionService {
    private final Map<Long, GameState> userGameStates;

    public SessionService() {
        this.userGameStates = new HashMap<>();
    }

    public void startNewGame(long chatId, String category, List<QuizQuestion> questions) {
        GameState gameState = new GameState();
        gameState.setSelectedCategory(category);
        gameState.setQuestions(questions);
        gameState.setStartTime(0);
        gameState.setEndTime(0);
        userGameStates.put(chatId, gameState);
    }

    public GameState getGameState(long chatId) {
        return userGameStates.get(chatId);
    }

    public void removeGameState(long chatId) {
        userGameStates.remove(chatId);
    }

    public boolean hasActiveGame(long chatId) {
        return userGameStates.containsKey(chatId);
    }
}