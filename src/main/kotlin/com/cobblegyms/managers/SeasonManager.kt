package com.cobblegyms.managers

import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import com.cobblegyms.utils.CooldownManager
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Manages competitive seasons: start, end, reset, and player statistics.
 */
class SeasonManager(private val config: CobbleGymsConfig.SeasonConfig = CobbleGymsConfig.get().season) {

    private val logger = LoggerFactory.getLogger("CobbleGyms/SeasonManager")

    data class Season(
        val id: Int,
        val number: Int,
        val startDate: String,
        val endDate: String?,
        val isActive: Boolean
    )

    data class PlayerStats(
        val playerUuid: UUID,
        val playerName: String,
        val seasonId: Int,
        val gymWins: Int,
        val gymLosses: Int,
        val e4Wins: Int,
        val e4Losses: Int,
        val championWins: Int,
        val championLosses: Int,
        val badgesEarned: Int
    )

    // -------------------------------------------------------------------------
    // Season lifecycle
    // -------------------------------------------------------------------------

    /** Returns the currently active season, or null if none exists. */
    fun getActiveSeason(): Season? =
        DatabaseManager.query(
            "SELECT id, number, start_date, end_date, is_active FROM season WHERE is_active=1 ORDER BY id DESC LIMIT 1"
        ) { rs ->
            Season(
                id = rs.getInt("id"),
                number = rs.getInt("number"),
                startDate = rs.getString("start_date"),
                endDate = rs.getString("end_date"),
                isActive = rs.getInt("is_active") == 1
            )
        }.firstOrNull()

    /**
     * Start a new season. Ends any currently active season first.
     * @return the new season number.
     */
    fun startNewSeason(): Int {
        val current = getActiveSeason()
        val nextNumber = (current?.number ?: 0) + 1

        // Close the current season
        current?.let {
            DatabaseManager.execute(
                "UPDATE season SET is_active=0, end_date=datetime('now') WHERE id=?",
                it.id
            )
        }

        // Start the new season
        DatabaseManager.execute(
            "INSERT INTO season (number, start_date, is_active) VALUES (?,datetime('now'),1)",
            nextNumber
        )

        // Clear reward cooldowns so everyone can earn rewards in the new season
        CooldownManager.clearAll()

        logger.info("Season $nextNumber started")
        return nextNumber
    }

    /**
     * End the current season without starting a new one.
     */
    fun endCurrentSeason(): Boolean {
        val current = getActiveSeason() ?: return false
        DatabaseManager.execute(
            "UPDATE season SET is_active=0, end_date=datetime('now') WHERE id=?",
            current.id
        )
        logger.info("Season ${current.number} ended")
        return true
    }

    /** Returns a list of all seasons (most recent first). */
    fun getAllSeasons(): List<Season> =
        DatabaseManager.query(
            "SELECT id, number, start_date, end_date, is_active FROM season ORDER BY id DESC"
        ) { rs ->
            Season(
                id = rs.getInt("id"),
                number = rs.getInt("number"),
                startDate = rs.getString("start_date"),
                endDate = rs.getString("end_date"),
                isActive = rs.getInt("is_active") == 1
            )
        }

    // -------------------------------------------------------------------------
    // Player statistics
    // -------------------------------------------------------------------------

    private fun ensurePlayerStats(seasonId: Int, uuid: UUID, playerName: String) {
        DatabaseManager.execute(
            """
            INSERT OR IGNORE INTO player_stats 
                (player_uuid, player_name, season_id) 
            VALUES (?,?,?)
            """.trimIndent(),
            uuid.toString(), playerName, seasonId
        )
        // Update name in case it changed
        DatabaseManager.execute(
            "UPDATE player_stats SET player_name=? WHERE player_uuid=? AND season_id=?",
            playerName, uuid.toString(), seasonId
        )
    }

    fun recordGymBattle(seasonId: Int, challengerUuid: UUID, challengerName: String, won: Boolean) {
        ensurePlayerStats(seasonId, challengerUuid, challengerName)
        if (won) {
            DatabaseManager.execute(
                "UPDATE player_stats SET gym_wins=gym_wins+1, badges_earned=badges_earned+1, last_updated=datetime('now') WHERE player_uuid=? AND season_id=?",
                challengerUuid.toString(), seasonId
            )
        } else {
            DatabaseManager.execute(
                "UPDATE player_stats SET gym_losses=gym_losses+1, last_updated=datetime('now') WHERE player_uuid=? AND season_id=?",
                challengerUuid.toString(), seasonId
            )
        }
    }

    fun recordE4Battle(seasonId: Int, challengerUuid: UUID, challengerName: String, won: Boolean) {
        ensurePlayerStats(seasonId, challengerUuid, challengerName)
        if (won) {
            DatabaseManager.execute(
                "UPDATE player_stats SET e4_wins=e4_wins+1, last_updated=datetime('now') WHERE player_uuid=? AND season_id=?",
                challengerUuid.toString(), seasonId
            )
        } else {
            DatabaseManager.execute(
                "UPDATE player_stats SET e4_losses=e4_losses+1, last_updated=datetime('now') WHERE player_uuid=? AND season_id=?",
                challengerUuid.toString(), seasonId
            )
        }
    }

    fun recordChampionBattle(seasonId: Int, challengerUuid: UUID, challengerName: String, won: Boolean) {
        ensurePlayerStats(seasonId, challengerUuid, challengerName)
        if (won) {
            DatabaseManager.execute(
                "UPDATE player_stats SET champion_wins=champion_wins+1, last_updated=datetime('now') WHERE player_uuid=? AND season_id=?",
                challengerUuid.toString(), seasonId
            )
        } else {
            DatabaseManager.execute(
                "UPDATE player_stats SET champion_losses=champion_losses+1, last_updated=datetime('now') WHERE player_uuid=? AND season_id=?",
                challengerUuid.toString(), seasonId
            )
        }
    }

    /** Returns the leaderboard for a season ordered by badges earned then total wins. */
    fun getLeaderboard(seasonId: Int, limit: Int = 10): List<PlayerStats> =
        DatabaseManager.query(
            """
            SELECT player_uuid, player_name, season_id,
                   gym_wins, gym_losses, e4_wins, e4_losses,
                   champion_wins, champion_losses, badges_earned
            FROM player_stats
            WHERE season_id=?
            ORDER BY badges_earned DESC, (gym_wins + e4_wins + champion_wins) DESC
            LIMIT ?
            """.trimIndent(),
            seasonId, limit
        ) { rs ->
            PlayerStats(
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                seasonId = rs.getInt("season_id"),
                gymWins = rs.getInt("gym_wins"),
                gymLosses = rs.getInt("gym_losses"),
                e4Wins = rs.getInt("e4_wins"),
                e4Losses = rs.getInt("e4_losses"),
                championWins = rs.getInt("champion_wins"),
                championLosses = rs.getInt("champion_losses"),
                badgesEarned = rs.getInt("badges_earned")
            )
        }

    fun getPlayerStats(seasonId: Int, uuid: UUID): PlayerStats? =
        DatabaseManager.query(
            """
            SELECT player_uuid, player_name, season_id,
                   gym_wins, gym_losses, e4_wins, e4_losses,
                   champion_wins, champion_losses, badges_earned
            FROM player_stats
            WHERE season_id=? AND player_uuid=?
            """.trimIndent(),
            seasonId, uuid.toString()
        ) { rs ->
            PlayerStats(
                playerUuid = UUID.fromString(rs.getString("player_uuid")),
                playerName = rs.getString("player_name"),
                seasonId = rs.getInt("season_id"),
                gymWins = rs.getInt("gym_wins"),
                gymLosses = rs.getInt("gym_losses"),
                e4Wins = rs.getInt("e4_wins"),
                e4Losses = rs.getInt("e4_losses"),
                championWins = rs.getInt("champion_wins"),
                championLosses = rs.getInt("champion_losses"),
                badgesEarned = rs.getInt("badges_earned")
            )
        }.firstOrNull()
}
