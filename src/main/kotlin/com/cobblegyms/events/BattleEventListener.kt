package com.cobblegyms.events

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.system.battle.BattleQueueManager
import com.cobblegyms.system.champion.ChampionManager
import com.cobblegyms.system.e4.EliteFourManager
import com.cobblegyms.system.gym.GymLeaderManager
import java.util.UUID

object BattleEventListener {

    private lateinit var gymLeaderManager: GymLeaderManager
    private lateinit var eliteFourManager: EliteFourManager
    private lateinit var championManager: ChampionManager

    fun register(
        gymLeaderManager: GymLeaderManager,
        eliteFourManager: EliteFourManager,
        championManager: ChampionManager
    ) {
        this.gymLeaderManager = gymLeaderManager
        this.eliteFourManager = eliteFourManager
        this.championManager = championManager
        CobbleGymsMod.LOGGER.info("Battle event listeners registered.")
    }

    fun onBattleStart(challengerUuid: UUID, targetUuid: UUID, targetType: String) {
        val entry = BattleQueueManager.peekNext(targetUuid, targetType) ?: return
        if (entry.playerUuid == challengerUuid) {
            BattleQueueManager.popNext(targetUuid, targetType)
            BattleQueueManager.startBattle(entry)
            CobbleGymsMod.LOGGER.info("Battle started: ${entry.playerName} vs $targetType")
        }
    }

    fun onBattleEnd(
        challengerUuid: UUID,
        targetUuid: UUID,
        targetType: String,
        challengerWon: Boolean
    ) {
        BattleQueueManager.endBattle(challengerUuid, targetUuid)

        when (targetType.uppercase()) {
            "GYM_LEADER" -> {
                if (challengerWon) {
                    gymLeaderManager.recordLoss(targetUuid)
                } else {
                    gymLeaderManager.recordWin(targetUuid)
                }
            }
            "ELITE_FOUR" -> {
                if (challengerWon) {
                    eliteFourManager.recordLoss(targetUuid)
                } else {
                    eliteFourManager.recordWin(targetUuid)
                }
            }
            "CHAMPION" -> {
                if (challengerWon) {
                    championManager.recordLoss(targetUuid)
                    CobbleGymsMod.LOGGER.info("Champion defeated! New champion challenge incoming from $challengerUuid")
                } else {
                    championManager.recordWin(targetUuid)
                }
            }
        }
        CobbleGymsMod.LOGGER.info("Battle ended: challenger=$challengerUuid won=$challengerWon type=$targetType")
    }

    fun onTurnCount(battleId: String, turn: Int) {
        CobbleGymsMod.LOGGER.debug("Battle $battleId turn $turn")
    }

    fun onMoveExecuted(battleId: String, move: String, user: UUID) {
        CobbleGymsMod.LOGGER.debug("Battle $battleId: $user used $move")
    }
}
