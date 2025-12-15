package org.example.command;

public class DuelRequest {
    private final long playerChatId;
    private final String category;

    public DuelRequest(long playerChatId, String category) {
        this.playerChatId = playerChatId;
        this.category = category;
    }

    public long getPlayerChatId() {
        return playerChatId;
    }

    public String getCategory() {
        return category;
    }
}