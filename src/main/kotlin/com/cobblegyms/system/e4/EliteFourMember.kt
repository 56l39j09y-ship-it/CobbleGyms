package com.cobblegyms.system.e4

import java.util.UUID

data class EliteFourMember(
    val uuid: UUID,
    val playerName: String,
    val position: Int,
    val typeSpecialty: String,
    var wins: Int = 0,
    var losses: Int = 0,
    var lastRewardDate: Long = 0L,
    var active: Boolean = true,
    val assignedAt: Long = System.currentTimeMillis()
) {
    val totalBattles: Int get() = wins + losses
    val winRate: Double get() = if (totalBattles > 0) wins.toDouble() / totalBattles * 100 else 0.0

    fun recordWin() { wins++ }
    fun recordLoss() { losses++ }
}
