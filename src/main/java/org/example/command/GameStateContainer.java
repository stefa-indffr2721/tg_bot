package org.example.command;

import org.example.service.SessionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameStateContainer {
    private final SessionService sessionService;
    private final Map<Long, String> userNames;
    private final Map<Long, DuelRequest> pendingDuels;
    private final Map<String, List<Long>> duelQueue;

    public GameStateContainer() {
        this.sessionService = new SessionService();
        this.userNames = new HashMap<>();
        this.pendingDuels = new ConcurrentHashMap<>();
        this.duelQueue = new ConcurrentHashMap<>();
    }

    public SessionService getSessionService() {
        return sessionService;
    }

    public Map<Long, String> getUserNames() {
        return userNames;
    }

    public Map<Long, DuelRequest> getPendingDuels() {
        return pendingDuels;
    }

    public Map<String, List<Long>> getDuelQueue() {
        return duelQueue;
    }
}