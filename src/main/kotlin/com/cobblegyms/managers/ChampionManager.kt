package com.cobblegyms.managers

import com.cobblegyms.database.DatabaseManager
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Manages the single Champion title: crowning, defense records, and history.
 */
class ChampionManager {

    private val logger = LoggerFactory.getLogger("CobbleGyms/ChampionManager")

    data class Champion(
        val id: Int,
        val playerUuid: UUID,
        val playerName: String,
        val defenses: Int,
        val becameChampion: String
    )

    // -------------------------------------------------------------------------
    // Crown / dethrone
    // -------------------------------------------------------------------------

    /**
     * Crown a new champion. Archives any existing champion record automatically.
     */
    fun setChampion(uuid: UUID, playerName: String) {
        // Archive previous champion by leaving their record (history stays in table).
        // We simply insert a new row; queries always fetch the latest.
        DatabaseManager.execute(
            "INSERT INTO champion (player_uuid, player_name, defenses, became_champion) VALUES (?,?,0,datetime('now'))",
            uuid.toString(), playerName
        )
        logger.info("New champion crowned: $playerName")
    }

    // -------------------------------------------------------------------------
    // Defense records
    // -------------------------------------------------------------------------

    fun recordDefense(championUuid: UUID) {
        DatabaseManager.execute(
            "UPDATE champion SET defenses = defenses + 1 WHERE player_uuid=? AND id = (SELECT MAX(id) FROM champion)",
            championUuid.toString()
        )
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns the current champion (most recent record). */
    fun getCurrentChampion(): Champion? =
        DatabaseManager.query(
            "SELECT id, player_uuid, player_name, defenses, became_champion FROM champion ORDER BY id DESC LIMIT 1"
        ) { rs ->
            Champion(
                id = rs.getInt("id"),
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                defenses = rs.getInt("defenses"),
                becameChampion = rs.getString("became_champion")
            )
        }.firstOrNull()

    fun isChampion(uuid: UUID): Boolean = getCurrentChampion()?.playerUuid == uuid

    /** Returns the full champion history (oldest to newest). */
    fun getHistory(): List<Champion> =
        DatabaseManager.query(
            "SELECT id, player_uuid, player_name, defenses, became_champion FROM champion ORDER BY id"
        ) { rs ->
            Champion(
                id = rs.getInt("id"),
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                defenses = rs.getInt("defenses"),
                becameChampion = rs.getString("became_champion")
            )
        }
}
