package com.cobblegyms.battle

import com.cobblegyms.CobbleGyms
import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.*
import com.cobblegyms.util.TimeUtil
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BattleManager {
    
    // In-memory tracking of active battles (battleId -> BattleState)
    private val activeBattles = ConcurrentHashMap<String, BattleState>()
    
    // Equipment tracking: leaderUuid -> original team pokepaste
    private val equippedTeams = ConcurrentHashMap<UUID, String?>()
    
    data class BattleState(
        val battleId: String,
        val battleType: BattleType,
        val leaderUuid: UUID,
        val challengerUuid: UUID,
        var turns: Int = 0,
        val startTime: Long = System.currentTimeMillis(),
        var status: BattleStatus = BattleStatus.ACTIVE
    )
    
    enum class BattleType { GYM, ELITE_FOUR, CHAMPION }
    enum class BattleStatus { ACTIVE, ENDED, REDO }
    
    fun onServerStop() {
        // Restore all equipped teams before shutdown
        equippedTeams.keys.toList().forEach { uuid ->
            restoreTeam(uuid)
        }
    }
    
    // ===== QUEUE MANAGEMENT =====
    
    fun joinQueue(targetUuid: UUID, targetType: String, challenger: ServerPlayerEntity): QueueJoinResult {
        val config = GymConfig.config
        
        // Check if already in queue
        if (GymRepository.isInQueue(targetUuid, challenger.uuid)) {
            return QueueJoinResult.ALREADY_IN_QUEUE
        }
        
        // Check cooldown
        val cooldown = GymRepository.getChallengeCooldown(challenger.uuid, targetUuid, targetType)
        if (cooldown != null) {
            val cooldownSeconds = config.battle.challengeCooldownHours * 3600L
            val elapsed = System.currentTimeMillis() / 1000 - cooldown
            if (elapsed < cooldownSeconds) {
                val remaining = cooldownSeconds - elapsed
                return QueueJoinResult.ON_COOLDOWN(remaining)
            }
        }
        
        // Check gym ban
        if (GymRepository.isPlayerBanned(targetUuid, challenger.uuid)) {
            return QueueJoinResult.BANNED
        }
        
        val queue = GymRepository.getQueue(targetUuid)
        val position = queue.size + 1
        
        // Check max queue size
        if (position > config.battle.maxQueueSize) {
            return QueueJoinResult.QUEUE_FULL
        }
        
        val entry = QueueEntry(
            targetUuid = targetUuid,
            targetType = targetType,
            challengerUuid = challenger.uuid,
            challengerName = challenger.name.string,
            queuedAt = System.currentTimeMillis() / 1000,
            position = position
        )
        GymRepository.addToQueue(entry)
        
        return QueueJoinResult.SUCCESS(position)
    }
    
    fun leaveQueue(targetUuid: UUID, challengerUuid: UUID) {
        GymRepository.removeFromQueue(targetUuid, challengerUuid)
    }
    
    fun getQueue(targetUuid: UUID): List<QueueEntry> = GymRepository.getQueue(targetUuid)
    
    fun getNextInQueue(targetUuid: UUID): QueueEntry? = GymRepository.getQueue(targetUuid).firstOrNull()
    
    // ===== BATTLE INITIATION =====
    
    fun startBattle(
        leader: ServerPlayerEntity,
        challenger: ServerPlayerEntity,
        battleType: BattleType
    ): BattleStartResult {
        // Check if leader already in battle
        if (getActiveBattleForLeader(leader.uuid) != null) {
            return BattleStartResult.LEADER_BUSY
        }
        
        val battleId = UUID.randomUUID().toString()
        val state = BattleState(
            battleId = battleId,
            battleType = battleType,
            leaderUuid = leader.uuid,
            challengerUuid = challenger.uuid
        )
        
        activeBattles[battleId] = state
        GymRepository.saveActiveBattle(ActiveBattle(
            battleId = battleId,
            battleType = battleType.name,
            leaderUuid = leader.uuid,
            challengerUuid = challenger.uuid,
            startTime = state.startTime,
            turns = 0,
            status = "ACTIVE"
        ))
        
        // Remove challenger from queue
        GymRepository.removeFromQueue(leader.uuid, challenger.uuid)
        
        // Teleport players to arena
        teleportToBattleArena(leader, challenger, battleType)
        
        // Notify players
        leader.sendMessage(
            Text.literal("§a§lBATTLE STARTING! §7vs §e${challenger.name.string}").formatted(Formatting.WHITE)
        )
        challenger.sendMessage(
            Text.literal("§a§lBATTLE STARTING! §7vs §e${leader.name.string}").formatted(Formatting.WHITE)
        )
        
        return BattleStartResult.SUCCESS(battleId)
    }
    
    fun endBattle(
        battleId: String,
        winnerUuid: UUID,
        turns: Int,
        challengerTeam: String?,
        leaderTeam: String?
    ) {
        val state = activeBattles[battleId] ?: return
        state.status = BattleStatus.ENDED
        state.turns = turns
        
        val leaderUuid = state.leaderUuid
        val challengerUuid = state.challengerUuid
        val leaderWon = winnerUuid == leaderUuid
        
        GymRepository.endActiveBattle(battleId)
        activeBattles.remove(battleId)
        
        // Get current season
        val season = GymRepository.getCurrentSeason()
        if (season != null) {
            val weekStart = TimeUtil.getWeekStartSeconds()
            
            // Save battle record
            val leaderName = CobbleGyms.server.playerManager.getPlayer(leaderUuid)?.name?.string ?: "Unknown"
            val challengerName = CobbleGyms.server.playerManager.getPlayer(challengerUuid)?.name?.string ?: "Unknown"
            
            val record = BattleRecord(
                id = 0,
                seasonId = season.id,
                battleType = state.battleType.name,
                leaderUuid = leaderUuid,
                leaderName = leaderName,
                challengerUuid = challengerUuid,
                challengerName = challengerName,
                winner = if (leaderWon) "LEADER" else "CHALLENGER",
                challengerTeam = challengerTeam,
                leaderTeam = leaderTeam,
                turns = turns,
                battleTime = System.currentTimeMillis() / 1000,
                notes = null
            )
            GymRepository.saveBattleRecord(record)
            
            // Update weekly stats
            GymRepository.incrementWeeklyStats(leaderUuid, leaderName, state.battleType.name, weekStart, leaderWon)
            
            // Award badges/victories if challenger won
            if (!leaderWon) {
                when (state.battleType) {
                    BattleType.GYM -> {
                        val gymLeader = GymRepository.getGymLeader(leaderUuid)
                        if (gymLeader != null) {
                            GymRepository.awardBadge(challengerUuid, challengerName, season.id, gymLeader.gymType)
                            // Set cooldown for next challenge (but they can re-challenge after winning too)
                        }
                    }
                    BattleType.ELITE_FOUR -> {
                        GymRepository.awardE4Victory(challengerUuid, challengerName, season.id, leaderUuid, leaderName)
                    }
                    BattleType.CHAMPION -> {
                        // Challenger becomes new champion!
                        val challenger = CobbleGyms.server.playerManager.getPlayer(challengerUuid)
                        if (challenger != null) {
                            CobbleGyms.gymManager.assignChampion(challenger)
                        }
                        // Old champion loses rank
                        announceNewChampion(challengerName)
                    }
                }
            } else {
                // Leader won - set cooldown for challenger
                GymRepository.setChallengeCooldown(
                    challengerUuid, leaderUuid, state.battleType.name,
                    System.currentTimeMillis() / 1000
                )
            }
            
            // Notify players of result
            val leaderPlayer = CobbleGyms.server.playerManager.getPlayer(leaderUuid)
            val challengerPlayer = CobbleGyms.server.playerManager.getPlayer(challengerUuid)
            
            if (leaderWon) {
                leaderPlayer?.sendMessage(Text.literal("§a§lVICTORY! §7You defeated §e$challengerName§7!"))
                challengerPlayer?.sendMessage(Text.literal("§c§lDEFEAT! §7You lost to §e$leaderName§7. Try again in 24 hours."))
            } else {
                leaderPlayer?.sendMessage(Text.literal("§c§lDEFEAT! §7You lost to §e$challengerName§7!"))
                challengerPlayer?.sendMessage(Text.literal("§a§lVICTORY! §7You defeated §e$leaderName§7!"))
            }
        }
        
        // Restore leader's team
        restoreTeam(leaderUuid)
        
        // Process next in queue
        processNextInQueue(leaderUuid)
    }
    
    fun redoBattle(battleId: String, adminUuid: UUID): RedoResult {
        val state = activeBattles[battleId] ?: run {
            // Try from DB
            val dbBattle = GymRepository.getActiveBattle(adminUuid)
            if (dbBattle != null && dbBattle.turns <= 1) {
                GymRepository.endActiveBattle(dbBattle.battleId)
                return RedoResult.SUCCESS
            }
            return RedoResult.NOT_FOUND
        }
        
        if (state.turns > 1) {
            return RedoResult.TOO_MANY_TURNS(state.turns)
        }
        
        state.status = BattleStatus.REDO
        activeBattles.remove(battleId)
        GymRepository.endActiveBattle(battleId)
        
        // Reset cooldown so challenger can re-challenge immediately
        GymRepository.setChallengeCooldown(
            state.challengerUuid, state.leaderUuid, state.battleType.name, 0L
        )
        
        return RedoResult.SUCCESS
    }
    
    fun updateBattleTurns(battleId: String, turns: Int) {
        activeBattles[battleId]?.turns = turns
        GymRepository.updateBattleTurns(battleId, turns)
    }
    
    fun getActiveBattleForLeader(leaderUuid: UUID): BattleState? {
        return activeBattles.values.find { it.leaderUuid == leaderUuid && it.status == BattleStatus.ACTIVE }
    }
    
    fun getActiveBattleForPlayer(playerUuid: UUID): BattleState? {
        return activeBattles.values.find {
            (it.leaderUuid == playerUuid || it.challengerUuid == playerUuid) && it.status == BattleStatus.ACTIVE
        }
    }
    
    fun cancelAllBattlesForLeader(leaderUuid: UUID) {
        val battles = activeBattles.values.filter { it.leaderUuid == leaderUuid }
        battles.forEach { state ->
            state.status = BattleStatus.ENDED
            GymRepository.endActiveBattle(state.battleId)
            
            // Notify challenger
            val challenger = CobbleGyms.server.playerManager.getPlayer(state.challengerUuid)
            challenger?.sendMessage(Text.literal("§c§lBATTLE CANCELLED §7by the Gym Leader. Your cooldown has been reset."))
            
            // Reset cooldown
            GymRepository.setChallengeCooldown(state.challengerUuid, leaderUuid, state.battleType.name, 0L)
        }
        
        // Clear queue
        GymRepository.clearQueue(leaderUuid)
        activeBattles.entries.removeIf { it.value.leaderUuid == leaderUuid }
        restoreTeam(leaderUuid)
    }
    
    // ===== TEAM EQUIPMENT =====
    
    fun equipTeam(leaderUuid: UUID, pokepaste: String) {
        // Store original team (in a real implementation, save from Cobblemon party)
        if (!equippedTeams.containsKey(leaderUuid)) {
            equippedTeams[leaderUuid] = null // placeholder for original team
        }
        // Apply team to player party (Cobblemon integration would go here)
        applyTeamToCobblemon(leaderUuid, pokepaste)
    }
    
    fun unequipTeam(leaderUuid: UUID) {
        restoreTeam(leaderUuid)
    }
    
    private fun restoreTeam(leaderUuid: UUID) {
        val original = equippedTeams.remove(leaderUuid)
        if (original != null) {
            applyTeamToCobblemon(leaderUuid, original)
        }
    }
    
    private fun applyTeamToCobblemon(playerUuid: UUID, pokepaste: String) {
        // This is where Cobblemon API would be called to set the player's party
        // In a full implementation: CobblemonAPI.getParty(playerUuid).setFromShowdownFormat(pokepaste)
        CobbleGyms.LOGGER.debug("Applying team to player $playerUuid (Cobblemon API integration)")
    }
    
    // ===== TELEPORTATION =====
    
    private fun teleportToBattleArena(leader: ServerPlayerEntity, challenger: ServerPlayerEntity, battleType: BattleType) {
        val location = when (battleType) {
            BattleType.GYM -> GymRepository.getGymLeader(leader.uuid)?.gymLocation
            BattleType.ELITE_FOUR -> GymRepository.getEliteFour(leader.uuid)?.arenaLocation
            BattleType.CHAMPION -> GymRepository.getChampion()?.arenaLocation
        }
        
        if (location != null) {
            val world = CobbleGyms.server.getWorld(net.minecraft.util.Identifier.of(location.world).let {
                net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, it)
            })
            if (world != null) {
                leader.teleport(world, location.x, location.y, location.z, location.yaw, location.pitch)
                challenger.teleport(world, location.x + 5, location.y, location.z, location.yaw, location.pitch)
            }
        }
    }
    
    private fun processNextInQueue(leaderUuid: UUID) {
        val next = getNextInQueue(leaderUuid)
        if (next != null) {
            val leader = CobbleGyms.server.playerManager.getPlayer(leaderUuid)
            val challenger = CobbleGyms.server.playerManager.getPlayer(next.challengerUuid)
            
            if (leader != null && challenger != null) {
                leader.sendMessage(Text.literal("§a§lNEXT CHALLENGER: §e${challenger.name.string} §7is waiting! Use /challenge to start."))
                challenger.sendMessage(Text.literal("§a§lYOU'RE NEXT! §7The Gym Leader is ready for you. Stand by..."))
            }
        }
    }
    
    private fun announceNewChampion(name: String) {
        CobbleGyms.server.playerManager.playerList.forEach { player ->
            player.sendMessage(Text.literal("§6§l★ NEW CHAMPION! §e$name §6has become the new Pokémon Champion! ★"))
        }
    }
    
    // ===== SEALED RESULT TYPES =====
    
    sealed class QueueJoinResult {
        object SUCCESS_NO_WAIT : QueueJoinResult()
        data class SUCCESS(val position: Int) : QueueJoinResult()
        object ALREADY_IN_QUEUE : QueueJoinResult()
        data class ON_COOLDOWN(val remainingSeconds: Long) : QueueJoinResult()
        object BANNED : QueueJoinResult()
        object QUEUE_FULL : QueueJoinResult()
        object GYM_CLOSED : QueueJoinResult()
        object MISSING_BADGES : QueueJoinResult()
        object MISSING_E4_VICTORIES : QueueJoinResult()
    }
    
    sealed class BattleStartResult {
        data class SUCCESS(val battleId: String) : BattleStartResult()
        object LEADER_BUSY : BattleStartResult()
        object CHALLENGER_NOT_FOUND : BattleStartResult()
        object INVALID_TEAM : BattleStartResult()
    }
    
    sealed class RedoResult {
        object SUCCESS : RedoResult()
        object NOT_FOUND : RedoResult()
        data class TOO_MANY_TURNS(val turns: Int) : RedoResult()
    }
}
