package com.cobblegyms

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object CobbleGymsMod : ModInitializer {
    const val MOD_ID = "cobblegyms"
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("Initializing CobbleGyms Mod for Minecraft 1.21.1")
        
        // Initialize managers
        GymManager.initialize()
        E4Manager.initialize()
        ChampionManager.initialize()
        SeasonManager.initialize()
        ChallengeQueue.initialize()
        Database.initialize()
        
        // Register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            GymsCommand.register(dispatcher)
            ChallengeCommand.register(dispatcher)
            AdminCommand.register(dispatcher)
        }
        
        LOGGER.info("CobbleGyms loaded successfully!")
    }
}