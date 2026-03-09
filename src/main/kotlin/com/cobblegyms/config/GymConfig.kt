package com.cobblegyms.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.io.File

@Serializable
data class GymConfigData(
    val season: SeasonConfig = SeasonConfig(),
    val rewards: RewardConfig = RewardConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val battle: BattleConfig = BattleConfig(),
    val smogon: SmogonConfig = SmogonConfig()
)

@Serializable
data class SeasonConfig(
    val durationDays: Int = 30,
    val autoReset: Boolean = true
)

@Serializable
data class RewardConfig(
    val leaderMinWeeklyBattles: Int = 25,
    val leaderMinWinrate: Double = 0.5,
    val leaderRewardCommand: String = "give {player} minecraft:diamond 5",
    val eliteFourRewardCommand: String = "give {player} minecraft:diamond_block 1",
    val championRewardCommand: String = "give {player} minecraft:netherite_ingot 1"
)

@Serializable
data class DiscordConfig(
    val enabled: Boolean = false,
    val token: String = "",
    val guildId: String = "",
    val battleLogChannelId: String = "",
    val adminChannelId: String = ""
)

@Serializable
data class BattleConfig(
    val challengeCooldownHours: Int = 24,
    val maxQueueSize: Int = 10,
    val battleTimeoutMinutes: Int = 60,
    val showTeamBeforeBattle: Boolean = true
)

@Serializable
data class SmogonConfig(
    val format: String = "nationaldex",
    val bannedPokemon: List<String> = listOf(
        "Mewtwo-Mega-X", "Mewtwo-Mega-Y", "Rayquaza-Mega", "Kyogre-Primal", "Groudon-Primal",
        "Zacian-Crowned", "Zamazenta-Crowned", "Eternatus-Eternamax"
    ),
    val bannedMoves: List<String> = listOf("Evasion moves", "OHKO moves", "Moody"),
    val bannedAbilities: List<String> = listOf("Arena Trap", "Shadow Tag", "Moody"),
    val bannedItems: List<String> = listOf(),
    val clauses: List<String> = listOf(
        "Sleep Clause", "Evasion Clause", "OHKO Clause", "Species Clause", "Endless Battle Clause"
    )
)

object GymConfig {
    private val configFile: File by lazy {
        FabricLoader.getInstance().configDir.resolve("cobblegyms.json").toFile()
    }
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    var config: GymConfigData = GymConfigData()
        private set
    
    fun load() {
        if (configFile.exists()) {
            try {
                config = json.decodeFromString(configFile.readText())
            } catch (e: Exception) {
                config = GymConfigData()
            }
        }
        save()
    }
    
    fun save() {
        configFile.parentFile?.mkdirs()
        configFile.writeText(json.encodeToString(GymConfigData.serializer(), config))
    }
    
    fun update(newConfig: GymConfigData) {
        config = newConfig
        save()
    }
}
