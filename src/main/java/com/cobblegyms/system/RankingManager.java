package com.cobblegyms.system;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.model.GymRole;
import com.cobblegyms.model.WeeklyStats;
import com.cobblegyms.util.TimeUtil;

import java.util.*;
import java.util.stream.Collectors;

public class RankingManager {
    private static RankingManager instance;

    private RankingManager() {}

    public static RankingManager getInstance() {
        if (instance == null) instance = new RankingManager();
        return instance;
    }

    public List<RankEntry> getTopGymLeaders(int limit) {
        long weekStart = TimeUtil.getWeekStart();
        List<WeeklyStats> allStats = DatabaseManager.getInstance().getAllWeeklyStats(weekStart);
        return allStats.stream()
                .filter(s -> {
                    GymLeaderData leader = GymManager.getInstance().getLeader(s.getLeaderId());
                    return leader != null && leader.getRole() == GymRole.GYM_LEADER;
                })
                .sorted(Comparator.comparingDouble(WeeklyStats::getWinrate).reversed()
                        .thenComparingInt(WeeklyStats::getBattles).reversed())
                .limit(limit)
                .map(s -> {
                    GymLeaderData leader = GymManager.getInstance().getLeader(s.getLeaderId());
                    return new RankEntry(s.getLeaderId(),
                            leader != null ? leader.getUsername() : s.getLeaderId().toString(),
                            s.getBattles(), s.getWins(), s.getLosses(), s.getWinrate());
                })
                .collect(Collectors.toList());
    }

    public List<RankEntry> getTopE4AndChampion(int limit) {
        long weekStart = TimeUtil.getWeekStart();
        List<WeeklyStats> allStats = DatabaseManager.getInstance().getAllWeeklyStats(weekStart);
        return allStats.stream()
                .filter(s -> {
                    GymLeaderData leader = GymManager.getInstance().getLeader(s.getLeaderId());
                    return leader != null && (leader.getRole() == GymRole.ELITE_FOUR
                            || leader.getRole() == GymRole.CHAMPION);
                })
                .sorted(Comparator.comparingDouble(WeeklyStats::getWinrate).reversed()
                        .thenComparingInt(WeeklyStats::getBattles).reversed())
                .limit(limit)
                .map(s -> {
                    GymLeaderData leader = GymManager.getInstance().getLeader(s.getLeaderId());
                    return new RankEntry(s.getLeaderId(),
                            leader != null ? leader.getUsername() : s.getLeaderId().toString(),
                            s.getBattles(), s.getWins(), s.getLosses(), s.getWinrate());
                })
                .collect(Collectors.toList());
    }

    public RankEntry getPlayerRanking(UUID playerId) {
        long weekStart = TimeUtil.getWeekStart();
        WeeklyStats stats = DatabaseManager.getInstance().getWeeklyStats(playerId, weekStart);
        GymLeaderData leader = GymManager.getInstance().getLeader(playerId);
        String name = leader != null ? leader.getUsername() : playerId.toString();
        return new RankEntry(playerId, name, stats.getBattles(), stats.getWins(), stats.getLosses(), stats.getWinrate());
    }

    public record RankEntry(UUID leaderId, String username, int battles, int wins, int losses, double winrate) {
        public String getWinratePercent() {
            return String.format("%.1f%%", winrate * 100);
        }
    }
}
