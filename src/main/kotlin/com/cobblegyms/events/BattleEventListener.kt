package com.cobblegyms.events

import com.cobblegyms.managers.BattleManager
import com.cobblegyms.managers.ChampionManager
import com.cobblegyms.managers.E4Manager
import com.cobblegyms.managers.GymManager
import com.cobblegyms.managers.RewardSystem
import com.cobblegyms.managers.SeasonManager
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.slf4j.LoggerFactory

/**
 * Registers Fabric event listeners for player connection / disconnection
 * and Cobblemon battle outcomes.
 *
 * NOTE: Cobblemon battle outcome hooks should be wired here once the
 *       Cobblemon API for battle events is stable. The placeholders below
 *       show the intended integration points.
 */
class BattleEventListener(
    private val gymManager: GymManager,
    private val e4Manager: E4Manager,
    private val championManager: ChampionManager,
    private val seasonManager: SeasonManager,
    private val battleManager: BattleManager,
    private val rewardSystem: RewardSystem
) {

    private val logger = LoggerFactory.getLogger("CobbleGyms/Events")

    fun register() {
        registerConnectionEvents()
        // registerBattleEvents() – wire Cobblemon API here
        logger.info("Event listeners registered")
    }

    // -------------------------------------------------------------------------
    // Connection events
    // -------------------------------------------------------------------------

    private fun registerConnectionEvents() {
        // Remove disconnected players from all queues to prevent ghost entries
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player
            battleManager.removeFromAllQueues(player.uuid)
            logger.debug("${player.name.string} disconnected – removed from all queues")
        }
    }

    // -------------------------------------------------------------------------
    // Battle outcome handler (Cobblemon integration point)
    // -------------------------------------------------------------------------

    /**
     * Call this method when a Cobblemon battle between a challenger and a
     * registered gym leader / E4 / champion concludes.
     *
     * @param challengerUuid UUID of the challenger
     * @param defenderUuid   UUID of the defender (gym leader / E4 / champion)
     * @param challengerWon  true if the challenger won the battle
     */
    fun onBattleEnd(
        challengerUuid: java.util.UUID,
        challengerName: String,
        defenderUuid: java.util.UUID,
        challengerWon: Boolean,
        server: net.minecraft.server.MinecraftServer
    ) {
        val activeSeason = seasonManager.getActiveSeason()

        // Determine battle type by checking which role the defender holds
        when {
            gymManager.isGymLeader(defenderUuid) -> handleGymBattleEnd(
                challengerUuid, challengerName, defenderUuid, challengerWon, server, activeSeason?.id
            )
            e4Manager.isEliteFour(defenderUuid) -> handleE4BattleEnd(
                challengerUuid, challengerName, defenderUuid, challengerWon, server, activeSeason?.id
            )
            championManager.isChampion(defenderUuid) -> handleChampionBattleEnd(
                challengerUuid, challengerName, defenderUuid, challengerWon, server, activeSeason?.id
            )
            else -> logger.warn(
                "onBattleEnd: defender $defenderUuid is not a gym leader, E4, or champion – ignoring"
            )
        }

        // Remove the challenger from the defender's queue regardless of outcome
        battleManager.removeFromQueue(challengerUuid, defenderUuid, reason = "battle_started")
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun handleGymBattleEnd(
        challengerUuid: java.util.UUID,
        challengerName: String,
        leaderUuid: java.util.UUID,
        challengerWon: Boolean,
        server: net.minecraft.server.MinecraftServer,
        seasonId: Int?
    ) {
        if (challengerWon) {
            gymManager.recordChallengerWin(leaderUuid)
            server.playerManager.getPlayer(challengerUuid)?.let { player ->
                rewardSystem.grantReward(player, RewardSystem.RewardType.GYM_BADGE)
            }
            seasonId?.let { seasonManager.recordGymBattle(it, challengerUuid, challengerName, won = true) }

            val leaderName = gymManager.findLeaderByUuid(leaderUuid)?.playerName ?: "the Gym Leader"
            com.cobblegyms.utils.MessageUtils.broadcast(
                server, "§e$challengerName §fdefeated §e$leaderName §fand earned a badge!"
            )
        } else {
            gymManager.recordLeaderWin(leaderUuid)
            seasonId?.let { seasonManager.recordGymBattle(it, challengerUuid, challengerName, won = false) }
        }
    }

    private fun handleE4BattleEnd(
        challengerUuid: java.util.UUID,
        challengerName: String,
        memberUuid: java.util.UUID,
        challengerWon: Boolean,
        server: net.minecraft.server.MinecraftServer,
        seasonId: Int?
    ) {
        if (challengerWon) {
            e4Manager.recordChallengerWin(memberUuid)
            server.playerManager.getPlayer(challengerUuid)?.let { player ->
                rewardSystem.grantReward(player, RewardSystem.RewardType.ELITE_FOUR_WIN)
            }
            seasonId?.let { seasonManager.recordE4Battle(it, challengerUuid, challengerName, won = true) }

            val memberName = e4Manager.findMemberByUuid(memberUuid)?.playerName ?: "an Elite Four member"
            com.cobblegyms.utils.MessageUtils.broadcast(
                server, "§5$challengerName §fdefeated §5$memberName §fof the Elite Four!"
            )
        } else {
            e4Manager.recordMemberWin(memberUuid)
            seasonId?.let { seasonManager.recordE4Battle(it, challengerUuid, challengerName, won = false) }
        }
    }

    private fun handleChampionBattleEnd(
        challengerUuid: java.util.UUID,
        challengerName: String,
        championUuid: java.util.UUID,
        challengerWon: Boolean,
        server: net.minecraft.server.MinecraftServer,
        seasonId: Int?
    ) {
        if (challengerWon) {
            // Crown the new champion
            championManager.setChampion(challengerUuid, challengerName)
            server.playerManager.getPlayer(challengerUuid)?.let { player ->
                rewardSystem.grantReward(player, RewardSystem.RewardType.CHAMPION_DEFEAT)
            }
            seasonId?.let { seasonManager.recordChampionBattle(it, challengerUuid, challengerName, won = true) }

            com.cobblegyms.utils.MessageUtils.broadcast(
                server, "§6§l★ NEW CHAMPION ★ §r§e$challengerName §6has defeated the Champion!"
            )
        } else {
            championManager.recordDefense(championUuid)
            seasonId?.let { seasonManager.recordChampionBattle(it, challengerUuid, challengerName, won = false) }

            val champName = championManager.getCurrentChampion()?.playerName ?: "the Champion"
            com.cobblegyms.utils.MessageUtils.broadcast(
                server, "§6$champName §fsuccessfully defended the Champion title against §e$challengerName§f!"
            )
        }
    }
}
