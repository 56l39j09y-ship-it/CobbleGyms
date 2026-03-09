package com.cobblegyms

import com.cobblegyms.commands.ChallengeCommand
import com.cobblegyms.commands.GymsAdminCommand
import com.cobblegyms.commands.GymsCommand
import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import com.cobblegyms.discord.DiscordBot
import com.cobblegyms.events.BattleEventListener
import com.cobblegyms.managers.BattleManager
import com.cobblegyms.managers.ChampionManager
import com.cobblegyms.managers.E4Manager
import com.cobblegyms.managers.GymManager
import com.cobblegyms.managers.RewardSystem
import com.cobblegyms.managers.SeasonManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory

/**
 * CobbleGyms – Competitive Pokémon Gym System for Cobblemon/Fabric.
 *
 * Initialises all subsystems in order:
 *  1. Configuration
 *  2. Database
 *  3. Managers
 *  4. Commands
 *  5. Event Listeners
 *  6. Discord bot (optional)
 */
class CobbleGymsMod : ModInitializer {

    companion object {
        const val MOD_ID = "cobblegyms"
        val logger = LoggerFactory.getLogger(MOD_ID)
    }

    // Core managers – created after config + database are ready
    private lateinit var gymManager: GymManager
    private lateinit var e4Manager: E4Manager
    private lateinit var championManager: ChampionManager
    private lateinit var seasonManager: SeasonManager
    private lateinit var battleManager: BattleManager
    private lateinit var rewardSystem: RewardSystem
    private lateinit var discordBot: DiscordBot
    private lateinit var eventListener: BattleEventListener

    override fun onInitialize() {
        logger.info("CobbleGyms initialising…")

        loadConfiguration()
        initializeDatabase()
        initializeManagers()
        registerCommands()
        setupEventListeners()
        initializeDiscordBot()

        logger.info("CobbleGyms initialised successfully!")
    }

    // -------------------------------------------------------------------------
    // Initialisation steps
    // -------------------------------------------------------------------------

    private fun loadConfiguration() {
        CobbleGymsConfig.load()
        logger.info("Configuration loaded")
    }

    private fun initializeDatabase() {
        DatabaseManager.initialize(CobbleGymsConfig.get().database)
        logger.info("Database initialised")
    }

    private fun initializeManagers() {
        val config = CobbleGymsConfig.get()
        gymManager = GymManager()
        e4Manager = E4Manager()
        championManager = ChampionManager()
        seasonManager = SeasonManager(config.season)
        battleManager = BattleManager(config.battle)
        rewardSystem = RewardSystem(config.rewards)
        logger.info("Managers initialised")
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            GymsCommand(gymManager, e4Manager, championManager, seasonManager).register(dispatcher)
            ChallengeCommand(gymManager, e4Manager, championManager, battleManager).register(dispatcher)
            GymsAdminCommand(gymManager, e4Manager, championManager, seasonManager).register(dispatcher)
        }
        logger.info("Commands registered")
    }

    private fun setupEventListeners() {
        eventListener = BattleEventListener(
            gymManager, e4Manager, championManager, seasonManager, battleManager, rewardSystem
        )
        eventListener.register()
        logger.info("Event listeners registered")
    }

    private fun initializeDiscordBot() {
        discordBot = DiscordBot(CobbleGymsConfig.get().discord)
        discordBot.start()
    }
}
