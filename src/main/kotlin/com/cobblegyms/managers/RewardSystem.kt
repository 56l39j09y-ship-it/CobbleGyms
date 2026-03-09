package com.cobblegyms.managers

import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.utils.CooldownManager
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Distributes in-game currency and item rewards after battles.
 *
 * Cooldowns are enforced per player so rewards cannot be farmed
 * by repeating battles within a single day.
 *
 * NOTE: In-game economy integration (e.g. ImpactorAPI, CMI) should be
 *       wired in via the [grantCurrency] hook.  For now the reward amount
 *       is logged and sent as a chat notification.
 */
class RewardSystem(
    private val config: CobbleGymsConfig.RewardsConfig = CobbleGymsConfig.get().rewards
) {

    private val logger = LoggerFactory.getLogger("CobbleGyms/RewardSystem")

    enum class RewardType {
        GYM_BADGE,
        ELITE_FOUR_WIN,
        CHAMPION_DEFEAT
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Grant a reward to [player] for winning the specified [rewardType].
     * Returns true if the reward was given, false if on cooldown.
     */
    fun grantReward(player: ServerPlayerEntity, rewardType: RewardType): Boolean {
        val uuid = player.uuid

        if (!CooldownManager.isReady(uuid, config.cooldownHours)) {
            val remaining = CooldownManager.remainingMinutes(uuid)
            player.sendMessage(
                net.minecraft.text.Text.literal(
                    "§c✗ Reward cooldown active. Available in $remaining minute(s)."
                ), false
            )
            return false
        }

        val amount = when (rewardType) {
            RewardType.GYM_BADGE -> config.gymBadgeReward
            RewardType.ELITE_FOUR_WIN -> config.e4WinReward
            RewardType.CHAMPION_DEFEAT -> config.championReward
        }

        val label = when (rewardType) {
            RewardType.GYM_BADGE -> "Gym Badge"
            RewardType.ELITE_FOUR_WIN -> "Elite Four"
            RewardType.CHAMPION_DEFEAT -> "Champion"
        }

        grantCurrency(player, amount)
        CooldownManager.setCooldown(uuid, config.cooldownHours)

        player.sendMessage(
            net.minecraft.text.Text.literal("§6✦ $label Reward: §a+$amount coins! §7(Cooldown: ${config.cooldownHours}h)"),
            false
        )

        logger.info("Granted $amount coins to ${player.name.string} for $label victory")
        return true
    }

    // -------------------------------------------------------------------------
    // Economy hook
    // -------------------------------------------------------------------------

    /**
     * Hook this method into your economy plugin.
     *
     * Replace the log statement with the appropriate API call for your
     * server's economy system (e.g. ImpactorAPI, CMI, EssentialsX, etc.).
     */
    private fun grantCurrency(player: ServerPlayerEntity, amount: Int) {
        // TODO: Integrate with your economy plugin here
        // Example: EconomyAPI.addBalance(player.uuid, amount.toDouble())
        logger.info("[Economy] Would grant $amount coins to ${player.name.string}")
    }
}
