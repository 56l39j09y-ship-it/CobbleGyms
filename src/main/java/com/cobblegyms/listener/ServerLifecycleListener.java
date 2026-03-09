package com.cobblegyms.listener;

import com.cobblegyms.config.CobbleGymsConfig;
import com.cobblegyms.config.SmogonBanConfig;
import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.discord.DiscordManager;
import com.cobblegyms.system.*;
import com.cobblegyms.team.TeamEquipManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLifecycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleListener::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleListener::onServerStopping);
    }

    private static void onServerStarting(MinecraftServer server) {
        LOGGER.info("[CobbleGyms] Server starting, initializing CobbleGyms...");
        try {
            // Load configs
            CobbleGymsConfig.load();
            SmogonBanConfig.load();

            // Initialize database
            DatabaseManager.getInstance().initialize();

            // Initialize all systems
            GymManager.getInstance().initialize(server);
            SeasonManager.getInstance().initialize(server);
            QueueManager.getInstance().initialize(server);
            BattleManager.getInstance().initialize(server);
            RewardManager.getInstance().initialize(server);
            BanManager.getInstance().initialize(server);
            TeamManager.getInstance().initialize(server);
            TeamEquipManager.getInstance().initialize(server);

            // Initialize Discord
            DiscordManager.getInstance().initialize();

            LOGGER.info("[CobbleGyms] All systems initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Failed to initialize: {}", e.getMessage(), e);
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        LOGGER.info("[CobbleGyms] Server stopping, cleaning up CobbleGyms...");
        try {
            SeasonManager.getInstance().shutdown();
            RewardManager.getInstance().shutdown();
            BanManager.getInstance().shutdown();
            DiscordManager.getInstance().shutdown();
            DatabaseManager.getInstance().close();
            LOGGER.info("[CobbleGyms] Cleanup complete.");
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Error during cleanup: {}", e.getMessage(), e);
        }
    }
}
