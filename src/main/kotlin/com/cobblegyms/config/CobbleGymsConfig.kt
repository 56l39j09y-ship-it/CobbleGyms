package com.cobblegyms.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Main configuration for CobbleGyms loaded from cobblegyms.json.
 */
data class CobbleGymsConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val season: SeasonConfig = SeasonConfig(),
    val battle: BattleConfig = BattleConfig(),
    val rewards: RewardsConfig = RewardsConfig(),
    val smogon: SmogonConfig = SmogonConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val gui: GuiConfig = GuiConfig()
) {
    data class DatabaseConfig(
        val filePath: String = "config/cobblegyms/cobblegyms.db"
    )

    data class SeasonConfig(
        val durationDays: Int = 30,
        val autoReset: Boolean = true
    )

    data class BattleConfig(
        val queueTimeoutSeconds: Long = 300,
        val maxQueueSize: Int = 50,
        val allowSpectators: Boolean = true
    )

    data class RewardsConfig(
        val gymBadgeReward: Int = 1000,
        val e4WinReward: Int = 5000,
        val championReward: Int = 10000,
        val cooldownHours: Int = 24
    )

    data class SmogonConfig(
        val format: String = "gen9ou",
        val banListEnabled: Boolean = true,
        val bannedPokemon: List<String> = listOf(
            "Koraidon", "Miraidon", "Zacian", "Zamazenta", "Eternatus",
            "Kyogre", "Groudon", "Rayquaza", "Dialga", "Palkia", "Giratina",
            "Reshiram", "Zekrom", "Kyurem-White", "Kyurem-Black",
            "Xerneas", "Yveltal", "Zygarde-Complete",
            "Cosmog", "Cosmoem", "Solgaleo", "Lunala", "Necrozma-Dawn",
            "Necrozma-Dusk", "Calyrex-Shadow", "Calyrex-Ice"
        ),
        val bannedMoves: List<String> = listOf(
            "Baton Pass"
        ),
        val bannedAbilities: List<String> = listOf(
            "Arena Trap", "Shadow Tag", "Moody", "Power Construct"
        ),
        val teamSize: Int = 6,
        val maxLevel: Int = 100
    )

    data class DiscordConfig(
        val enabled: Boolean = false,
        val botToken: String = "",
        val channelId: String = "",
        val notifyGymWins: Boolean = true,
        val notifyE4Wins: Boolean = true,
        val notifyChampion: Boolean = true,
        val notifySeasonEnd: Boolean = true
    )

    data class GuiConfig(
        val useGui: Boolean = true,
        val mainMenuTitle: String = "§6✦ CobbleGyms ✦",
        val gymMenuTitle: String = "§e⚔ Gym Leaders",
        val e4MenuTitle: String = "§5★ Elite Four",
        val challengeMenuTitle: String = "§c⚡ Challenge Queue"
    )

    companion object {
        private val logger = LoggerFactory.getLogger("CobbleGyms/Config")
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        private const val CONFIG_FILE = "config/cobblegyms/config.json"

        private var instance: CobbleGymsConfig = CobbleGymsConfig()

        fun load(): CobbleGymsConfig {
            val file = File(CONFIG_FILE)
            if (!file.exists()) {
                logger.info("No config found, creating default config at $CONFIG_FILE")
                save(CobbleGymsConfig())
                return CobbleGymsConfig()
            }
            return try {
                FileReader(file).use { reader ->
                    gson.fromJson(reader, CobbleGymsConfig::class.java).also {
                        instance = it
                        logger.info("Configuration loaded from $CONFIG_FILE")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to load config, using defaults: ${e.message}")
                CobbleGymsConfig()
            }
        }

        fun save(config: CobbleGymsConfig = instance) {
            val file = File(CONFIG_FILE)
            file.parentFile?.mkdirs()
            try {
                FileWriter(file).use { writer ->
                    gson.toJson(config, writer)
                }
                instance = config
                logger.info("Configuration saved to $CONFIG_FILE")
            } catch (e: Exception) {
                logger.error("Failed to save config: ${e.message}")
            }
        }

        fun get(): CobbleGymsConfig = instance
    }
}
