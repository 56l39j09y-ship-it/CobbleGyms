package com.cobblegyms

import com.cobblegyms.commands.ChallengeCommand
import com.cobblegyms.commands.GymsAdminCommand
import com.cobblegyms.commands.GymsCommand
import com.cobblegyms.data.DatabaseManager
import com.cobblegyms.gym.GymManager
import com.cobblegyms.battle.BattleManager
import com.cobblegyms.season.SeasonManager
import com.cobblegyms.rewards.RewardManager
import com.cobblegyms.discord.DiscordBotManager
import com.cobblegyms.config.GymConfig
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

object CobbleGyms : ModInitializer {
    const val MOD_ID = "cobblegyms"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)
    
    lateinit var server: MinecraftServer
        private set
    
    lateinit var gymManager: GymManager
        private set
    
    lateinit var battleManager: BattleManager
        private set
    
    lateinit var seasonManager: SeasonManager
        private set
    
    lateinit var rewardManager: RewardManager
        private set
    
    lateinit var discordBot: DiscordBotManager
        private set

    override fun onInitialize() {
        LOGGER.info("CobbleGyms initializing...")
        
        // Load config
        GymConfig.load()
        
        // Initialize database
        DatabaseManager.initialize()
        
        // Initialize managers
        gymManager = GymManager()
        battleManager = BattleManager()
        seasonManager = SeasonManager()
        rewardManager = RewardManager()
        discordBot = DiscordBotManager()
        
        // Register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            GymsCommand.register(dispatcher)
            ChallengeCommand.register(dispatcher)
            GymsAdminCommand.register(dispatcher)
        }
        
        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            server = srv
            gymManager.onServerStart()
            seasonManager.onServerStart()
            rewardManager.onServerStart()
            if (GymConfig.config.discord.enabled) {
                discordBot.start()
            }
            LOGGER.info("CobbleGyms started successfully!")
        }
        
        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            battleManager.onServerStop()
            discordBot.shutdown()
            DatabaseManager.close()
            LOGGER.info("CobbleGyms stopped.")
        }
        
        LOGGER.info("CobbleGyms initialized!")
    }
}
