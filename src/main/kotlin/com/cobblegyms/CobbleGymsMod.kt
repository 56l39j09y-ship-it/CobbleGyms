package com.cobblegyms

import com.cobblegyms.command.AdminCommand
import com.cobblegyms.command.ChallengeCommand
import com.cobblegyms.command.GymsCommand
import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import com.cobblegyms.events.BattleEventListener
import com.cobblegyms.system.battle.BattleQueueManager
import com.cobblegyms.system.champion.ChampionManager
import com.cobblegyms.system.e4.EliteFourManager
import com.cobblegyms.system.gym.GymLeaderManager
import com.cobblegyms.system.rewards.RewardManager
import com.cobblegyms.system.season.SeasonManager
import com.cobblegyms.util.GymLogger
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CobbleGymsMod : ModInitializer {
    const val MOD_ID = "cobblegyms"
    val LOGGER = GymLogger.create(MOD_ID)

    lateinit var gymLeaderManager: GymLeaderManager
        private set
    lateinit var eliteFourManager: EliteFourManager
        private set
    lateinit var championManager: ChampionManager
        private set
    lateinit var seasonManager: SeasonManager
        private set
    lateinit var rewardManager: RewardManager
        private set

    override fun onInitialize() {
        LOGGER.info("Initializing CobbleGyms mod...")

        loadConfiguration()
        initializeDatabase()
        initializeManagers()
        setupEventListeners()
        registerCommands()

        LOGGER.info("CobbleGyms mod initialized successfully.")
    }

    private fun loadConfiguration() {
        CobbleGymsConfig.load()
        LOGGER.info("Configuration loaded.")
    }

    private fun initializeDatabase() {
        DatabaseManager.initialize()
        LOGGER.info("Database initialized.")
    }

    private fun initializeManagers() {
        gymLeaderManager = GymLeaderManager()
        eliteFourManager = EliteFourManager()
        championManager = ChampionManager()
        seasonManager = SeasonManager()
        rewardManager = RewardManager()

        BattleQueueManager.initialize()
        LOGGER.info("All managers initialized.")
    }

    private fun setupEventListeners() {
        BattleEventListener.register(gymLeaderManager, eliteFourManager, championManager)
        LOGGER.info("Event listeners registered.")
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            GymsCommand.register(dispatcher)
            ChallengeCommand.register(dispatcher)
            AdminCommand.register(dispatcher)
        }
        LOGGER.info("Commands registered.")
    }
}
