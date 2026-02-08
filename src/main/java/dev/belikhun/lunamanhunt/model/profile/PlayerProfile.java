package dev.belikhun.lunamanhunt.model.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerProfile {
    private final UUID playerId;
    private long bestTimeMillis;
    private final List<Long> runTimesMillis = new ArrayList<>();
    private final List<MatchHistoryEntry> matchHistory = new ArrayList<>();
    private int totalWins;
    private int totalLosses;

    public PlayerProfile(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getBestTimeMillis() {
        return bestTimeMillis;
    }

    public void setBestTimeMillis(long bestTimeMillis) {
        this.bestTimeMillis = bestTimeMillis;
    }

    public List<Long> getRunTimesMillis() {
        return runTimesMillis;
    }

    public List<MatchHistoryEntry> getMatchHistory() {
        return matchHistory;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = totalLosses;
    }
}
