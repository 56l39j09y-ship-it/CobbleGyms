package com.cobblegyms.system.rewards

import java.util.*

data class RewardConfig(
    val gymLeaderMinBattles: Int = 25,
    val gymLeaderMinWinrate: Double = 50.0,
    val e4WeeklyReward: Int = 5000,
    val championWeeklyReward: Int = 10000,
    val rewardCurrency: String = "coins"
)

data class RewardHistory(
    val playerUuid: UUID,
    val rewardAmount: Int,
    val rewardType: String, // GYM_LEADER, E4, CHAMPION
    val rewardDate: Long = System.currentTimeMillis()
)

object RewardManager {
    private var config = RewardConfig()
    private val rewardHistory = mutableListOf<RewardHistory>()
    private val lastRewardDate = mutableMapOf<UUID, Long>()

    fun initialize(rewardConfig: RewardConfig = RewardConfig()) {
        config = rewardConfig
        // Load reward history from database
    }

    fun canClaimGymLeaderReward(uuid: UUID, battles: Int, wins: Int): Boolean {
        val hasMinBattles = battles >= config.gymLeaderMinBattles
        val hasMinWinrate = if (battles > 0) {
            (wins.toDouble() / battles * 100) >= config.gymLeaderMinWinrate
        } else {
            false
        }

        val lastClaim = lastRewardDate[uuid] ?: 0L
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val canClaim = lastClaim < weekAgo

        return hasMinBattles && hasMinWinrate && canClaim
    }

    fun claimGymLeaderReward(uuid: UUID, playerName: String): Boolean {
        if (canClaimGymLeaderReward(uuid, config.gymLeaderMinBattles, 
            (config.gymLeaderMinBattles * config.gymLeaderMinWinrate / 100).toInt())) {
            
            val reward = RewardHistory(uuid, 0, "GYM_LEADER") // Amount set by server
            rewardHistory.add(reward)
            lastRewardDate[uuid] = System.currentTimeMillis()
            return true
        }
        return false
    }

    fun claimE4Reward(uuid: UUID): Boolean {
        val lastClaim = lastRewardDate[uuid] ?: 0L
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        
        if (lastClaim < weekAgo) {
            val reward = RewardHistory(uuid, config.e4WeeklyReward, "E4")
            rewardHistory.add(reward)
            lastRewardDate[uuid] = System.currentTimeMillis()
            return true
        }
        return false
    }

    fun claimChampionReward(uuid: UUID): Boolean {
        val lastClaim = lastRewardDate[uuid] ?: 0L
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        
        if (lastClaim < weekAgo) {
            val reward = RewardHistory(uuid, config.championWeeklyReward, "CHAMPION")
            rewardHistory.add(reward)
            lastRewardDate[uuid] = System.currentTimeMillis()
            return true
        }
        return false
    }

    fun getPlayerRewardHistory(uuid: UUID): List<RewardHistory> {
        return rewardHistory.filter { it.playerUuid == uuid }.toList()
    }

    fun getTotalRewards(uuid: UUID): Int {
        return rewardHistory.filter { it.playerUuid == uuid }
            .sumOf { it.rewardAmount }
    }

    fun getLastRewardDate(uuid: UUID): Long = lastRewardDate[uuid] ?: 0L

    fun updateConfig(newConfig: RewardConfig) {
        config = newConfig
    }

    fun getConfig(): RewardConfig = config

    fun clearOldHistory(daysOld: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        rewardHistory.removeIf { it.rewardDate < cutoffTime }
    }
}