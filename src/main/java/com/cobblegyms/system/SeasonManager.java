package com.cobblegyms.system;

import com.cobblegyms.config.CobbleGymsConfig;
import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.Season;
import com.cobblegyms.util.MessageUtil;
import com.cobblegyms.util.TimeUtil;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SeasonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static SeasonManager instance;

    private MinecraftServer server;
    private ScheduledExecutorService scheduler;
    private Season currentSeason;

    private SeasonManager() {}

    public static SeasonManager getInstance() {
        if (instance == null) instance = new SeasonManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        currentSeason = DatabaseManager.getInstance().getCurrentSeason();
        if (currentSeason == null) {
            startNewSeason();
        }
        startScheduler();
    }

    private void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CobbleGyms-SeasonChecker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkSeasonEnd, 1, 1, TimeUnit.HOURS);
    }

    public Season getCurrentSeason() {
        if (currentSeason == null) {
            currentSeason = DatabaseManager.getInstance().getCurrentSeason();
        }
        return currentSeason;
    }

    public void startNewSeason() {
        long now = System.currentTimeMillis();
        long end = now + CobbleGymsConfig.getInstance().getSeasonDurationMillis();
        currentSeason = DatabaseManager.getInstance().createSeason(now, end);
        if (currentSeason != null) {
            LOGGER.info("[CobbleGyms] New season {} started, ends at {}", currentSeason.getId(),
                    TimeUtil.formatTimestamp(end));
            if (server != null) {
                MessageUtil.broadcast(server, "\u00a7eSeason " + currentSeason.getId()
                        + " has started! Duration: " + CobbleGymsConfig.getInstance().seasonDurationDays + " days.");
            }
        }
    }

    public void checkSeasonEnd() {
        Season season = getCurrentSeason();
        if (season != null && season.isExpired()) {
            LOGGER.info("[CobbleGyms] Season {} has expired, ending it...", season.getId());
            onSeasonEnd(season);
        }
    }

    public void onSeasonEnd(Season season) {
        DatabaseManager.getInstance().deactivateSeason(season.getId());
        if (server != null) {
            server.execute(() -> {
                RewardManager.getInstance().giveSeasonEndRewards(season.getId());
                MessageUtil.broadcast(server, "\u00a76Season " + season.getId()
                        + " has ended! Congratulations to all participants!");
            });
        }
        startNewSeason();
    }

    public String getRemainingTime() {
        Season season = getCurrentSeason();
        if (season == null) return "No active season";
        return TimeUtil.formatDuration(season.getRemainingMillis());
    }

    public void forceEndSeason() {
        Season season = getCurrentSeason();
        if (season != null) {
            onSeasonEnd(season);
        }
    }

    public void shutdown() {
        if (scheduler != null) scheduler.shutdown();
    }
}
