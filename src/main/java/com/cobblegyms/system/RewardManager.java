package com.cobblegyms.system;

import com.cobblegyms.config.CobbleGymsConfig;
import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.model.WeeklyStats;
import com.cobblegyms.util.MessageUtil;
import com.cobblegyms.util.TimeUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RewardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static RewardManager instance;

    private MinecraftServer server;
    private ScheduledExecutorService scheduler;
    private long lastWeekStart = 0;

    private RewardManager() {}

    public static RewardManager getInstance() {
        if (instance == null) instance = new RewardManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        lastWeekStart = TimeUtil.getWeekStart();
        startScheduler();
    }

    private void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CobbleGyms-RewardManager");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkWeeklyRewards, 1, 1, TimeUnit.HOURS);
    }

    private void checkWeeklyRewards() {
        long currentWeekStart = TimeUtil.getWeekStart();
        if (currentWeekStart > lastWeekStart) {
            LOGGER.info("[CobbleGyms] Week ended, distributing rewards...");
            if (server != null) {
                server.execute(() -> {
                    checkWeeklyGymRewards();
                    giveChampionE4WeeklyRewards();
                    DiscordManager.getInstance().sendWeeklyReportAll(lastWeekStart);
                });
            }
            lastWeekStart = currentWeekStart;
        }
    }

    public void checkWeeklyGymRewards() {
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();
        long weekStart = lastWeekStart > 0 ? lastWeekStart : TimeUtil.getWeekStart();

        for (GymLeaderData leader : GymManager.getInstance().getAllGymLeaders()) {
            WeeklyStats stats = DatabaseManager.getInstance().getWeeklyStats(leader.getLeaderId(), weekStart);
            if (stats.getBattles() >= config.minBattlesForReward
                    && stats.getWinrate() >= config.minWinrateForReward) {
                executeRewardCommand(config.gymLeaderWeeklyRewardCommand, leader.getLeaderId(), leader.getUsername(), 1);
                LOGGER.info("[CobbleGyms] Gym leader {} qualifies for weekly reward ({} battles, {:.1f}% winrate)",
                        leader.getUsername(), stats.getBattles(), stats.getWinrate() * 100);
            }
        }
    }

    public void giveChampionE4WeeklyRewards() {
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();
        for (GymLeaderData e4 : GymManager.getInstance().getAllE4()) {
            executeRewardCommand(config.e4WeeklyRewardCommand, e4.getLeaderId(), e4.getUsername(), 1);
        }
        GymManager.getInstance().getChampion().ifPresent(champion ->
                executeRewardCommand(config.championWeeklyRewardCommand, champion.getLeaderId(), champion.getUsername(), 1));
    }

    public void giveSeasonEndRewards(int seasonId) {
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();
        for (Map.Entry<UUID, GymLeaderData> entry : GymManager.getInstance().getAllLeaders().entrySet()) {
            GymLeaderData leader = entry.getValue();
            long weekStart = TimeUtil.getWeekStart();
            WeeklyStats stats = DatabaseManager.getInstance().getWeeklyStats(leader.getLeaderId(), weekStart);
            int amount = Math.max(1, stats.getBattles() / 5);
            executeRewardCommand(config.seasonEndRewardCommand, leader.getLeaderId(), leader.getUsername(), amount);
        }
    }

    public void updateWeeklyStats(UUID leaderId, boolean won) {
        long weekStart = TimeUtil.getWeekStart();
        DatabaseManager.getInstance().updateWeeklyStats(leaderId, weekStart, won);
    }

    public WeeklyStats getWeeklyStats(UUID leaderId) {
        long weekStart = TimeUtil.getWeekStart();
        return DatabaseManager.getInstance().getWeeklyStats(leaderId, weekStart);
    }

    private void executeRewardCommand(String commandTemplate, UUID playerId, String username, int amount) {
        if (server == null || commandTemplate == null || commandTemplate.isEmpty()) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        String playerName = player != null ? player.getName().getString() : username;
        String command = commandTemplate
                .replace("%player%", playerName)
                .replace("%amount%", String.valueOf(amount));
        try {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Failed to execute reward command '{}': {}", command, e.getMessage());
        }
    }

    public void shutdown() {
        if (scheduler != null) scheduler.shutdown();
    }
}
