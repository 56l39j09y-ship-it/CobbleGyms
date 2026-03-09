package com.cobblegyms.managers

import com.cobblegyms.database.DatabaseManager
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Manages the Elite Four: registration, ordering, and battle records.
 */
class E4Manager {

    private val logger = LoggerFactory.getLogger("CobbleGyms/E4Manager")

    data class EliteFourMember(
        val id: Int,
        val playerUuid: UUID,
        val playerName: String,
        val position: Int,
        val wins: Int,
        val losses: Int,
        val isActive: Boolean
    )

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Assign a player to an Elite Four position (1-4).
     * @return true if newly inserted, false if updated.
     */
    fun setEliteFour(uuid: UUID, playerName: String, position: Int): Boolean {
        require(position in 1..4) { "Elite Four position must be between 1 and 4" }

        // Clear any other player already in that position
        DatabaseManager.execute(
            "UPDATE elite_four SET is_active=0 WHERE position=? AND player_uuid != ?",
            position, uuid.toString()
        )

        val existing = findMemberByUuid(uuid)
        return if (existing == null) {
            DatabaseManager.execute(
                "INSERT INTO elite_four (player_uuid, player_name, position, is_active) VALUES (?,?,?,1)",
                uuid.toString(), playerName, position
            )
            logger.info("Elite Four position $position assigned to $playerName")
            true
        } else {
            DatabaseManager.execute(
                "UPDATE elite_four SET player_name=?, position=?, is_active=1 WHERE player_uuid=?",
                playerName, position, uuid.toString()
            )
            logger.info("Elite Four member updated: $playerName (position $position)")
            false
        }
    }

    /** Remove an Elite Four member. */
    fun removeEliteFour(uuid: UUID): Boolean {
        val member = findMemberByUuid(uuid) ?: return false
        DatabaseManager.execute(
            "UPDATE elite_four SET is_active=0 WHERE player_uuid=?",
            uuid.toString()
        )
        logger.info("Elite Four member removed: ${member.playerName}")
        return true
    }

    // -------------------------------------------------------------------------
    // Record battle outcome
    // -------------------------------------------------------------------------

    fun recordChallengerWin(memberUuid: UUID) {
        DatabaseManager.execute(
            "UPDATE elite_four SET losses = losses + 1 WHERE player_uuid=?",
            memberUuid.toString()
        )
    }

    fun recordMemberWin(memberUuid: UUID) {
        DatabaseManager.execute(
            "UPDATE elite_four SET wins = wins + 1 WHERE player_uuid=?",
            memberUuid.toString()
        )
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns all active Elite Four members ordered by position. */
    fun getActiveMembers(): List<EliteFourMember> =
        DatabaseManager.query(
            "SELECT id, player_uuid, player_name, position, wins, losses, is_active FROM elite_four WHERE is_active=1 ORDER BY position"
        ) { rs ->
            EliteFourMember(
                id = rs.getInt("id"),
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                position = rs.getInt("position"),
                wins = rs.getInt("wins"),
                losses = rs.getInt("losses"),
                isActive = rs.getInt("is_active") == 1
            )
        }

    fun findMemberByUuid(uuid: UUID): EliteFourMember? =
        DatabaseManager.query(
            "SELECT id, player_uuid, player_name, position, wins, losses, is_active FROM elite_four WHERE player_uuid=?",
            uuid.toString()
        ) { rs ->
            EliteFourMember(
                id = rs.getInt("id"),
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                position = rs.getInt("position"),
                wins = rs.getInt("wins"),
                losses = rs.getInt("losses"),
                isActive = rs.getInt("is_active") == 1
            )
        }.firstOrNull()

    fun isEliteFour(uuid: UUID): Boolean = findMemberByUuid(uuid)?.isActive == true
}
