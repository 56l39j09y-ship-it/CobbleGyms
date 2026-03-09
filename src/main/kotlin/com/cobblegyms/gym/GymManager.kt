package com.cobblegyms.gym

import com.cobblegyms.CobbleGyms
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.UUID

class GymManager {
    
    // Cache of currently equipped teams (playerUuid -> original team snapshot)
    private val equippedLeaders = mutableSetOf<UUID>()
    private val originalTeams = mutableMapOf<UUID, String?>() // stored as pokepaste string
    
    fun onServerStart() {
        // Close all gyms on server start (leaders must manually open)
        GymRepository.getAllGymLeaders().forEach { leader ->
            if (leader.isOpen) {
                GymRepository.upsertGymLeader(leader.copy(isOpen = false))
            }
        }
        GymRepository.getAllEliteFour().forEach { e4 ->
            if (e4.isOpen) {
                GymRepository.upsertEliteFour(e4.copy(isOpen = false))
            }
        }
        val champ = GymRepository.getChampion()
        if (champ?.isOpen == true) {
            GymRepository.setChampion(champ.copy(isOpen = false))
        }
    }
    
    // ===== GYM LEADER MANAGEMENT =====
    
    fun assignGymLeader(player: ServerPlayerEntity, gymType: PokemonType): Boolean {
        val existing = GymRepository.getGymLeaderByType(gymType)
        if (existing != null && existing.playerUuid != player.uuid) {
            return false // type already taken
        }
        val leaderData = GymLeaderData(
            id = 0,
            playerUuid = player.uuid,
            playerName = player.name.string,
            gymType = gymType,
            battleFormat = BattleFormat.SINGLES,
            isOpen = false,
            gymLocation = null,
            extraBannedPokemon = null,
            extraBanSeason = 0,
            multiTeamEnabled = false,
            team1 = null,
            team2 = null,
            team3 = null,
            currentTeamSlot = 1
        )
        GymRepository.upsertGymLeader(leaderData)
        return true
    }
    
    fun removeGymLeader(uuid: UUID) {
        GymRepository.deleteGymLeader(uuid)
    }
    
    fun getGymLeader(uuid: UUID): GymLeaderData? = GymRepository.getGymLeader(uuid)
    
    fun getAllGymLeaders(): List<GymLeaderData> = GymRepository.getAllGymLeaders()
    
    fun isGymLeader(uuid: UUID): Boolean = GymRepository.getGymLeader(uuid) != null
    
    fun openGym(uuid: UUID): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        GymRepository.upsertGymLeader(leader.copy(isOpen = true))
        return true
    }
    
    fun closeGym(uuid: UUID): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        GymRepository.upsertGymLeader(leader.copy(isOpen = false))
        return true
    }
    
    fun setGymFormat(uuid: UUID, format: BattleFormat): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        GymRepository.upsertGymLeader(leader.copy(battleFormat = format))
        return true
    }
    
    fun setGymLocation(uuid: UUID, location: GymLocation): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        GymRepository.upsertGymLeader(leader.copy(gymLocation = location))
        return true
    }
    
    fun setGymTeam(uuid: UUID, slot: Int, pokepaste: String): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        val updated = when (slot) {
            1 -> leader.copy(team1 = pokepaste)
            2 -> leader.copy(team2 = pokepaste)
            3 -> leader.copy(team3 = pokepaste)
            else -> return false
        }
        GymRepository.upsertGymLeader(updated)
        return true
    }
    
    fun setMultiTeamEnabled(uuid: UUID, enabled: Boolean): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        GymRepository.upsertGymLeader(leader.copy(multiTeamEnabled = enabled))
        return true
    }
    
    fun setExtraBan(uuid: UUID, pokemon: String, seasonId: Int): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        if (leader.extraBanSeason == seasonId && leader.extraBannedPokemon != null) {
            return false // already used ban this season
        }
        GymRepository.upsertGymLeader(leader.copy(extraBannedPokemon = pokemon, extraBanSeason = seasonId))
        return true
    }
    
    fun getActiveTeam(uuid: UUID): String? {
        val leader = GymRepository.getGymLeader(uuid) ?: return null
        return when (leader.currentTeamSlot) {
            1 -> leader.team1
            2 -> leader.team2
            3 -> leader.team3
            else -> leader.team1
        }
    }
    
    fun selectTeamSlot(uuid: UUID, slot: Int): Boolean {
        val leader = GymRepository.getGymLeader(uuid) ?: return false
        if (!leader.multiTeamEnabled && slot != 1) return false
        GymRepository.upsertGymLeader(leader.copy(currentTeamSlot = slot))
        return true
    }
    
    // ===== ELITE FOUR MANAGEMENT =====
    
    fun assignEliteFour(player: ServerPlayerEntity, type1: PokemonType, type2: PokemonType): Boolean {
        val e4Data = EliteFourData(
            id = 0,
            playerUuid = player.uuid,
            playerName = player.name.string,
            type1 = type1,
            type2 = type2,
            battleFormat = BattleFormat.SINGLES,
            isOpen = false,
            arenaLocation = null,
            multiTeamEnabled = false,
            team1 = null,
            team2 = null,
            team3 = null,
            currentTeamSlot = 1
        )
        GymRepository.upsertEliteFour(e4Data)
        return true
    }
    
    fun removeEliteFour(uuid: UUID) {
        GymRepository.deleteEliteFour(uuid)
    }
    
    fun getEliteFour(uuid: UUID): EliteFourData? = GymRepository.getEliteFour(uuid)
    
    fun getAllEliteFour(): List<EliteFourData> = GymRepository.getAllEliteFour()
    
    fun isEliteFour(uuid: UUID): Boolean = GymRepository.getEliteFour(uuid) != null
    
    fun openE4(uuid: UUID): Boolean {
        val e4 = GymRepository.getEliteFour(uuid) ?: return false
        GymRepository.upsertEliteFour(e4.copy(isOpen = true))
        return true
    }
    
    fun closeE4(uuid: UUID): Boolean {
        val e4 = GymRepository.getEliteFour(uuid) ?: return false
        GymRepository.upsertEliteFour(e4.copy(isOpen = false))
        return true
    }
    
    fun setE4Format(uuid: UUID, format: BattleFormat): Boolean {
        val e4 = GymRepository.getEliteFour(uuid) ?: return false
        GymRepository.upsertEliteFour(e4.copy(battleFormat = format))
        return true
    }
    
    fun setE4Team(uuid: UUID, slot: Int, pokepaste: String): Boolean {
        val e4 = GymRepository.getEliteFour(uuid) ?: return false
        val updated = when (slot) {
            1 -> e4.copy(team1 = pokepaste)
            2 -> e4.copy(team2 = pokepaste)
            3 -> e4.copy(team3 = pokepaste)
            else -> return false
        }
        GymRepository.upsertEliteFour(updated)
        return true
    }
    
    fun getActiveE4Team(uuid: UUID): String? {
        val e4 = GymRepository.getEliteFour(uuid) ?: return null
        return when (e4.currentTeamSlot) {
            1 -> e4.team1
            2 -> e4.team2
            3 -> e4.team3
            else -> e4.team1
        }
    }
    
    // ===== CHAMPION MANAGEMENT =====
    
    fun assignChampion(player: ServerPlayerEntity): Boolean {
        val champData = ChampionData(
            id = 0,
            playerUuid = player.uuid,
            playerName = player.name.string,
            battleFormat = BattleFormat.SINGLES,
            isOpen = false,
            arenaLocation = null,
            multiTeamEnabled = false,
            team1 = null,
            team2 = null,
            team3 = null,
            currentTeamSlot = 1
        )
        GymRepository.setChampion(champData)
        return true
    }
    
    fun removeChampion() {
        GymRepository.removeChampion()
    }
    
    fun getChampion(): ChampionData? = GymRepository.getChampion()
    
    fun isChampion(uuid: UUID): Boolean = GymRepository.getChampion()?.playerUuid == uuid
    
    fun openChampion(uuid: UUID): Boolean {
        val champ = GymRepository.getChampion() ?: return false
        if (champ.playerUuid != uuid) return false
        GymRepository.setChampion(champ.copy(isOpen = true))
        return true
    }
    
    fun closeChampion(uuid: UUID): Boolean {
        val champ = GymRepository.getChampion() ?: return false
        if (champ.playerUuid != uuid) return false
        GymRepository.setChampion(champ.copy(isOpen = false))
        return true
    }
    
    fun setChampionFormat(uuid: UUID, format: BattleFormat): Boolean {
        val champ = GymRepository.getChampion() ?: return false
        if (champ.playerUuid != uuid) return false
        GymRepository.setChampion(champ.copy(battleFormat = format))
        return true
    }
    
    fun setChampionTeam(uuid: UUID, slot: Int, pokepaste: String): Boolean {
        val champ = GymRepository.getChampion() ?: return false
        if (champ.playerUuid != uuid) return false
        val updated = when (slot) {
            1 -> champ.copy(team1 = pokepaste)
            2 -> champ.copy(team2 = pokepaste)
            3 -> champ.copy(team3 = pokepaste)
            else -> return false
        }
        GymRepository.setChampion(updated)
        return true
    }
    
    fun getActiveChampionTeam(uuid: UUID): String? {
        val champ = GymRepository.getChampion() ?: return null
        return when (champ.currentTeamSlot) {
            1 -> champ.team1
            2 -> champ.team2
            3 -> champ.team3
            else -> champ.team1
        }
    }
    
    // ===== PLAYER ROLE DETECTION =====
    
    fun getPlayerRole(uuid: UUID): PlayerRole {
        return when {
            isChampion(uuid) -> PlayerRole.CHAMPION
            isEliteFour(uuid) -> PlayerRole.ELITE_FOUR
            isGymLeader(uuid) -> PlayerRole.GYM_LEADER
            else -> PlayerRole.CHALLENGER
        }
    }
    
    // ===== BADGE SYSTEM =====
    
    fun getPlayerBadges(playerUuid: UUID, seasonId: Int) = GymRepository.getPlayerBadges(playerUuid, seasonId)
    
    fun hasAllBadges(playerUuid: UUID, seasonId: Int) = GymRepository.hasAllBadges(playerUuid, seasonId)
    
    fun hasBeatenAllE4(playerUuid: UUID, seasonId: Int) = GymRepository.hasBeatenAllE4(playerUuid, seasonId)
    
    // ===== GYM BAN SYSTEM =====
    
    fun banPlayer(gymLeaderUuid: UUID, playerUuid: UUID, playerName: String, reason: String?, durationHours: Int) {
        val ban = com.cobblegyms.data.models.GymBan(
            gymLeaderUuid = gymLeaderUuid,
            bannedPlayerUuid = playerUuid,
            bannedPlayerName = playerName,
            reason = reason,
            banUntil = System.currentTimeMillis() / 1000 + durationHours * 3600L,
            createdAt = System.currentTimeMillis() / 1000
        )
        GymRepository.addGymBan(ban)
    }
    
    fun unbanPlayer(gymLeaderUuid: UUID, playerUuid: UUID) {
        GymRepository.removeGymBan(gymLeaderUuid, playerUuid)
    }
    
    fun isPlayerBannedFromGym(gymLeaderUuid: UUID, playerUuid: UUID): Boolean {
        return GymRepository.isPlayerBanned(gymLeaderUuid, playerUuid)
    }
    
    enum class PlayerRole {
        CHALLENGER, GYM_LEADER, ELITE_FOUR, CHAMPION
    }
}
