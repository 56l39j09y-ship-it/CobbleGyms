package com.cobblegyms.system.validation

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.system.battle.BattleQueueManager
import java.util.UUID

object BattleValidator {

    fun canChallenge(challengerUuid: UUID, targetUuid: UUID, targetType: String): ValidationResult {
        val errors = mutableListOf<String>()

        if (CobbleGymsMod.championManager.isBanned(challengerUuid)) {
            val ban = CobbleGymsMod.championManager.getBan(challengerUuid)
            errors.add("You are banned from challenging. Reason: ${ban?.reason ?: "unknown"}")
            return ValidationResult(false, errors)
        }

        if (BattleQueueManager.isInActiveBattle(challengerUuid)) {
            errors.add("You are already in an active battle.")
        }

        if (challengerUuid == targetUuid) {
            errors.add("You cannot challenge yourself.")
        }

        when (targetType.uppercase()) {
            "GYM_LEADER" -> {
                if (!CobbleGymsMod.gymLeaderManager.isGymLeader(targetUuid)) {
                    errors.add("That player is not a Gym Leader.")
                }
            }
            "ELITE_FOUR" -> {
                if (!CobbleGymsMod.eliteFourManager.isMember(targetUuid)) {
                    errors.add("That player is not an Elite Four member.")
                }
            }
            "CHAMPION" -> {
                if (!CobbleGymsMod.championManager.isChampion(targetUuid)) {
                    errors.add("That player is not the Champion.")
                }
            }
            else -> errors.add("Unknown target type: $targetType")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
