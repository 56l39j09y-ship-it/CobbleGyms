package com.cobblegyms.system.rewards

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import java.util.UUID

enum class RewardType { GYM_LEADER, E4, CHAMPION }

data class RewardHistory(
    val id: Int,
    val playerUuid: UUID,
    val playerName: String,
    val rewardAmount: Int,
    val rewardType: RewardType,
    val rewardDate: Long = System.currentTimeMillis()
)

class RewardManager {
    private val lastRewardDate = mutableMapOf<UUID, Long>()

    init {
        loadLastRewardDates()
    }

    private fun loadLastRewardDates() {
        val rows = DatabaseManager.executeQuery(
            "SELECT player_uuid, MAX(reward_date) as last_date FROM reward_history GROUP BY player_uuid"
        )
        for (row in rows) {
            val uuid = UUID.fromString(row["player_uuid"] as String)
            lastRewardDate[uuid] = (row["last_date"] as Number).toLong()
        }
    }

    private fun isOnCooldown(uuid: UUID): Boolean {
        val lastClaim = lastRewardDate[uuid] ?: 0L
        return System.currentTimeMillis() - lastClaim < CobbleGymsConfig.rewardCooldownMs
    }

    fun canClaimGymLeaderReward(uuid: UUID, totalBattles: Int, wins: Int): Boolean {
        val hasMinBattles = totalBattles >= CobbleGymsConfig.gymLeaderMinBattles
        val hasMinWinrate = if (totalBattles > 0) {
            (wins.toDouble() / totalBattles * 100) >= CobbleGymsConfig.gymLeaderMinWinrate
        } else false
        return hasMinBattles && hasMinWinrate && !isOnCooldown(uuid)
    }

    fun claimGymLeaderReward(uuid: UUID, playerName: String, totalBattles: Int, wins: Int): Boolean {
        if (!canClaimGymLeaderReward(uuid, totalBattles, wins)) return false
        return recordReward(uuid, playerName, CobbleGymsConfig.gymLeaderWeeklyReward, RewardType.GYM_LEADER)
    }

    fun canClaimE4Reward(uuid: UUID): Boolean = !isOnCooldown(uuid)

    fun claimE4Reward(uuid: UUID, playerName: String): Boolean {
        if (!canClaimE4Reward(uuid)) return false
        return recordReward(uuid, playerName, CobbleGymsConfig.e4WeeklyReward, RewardType.E4)
    }

    fun canClaimChampionReward(uuid: UUID): Boolean = !isOnCooldown(uuid)

    fun claimChampionReward(uuid: UUID, playerName: String): Boolean {
        if (!canClaimChampionReward(uuid)) return false
        return recordReward(uuid, playerName, CobbleGymsConfig.championWeeklyReward, RewardType.CHAMPION)
    }

    private fun recordReward(uuid: UUID, playerName: String, amount: Int, type: RewardType): Boolean {
        val now = System.currentTimeMillis()
        DatabaseManager.executeUpdate(
            "INSERT INTO reward_history (player_uuid, player_name, reward_amount, reward_type, reward_date) VALUES (?, ?, ?, ?, ?)",
            uuid.toString(), playerName, amount, type.name, now
        )
        lastRewardDate[uuid] = now
        CobbleGymsMod.LOGGER.info("Reward claimed: $playerName received $amount ${CobbleGymsConfig.rewardCurrency} ($type)")
        return true
    }

    fun getPlayerRewardHistory(uuid: UUID): List<RewardHistory> {
        val rows = DatabaseManager.executeQuery(
            "SELECT id, player_uuid, player_name, reward_amount, reward_type, reward_date FROM reward_history WHERE player_uuid = ? ORDER BY reward_date DESC LIMIT 20",
            uuid.toString()
        )
        return rows.map { row ->
            RewardHistory(
                id = (row["id"] as Number).toInt(),
                playerUuid = UUID.fromString(row["player_uuid"] as String),
                playerName = row["player_name"] as String,
                rewardAmount = (row["reward_amount"] as Number).toInt(),
                rewardType = RewardType.valueOf(row["reward_type"] as String),
                rewardDate = (row["reward_date"] as Number).toLong()
            )
        }
    }

    fun getLastRewardDate(uuid: UUID): Long = lastRewardDate[uuid] ?: 0L

    fun getCooldownRemainingMs(uuid: UUID): Long {
        val lastClaim = lastRewardDate[uuid] ?: 0L
        val remaining = CobbleGymsConfig.rewardCooldownMs - (System.currentTimeMillis() - lastClaim)
        return if (remaining > 0) remaining else 0L
    }
}
