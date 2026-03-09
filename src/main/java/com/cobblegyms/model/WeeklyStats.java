package com.cobblegyms.model;

import java.util.UUID;

public class WeeklyStats {
    private UUID leaderId;
    private long weekStart;
    private int battles;
    private int wins;
    private int losses;

    public WeeklyStats() {}

    public WeeklyStats(UUID leaderId, long weekStart) {
        this.leaderId = leaderId;
        this.weekStart = weekStart;
        this.battles = 0;
        this.wins = 0;
        this.losses = 0;
    }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public long getWeekStart() { return weekStart; }
    public void setWeekStart(long weekStart) { this.weekStart = weekStart; }

    public int getBattles() { return battles; }
    public void setBattles(int battles) { this.battles = battles; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public double getWinrate() {
        if (battles == 0) return 0.0;
        return (double) wins / battles;
    }

    public String getWinratePercent() {
        return String.format("%.1f%%", getWinrate() * 100);
    }

    public void recordWin() {
        battles++;
        wins++;
    }

    public void recordLoss() {
        battles++;
        losses++;
    }
}
