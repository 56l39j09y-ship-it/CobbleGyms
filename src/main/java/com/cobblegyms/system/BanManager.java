package com.cobblegyms.system;

import com.cobblegyms.config.CobbleGymsConfig;
import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.GymBan;
import com.cobblegyms.model.GymRole;
import com.cobblegyms.util.MessageUtil;
import com.cobblegyms.util.TimeUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");
    private static BanManager instance;

    private MinecraftServer server;
    private ScheduledExecutorService scheduler;

    private BanManager() {}

    public static BanManager getInstance() {
        if (instance == null) instance = new BanManager();
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CobbleGyms-BanCleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::cleanupExpiredBans, 10, 60, TimeUnit.MINUTES);
    }

    public void banFromGym(UUID bannedId, UUID bannerId, GymRole bannerRole, int hours, String reason) {
        int maxHours = CobbleGymsConfig.getInstance().tempBanMaxHours;
        hours = Math.min(hours, maxHours);
        long expiresAt = System.currentTimeMillis() + TimeUtil.hoursToMillis(hours);
        GymBan ban = new GymBan(bannedId, bannerId, bannerRole, expiresAt, reason);
        DatabaseManager.getInstance().insertBan(ban);
        LOGGER.info("[CobbleGyms] {} banned {} from gym for {} hours. Reason: {}", bannerId, bannedId, hours, reason);
        if (server != null) {
            ServerPlayerEntity banned = server.getPlayerManager().getPlayer(bannedId);
            if (banned != null) {
                MessageUtil.sendError(banned, "You have been temporarily banned from this gym for "
                        + hours + " hours. Reason: " + reason);
            }
        }
    }

    public void unbanFromGym(UUID bannedId, UUID leaderId) {
        DatabaseManager.getInstance().removeBan(bannedId, leaderId);
        if (server != null) {
            ServerPlayerEntity banned = server.getPlayerManager().getPlayer(bannedId);
            if (banned != null) {
                MessageUtil.sendSuccess(banned, "You have been unbanned from this gym.");
            }
        }
    }

    public boolean isBanned(UUID playerId, UUID leaderId) {
        return DatabaseManager.getInstance().isBanned(playerId, leaderId);
    }

    public void cleanupExpiredBans() {
        DatabaseManager.getInstance().cleanupExpiredBans();
    }

    public List<GymBan> getBansForGym(UUID leaderId) {
        return DatabaseManager.getInstance().getActiveBansForBanner(leaderId);
    }

    public void shutdown() {
        if (scheduler != null) scheduler.shutdown();
    }
}
