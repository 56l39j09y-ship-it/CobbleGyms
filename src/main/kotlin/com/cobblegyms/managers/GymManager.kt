package com.cobblegyms.managers

import com.cobblegyms.database.DatabaseManager
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Manages Gym Leaders: registration, removal, wins/losses, and queries.
 */
class GymManager {

    private val logger = LoggerFactory.getLogger("CobbleGyms/GymManager")

    data class GymLeader(
        val id: Int,
        val playerUuid: UUID,
        val playerName: String,
        val gymType: String,
        val badgeName: String,
        val wins: Int,
        val losses: Int,
        val isActive: Boolean
    )

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Register or update a gym leader.
     * @return true if the leader was newly created, false if updated.
     */
    fun setGymLeader(uuid: UUID, playerName: String, gymType: String, badgeName: String): Boolean {
        val existing = findLeaderByUuid(uuid)
        return if (existing == null) {
            DatabaseManager.execute(
                "INSERT INTO gym_leaders (player_uuid, player_name, gym_type, badge_name, is_active) VALUES (?,?,?,?,1)",
                uuid.toString(), playerName, gymType, badgeName
            )
            logger.info("New gym leader registered: $playerName ($gymType)")
            true
        } else {
            DatabaseManager.execute(
                "UPDATE gym_leaders SET player_name=?, gym_type=?, badge_name=?, is_active=1 WHERE player_uuid=?",
                playerName, gymType, badgeName, uuid.toString()
            )
            logger.info("Gym leader updated: $playerName ($gymType)")
            false
        }
    }

    /** Remove a gym leader by UUID, returning true if found and removed. */
    fun removeGymLeader(uuid: UUID): Boolean {
        val leader = findLeaderByUuid(uuid) ?: return false
        DatabaseManager.execute(
            "UPDATE gym_leaders SET is_active=0 WHERE player_uuid=?",
            uuid.toString()
        )
        logger.info("Gym leader removed: ${leader.playerName}")
        return true
    }

    // -------------------------------------------------------------------------
    // Record battle outcome
    // -------------------------------------------------------------------------

    /** Record a challenger win (leader gets a loss). */
    fun recordChallengerWin(leaderUuid: UUID) {
        DatabaseManager.execute(
            "UPDATE gym_leaders SET losses = losses + 1 WHERE player_uuid=?",
            leaderUuid.toString()
        )
    }

    /** Record a leader win (leader gets a win). */
    fun recordLeaderWin(leaderUuid: UUID) {
        DatabaseManager.execute(
            "UPDATE gym_leaders SET wins = wins + 1 WHERE player_uuid=?",
            leaderUuid.toString()
        )
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns all active gym leaders. */
    fun getActiveLeaders(): List<GymLeader> =
        DatabaseManager.query(
            "SELECT id, player_uuid, player_name, gym_type, badge_name, wins, losses, is_active FROM gym_leaders WHERE is_active=1 ORDER BY gym_type"
        ) { rs ->
            GymLeader(
                id = rs.getInt("id"),
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                gymType = rs.getString("gym_type"),
                badgeName = rs.getString("badge_name"),
                wins = rs.getInt("wins"),
                losses = rs.getInt("losses"),
                isActive = rs.getInt("is_active") == 1
            )
        }

    /** Find a single active leader by UUID. */
    fun findLeaderByUuid(uuid: UUID): GymLeader? =
        DatabaseManager.query(
            "SELECT id, player_uuid, player_name, gym_type, badge_name, wins, losses, is_active FROM gym_leaders WHERE player_uuid=?",
            uuid.toString()
        ) { rs ->
            GymLeader(
                id = rs.getInt("id"),
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                gymType = rs.getString("gym_type"),
                badgeName = rs.getString("badge_name"),
                wins = rs.getInt("wins"),
                losses = rs.getInt("losses"),
                isActive = rs.getInt("is_active") == 1
            )
        }.firstOrNull()

    /** Returns true if the given UUID belongs to an active gym leader. */
    fun isGymLeader(uuid: UUID): Boolean = findLeaderByUuid(uuid)?.isActive == true
}
