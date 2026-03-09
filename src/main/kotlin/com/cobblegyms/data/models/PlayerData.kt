package com.cobblegyms.data.models

import java.util.UUID

data class PlayerBadge(
    val playerUuid: UUID,
    val playerName: String,
    val seasonId: Int,
    val gymType: PokemonType,
    val earnedAt: Long
)

data class PlayerE4Victory(
    val playerUuid: UUID,
    val playerName: String,
    val seasonId: Int,
    val e4Uuid: UUID,
    val e4Name: String,
    val earnedAt: Long
)

data class BattleRecord(
    val id: Int,
    val seasonId: Int,
    val battleType: String,
    val leaderUuid: UUID,
    val leaderName: String,
    val challengerUuid: UUID,
    val challengerName: String,
    val winner: String,
    val challengerTeam: String?,
    val leaderTeam: String?,
    val turns: Int,
    val battleTime: Long,
    val notes: String?
)

data class WeeklyStats(
    val playerUuid: UUID,
    val playerName: String,
    val role: String,
    val weekStart: Long,
    val battles: Int,
    val wins: Int,
    val losses: Int
) {
    val winrate: Double get() = if (battles > 0) wins.toDouble() / battles else 0.0
}

data class ChallengeCooldown(
    val challengerUuid: UUID,
    val targetUuid: UUID,
    val targetType: String,
    val lastChallenge: Long
)

data class GymBan(
    val gymLeaderUuid: UUID,
    val bannedPlayerUuid: UUID,
    val bannedPlayerName: String,
    val reason: String?,
    val banUntil: Long,
    val createdAt: Long
)

data class QueueEntry(
    val targetUuid: UUID,
    val targetType: String,
    val challengerUuid: UUID,
    val challengerName: String,
    val queuedAt: Long,
    val position: Int
)

data class ActiveBattle(
    val battleId: String,
    val battleType: String,
    val leaderUuid: UUID,
    val challengerUuid: UUID,
    val startTime: Long,
    val turns: Int,
    val status: String
)

data class SeasonData(
    val id: Int,
    val number: Int,
    val startTime: Long,
    val endTime: Long?,
    val active: Boolean
)
