package com.cobblegyms;

import com.cobblegyms.command.ChallengeCommand;
import com.cobblegyms.command.GymsAdminCommand;
import com.cobblegyms.command.GymsCommand;
import com.cobblegyms.listener.BattleEventListener;
import com.cobblegyms.listener.PlayerJoinListener;
import com.cobblegyms.listener.ServerLifecycleListener;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleGyms implements ModInitializer {
    public static final String MOD_ID = "cobblegyms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[CobbleGyms] Initializing CobbleGyms mod...");

        // Register lifecycle listeners first (handles database init, system startup)
        ServerLifecycleListener.register();

        // Register event listeners
        PlayerJoinListener.register();
        BattleEventListener.register();

        // Register commands
        GymsCommand.register();
        ChallengeCommand.register();
        GymsAdminCommand.register();

        LOGGER.info("[CobbleGyms] CobbleGyms initialized successfully!");
    }
}
