package com.cobblegyms.rewards

import com.cobblegyms.CobbleGyms
import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.GymRepository
import com.cobblegyms.util.TimeUtil
import net.minecraft.server.network.ServerPlayerEntity
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class RewardManager {
    private var rewardTimer: Timer? = null
    
    fun onServerStart() {
        scheduleWeeklyRewards()
    }
    
    private fun scheduleWeeklyRewards() {
        rewardTimer?.cancel()
        rewardTimer = Timer("RewardTimer", true)
        
        // Schedule weekly reward check every hour
        rewardTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    checkAndDistributeWeeklyRewards()
                } catch (e: Exception) {
                    CobbleGyms.LOGGER.error("Error in reward check: ${e.message}")
                }
            }
        }, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1))
    }
    
    private fun checkAndDistributeWeeklyRewards() {
        val now = System.currentTimeMillis() / 1000
        val weekStart = TimeUtil.getWeekStartSeconds()
        
        // Only process once the current week has started (i.e., the previous week has ended)
        if (now < weekStart) return
        
        val previousWeekStart = weekStart - 7 * 24 * 3600L
        
        val config = GymConfig.config.rewards
        
        // Process Gym Leader rewards
        GymRepository.getAllGymLeaders().forEach { leader ->
            val stats = GymRepository.getWeeklyStats(leader.playerUuid, previousWeekStart)
            if (stats != null) {
                val meetsRequirements = stats.battles >= config.leaderMinWeeklyBattles &&
                        stats.winrate >= config.leaderMinWinrate
                
                if (meetsRequirements) {
                    giveReward(leader.playerUuid, leader.playerName, config.leaderRewardCommand, "GymLeader")
                }
            }
        }
        
        // Process E4 rewards (always get rewards)
        GymRepository.getAllEliteFour().forEach { e4 ->
            giveReward(e4.playerUuid, e4.playerName, config.eliteFourRewardCommand, "EliteFour")
        }
        
        // Process Champion reward (always gets reward)
        GymRepository.getChampion()?.let { champ ->
            giveReward(champ.playerUuid, champ.playerName, config.championRewardCommand, "Champion")
        }
    }
    
    fun processSeasonEndRewards(seasonId: Int) {
        // Season end rewards can be configured differently
        CobbleGyms.LOGGER.info("Processing season $seasonId end rewards")
        // Implement season-specific rewards here
    }
    
    private fun giveReward(playerUuid: java.util.UUID, playerName: String, command: String, role: String) {
        try {
            val player = CobbleGyms.server.playerManager.getPlayer(playerUuid)
            val actualCommand = command.replace("{player}", playerName)
            
            CobbleGyms.server.commandManager.executeWithPrefix(
                CobbleGyms.server.commandSource,
                actualCommand
            )
            
            player?.sendMessage(
                net.minecraft.text.Text.literal("§a§l★ Weekly Reward! §7You received your $role weekly reward!")
            )
            
            CobbleGyms.LOGGER.info("Reward given to $playerName ($role): $actualCommand")
        } catch (e: Exception) {
            CobbleGyms.LOGGER.error("Failed to give reward to $playerName: ${e.message}")
        }
    }
    
}
