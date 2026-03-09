package com.cobblegyms.database

import com.cobblegyms.config.CobbleGymsConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Manages the SQLite database connection and schema for CobbleGyms.
 *
 * Tables:
 *  - gym_leaders      : registered gym leaders
 *  - elite_four       : registered Elite Four members
 *  - champion         : current champion record
 *  - season           : season metadata and history
 *  - player_stats     : per-player win/loss statistics
 *  - battle_history   : detailed log of every battle
 */
object DatabaseManager {

    private val logger = LoggerFactory.getLogger("CobbleGyms/Database")
    private var connection: Connection? = null

    fun initialize(config: CobbleGymsConfig.DatabaseConfig) {
        val dbFile = File(config.filePath)
        dbFile.parentFile?.mkdirs()
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            connection!!.autoCommit = true
            logger.info("Database connected: ${dbFile.absolutePath}")
            createSchema()
        } catch (e: Exception) {
            logger.error("Failed to initialize database: ${e.message}", e)
        }
    }

    fun close() {
        try {
            connection?.close()
            logger.info("Database connection closed")
        } catch (e: SQLException) {
            logger.warn("Error closing database: ${e.message}")
        }
    }

    fun getConnection(): Connection? = connection

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private fun createSchema() {
        val ddl = listOf(
            """
            CREATE TABLE IF NOT EXISTS gym_leaders (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT    NOT NULL UNIQUE,
                player_name TEXT    NOT NULL,
                gym_type    TEXT    NOT NULL,
                badge_name  TEXT    NOT NULL,
                wins        INTEGER NOT NULL DEFAULT 0,
                losses      INTEGER NOT NULL DEFAULT 0,
                is_active   INTEGER NOT NULL DEFAULT 1,
                created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS elite_four (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT    NOT NULL UNIQUE,
                player_name TEXT    NOT NULL,
                position    INTEGER NOT NULL CHECK (position BETWEEN 1 AND 4),
                wins        INTEGER NOT NULL DEFAULT 0,
                losses      INTEGER NOT NULL DEFAULT 0,
                is_active   INTEGER NOT NULL DEFAULT 1,
                created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS champion (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid     TEXT    NOT NULL,
                player_name     TEXT    NOT NULL,
                defenses        INTEGER NOT NULL DEFAULT 0,
                became_champion TEXT    NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS season (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                number      INTEGER NOT NULL,
                start_date  TEXT    NOT NULL,
                end_date    TEXT,
                is_active   INTEGER NOT NULL DEFAULT 1
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS player_stats (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid     TEXT    NOT NULL,
                player_name     TEXT    NOT NULL,
                season_id       INTEGER NOT NULL,
                gym_wins        INTEGER NOT NULL DEFAULT 0,
                gym_losses      INTEGER NOT NULL DEFAULT 0,
                e4_wins         INTEGER NOT NULL DEFAULT 0,
                e4_losses       INTEGER NOT NULL DEFAULT 0,
                champion_wins   INTEGER NOT NULL DEFAULT 0,
                champion_losses INTEGER NOT NULL DEFAULT 0,
                badges_earned   INTEGER NOT NULL DEFAULT 0,
                last_updated    TEXT    NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (season_id) REFERENCES season(id),
                UNIQUE (player_uuid, season_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS battle_history (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                season_id       INTEGER NOT NULL,
                challenger_uuid TEXT    NOT NULL,
                challenger_name TEXT    NOT NULL,
                defender_uuid   TEXT    NOT NULL,
                defender_name   TEXT    NOT NULL,
                battle_type     TEXT    NOT NULL,
                winner_uuid     TEXT    NOT NULL,
                battle_date     TEXT    NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (season_id) REFERENCES season(id)
            )
            """
        )

        val conn = connection ?: return
        try {
            conn.createStatement().use { stmt ->
                ddl.forEach { sql -> stmt.executeUpdate(sql.trimIndent()) }
            }
            logger.info("Database schema verified/created successfully")
        } catch (e: SQLException) {
            logger.error("Schema creation failed: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    fun execute(sql: String, vararg params: Any?) {
        val conn = connection ?: throw IllegalStateException("Database not connected")
        conn.prepareStatement(sql).use { ps ->
            params.forEachIndexed { i, v -> ps.setObject(i + 1, v) }
            ps.executeUpdate()
        }
    }

    fun <T> query(sql: String, vararg params: Any?, mapper: (java.sql.ResultSet) -> T): List<T> {
        val conn = connection ?: throw IllegalStateException("Database not connected")
        val results = mutableListOf<T>()
        conn.prepareStatement(sql).use { ps ->
            params.forEachIndexed { i, v -> ps.setObject(i + 1, v) }
            ps.executeQuery().use { rs ->
                while (rs.next()) results.add(mapper(rs))
            }
        }
        return results
    }
}
