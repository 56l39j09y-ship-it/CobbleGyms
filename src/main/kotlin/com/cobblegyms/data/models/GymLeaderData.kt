package com.cobblegyms.data.models

import java.util.UUID

data class GymLeaderData(
    val id: Int,
    val playerUuid: UUID,
    val playerName: String,
    val gymType: PokemonType,
    val battleFormat: BattleFormat,
    val isOpen: Boolean,
    val gymLocation: GymLocation?,
    val extraBannedPokemon: String?,
    val extraBanSeason: Int,
    val multiTeamEnabled: Boolean,
    val team1: String?,
    val team2: String?,
    val team3: String?,
    val currentTeamSlot: Int
)

data class EliteFourData(
    val id: Int,
    val playerUuid: UUID,
    val playerName: String,
    val type1: PokemonType,
    val type2: PokemonType,
    val battleFormat: BattleFormat,
    val isOpen: Boolean,
    val arenaLocation: GymLocation?,
    val multiTeamEnabled: Boolean,
    val team1: String?,
    val team2: String?,
    val team3: String?,
    val currentTeamSlot: Int
)

data class ChampionData(
    val id: Int,
    val playerUuid: UUID,
    val playerName: String,
    val battleFormat: BattleFormat,
    val isOpen: Boolean,
    val arenaLocation: GymLocation?,
    val multiTeamEnabled: Boolean,
    val team1: String?,
    val team2: String?,
    val team3: String?,
    val currentTeamSlot: Int
)

data class GymLocation(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    companion object
}

enum class BattleFormat {
    SINGLES, DOUBLES
}

enum class PokemonType(val displayName: String, val color: String) {
    NORMAL("Normal", "§7"),
    FIRE("Fire", "§c"),
    WATER("Water", "§9"),
    ELECTRIC("Electric", "§e"),
    GRASS("Grass", "§a"),
    ICE("Ice", "§b"),
    FIGHTING("Fighting", "§4"),
    POISON("Poison", "§5"),
    GROUND("Ground", "§6"),
    FLYING("Flying", "§3"),
    PSYCHIC("Psychic", "§d"),
    BUG("Bug", "§2"),
    ROCK("Rock", "§8"),
    GHOST("Ghost", "§5"),
    DRAGON("Dragon", "§1"),
    DARK("Dark", "§8"),
    STEEL("Steel", "§7"),
    FAIRY("Fairy", "§d");
    
    companion object {
        fun fromString(name: String): PokemonType? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }
    }
}
