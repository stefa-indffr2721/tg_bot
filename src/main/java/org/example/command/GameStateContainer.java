package org.example.command;

import org.example.service.SessionService;
import java.util.HashMap;
import java.util.Map;

public class GameStateContainer {
    private final SessionService sessionService;
    private final Map<Long, String> userNames;

    public GameStateContainer() {
        this.sessionService = new SessionService();
        this.userNames = new HashMap<>();
    }

    public SessionService getSessionService() {
        return sessionService;
    }

    public Map<Long, String> getUserNames() {
        return userNames;
    }
}