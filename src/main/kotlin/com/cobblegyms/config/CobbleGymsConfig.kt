package com.cobblegyms.config

import com.cobblegyms.CobbleGymsMod
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object CobbleGymsConfig {
    // Season settings
    var seasonDurationDays: Int = 30
    var maxGymLeaders: Int = 8
    var maxEliteFourMembers: Int = 4

    // Reward settings (7-day cooldown in milliseconds)
    val rewardCooldownMs: Long = 7 * 24 * 60 * 60 * 1000L
    var gymLeaderWeeklyReward: Int = 2000
    var e4WeeklyReward: Int = 5000
    var championWeeklyReward: Int = 10000
    var rewardCurrency: String = "coins"

    // Battle queue settings (5-minute timeout)
    val queueTimeoutMs: Long = 5 * 60 * 1000L

    // Gym leader requirement settings
    var gymLeaderMinBattles: Int = 25
    var gymLeaderMinWinrate: Double = 50.0

    // Smogon ban list defaults
    var defaultBannedSpecies: List<String> = listOf(
        "arceus", "mewtwo", "mew", "rayquaza", "kyogre", "groudon",
        "dialga", "palkia", "giratina", "zekrom", "reshiram", "kyurem"
    )
    var allowMega: Boolean = false
    var allowDynamax: Boolean = false
    var maxTeamSize: Int = 6
    var minTeamSize: Int = 1

    // Database settings
    var databaseFile: String = "cobblegyms.db"
    var useExternalDb: Boolean = false

    fun load() {
        try {
            val configFile = File("config/cobblegyms.properties")
            if (!configFile.exists()) {
                configFile.parentFile.mkdirs()
                save(configFile)
                return
            }
            val props = Properties()
            FileInputStream(configFile).use { props.load(it) }

            seasonDurationDays = props.getProperty("season_duration_days", "30").toIntOrNull() ?: 30
            maxGymLeaders = props.getProperty("max_gym_leaders", "8").toIntOrNull() ?: 8
            maxEliteFourMembers = props.getProperty("max_elite_four_members", "4").toIntOrNull() ?: 4
            gymLeaderWeeklyReward = props.getProperty("gym_leader_weekly_reward", "2000").toIntOrNull() ?: 2000
            e4WeeklyReward = props.getProperty("e4_weekly_reward", "5000").toIntOrNull() ?: 5000
            championWeeklyReward = props.getProperty("champion_weekly_reward", "10000").toIntOrNull() ?: 10000
            rewardCurrency = props.getProperty("reward_currency", "coins")
            gymLeaderMinBattles = props.getProperty("gym_leader_min_battles", "25").toIntOrNull() ?: 25
            gymLeaderMinWinrate = props.getProperty("gym_leader_min_winrate", "50.0").toDoubleOrNull() ?: 50.0
            allowMega = props.getProperty("allow_mega", "false").toBoolean()
            allowDynamax = props.getProperty("allow_dynamax", "false").toBoolean()
            maxTeamSize = props.getProperty("max_team_size", "6").toIntOrNull() ?: 6
            minTeamSize = props.getProperty("min_team_size", "1").toIntOrNull() ?: 1
            databaseFile = props.getProperty("database_file", "cobblegyms.db")

            CobbleGymsMod.LOGGER.info("CobbleGyms configuration loaded from $configFile")
        } catch (e: Exception) {
            CobbleGymsMod.LOGGER.warn("Failed to load CobbleGyms config, using defaults: ${e.message}")
        }
    }

    private fun save(file: File) {
        val props = Properties()
        props["season_duration_days"] = seasonDurationDays.toString()
        props["max_gym_leaders"] = maxGymLeaders.toString()
        props["max_elite_four_members"] = maxEliteFourMembers.toString()
        props["gym_leader_weekly_reward"] = gymLeaderWeeklyReward.toString()
        props["e4_weekly_reward"] = e4WeeklyReward.toString()
        props["champion_weekly_reward"] = championWeeklyReward.toString()
        props["reward_currency"] = rewardCurrency
        props["gym_leader_min_battles"] = gymLeaderMinBattles.toString()
        props["gym_leader_min_winrate"] = gymLeaderMinWinrate.toString()
        props["allow_mega"] = allowMega.toString()
        props["allow_dynamax"] = allowDynamax.toString()
        props["max_team_size"] = maxTeamSize.toString()
        props["min_team_size"] = minTeamSize.toString()
        props["database_file"] = databaseFile
        file.outputStream().use { props.store(it, "CobbleGyms Configuration") }
    }
}
