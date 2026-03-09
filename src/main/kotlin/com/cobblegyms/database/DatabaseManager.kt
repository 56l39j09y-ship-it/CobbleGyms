package com.cobblegyms.database

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.config.CobbleGymsConfig
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseManager {
    private var connection: Connection? = null

    fun initialize() {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:${CobbleGymsConfig.databaseFile}")
            createTables()
            CobbleGymsMod.LOGGER.info("SQLite database initialized: ${CobbleGymsConfig.databaseFile}")
        } catch (e: Exception) {
            CobbleGymsMod.LOGGER.error("Failed to initialize database: ${e.message}")
        }
    }

    private fun createTables() {
        val conn = connection ?: return
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gym_leaders (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    type_specialty TEXT NOT NULL,
                    wins INTEGER DEFAULT 0,
                    losses INTEGER DEFAULT 0,
                    last_reward_date INTEGER DEFAULT 0,
                    active INTEGER DEFAULT 1,
                    assigned_at INTEGER DEFAULT 0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS elite_four (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    type_specialty TEXT NOT NULL,
                    wins INTEGER DEFAULT 0,
                    losses INTEGER DEFAULT 0,
                    last_reward_date INTEGER DEFAULT 0,
                    active INTEGER DEFAULT 1,
                    assigned_at INTEGER DEFAULT 0
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS champion (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    wins INTEGER DEFAULT 0,
                    losses INTEGER DEFAULT 0,
                    last_reward_date INTEGER DEFAULT 0,
                    title_date INTEGER DEFAULT 0,
                    active INTEGER DEFAULT 1
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_bans (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    reason TEXT,
                    banned_by TEXT,
                    ban_date INTEGER DEFAULT 0,
                    expiry_date INTEGER DEFAULT -1
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS seasons (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    start_date INTEGER NOT NULL,
                    end_date INTEGER,
                    active INTEGER DEFAULT 1,
                    champion_uuid TEXT,
                    champion_name TEXT
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reward_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    reward_amount INTEGER NOT NULL,
                    reward_type TEXT NOT NULL,
                    reward_date INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    fun getConnection(): Connection? = connection

    fun executeQuery(sql: String, vararg params: Any?): List<Map<String, Any?>> {
        val conn = connection ?: return emptyList()
        val results = mutableListOf<Map<String, Any?>>()
        try {
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, param -> stmt.setObject(i + 1, param) }
                val rs = stmt.executeQuery()
                val metaData = rs.metaData
                val colCount = metaData.columnCount
                while (rs.next()) {
                    val row = mutableMapOf<String, Any?>()
                    for (i in 1..colCount) {
                        row[metaData.getColumnName(i)] = rs.getObject(i)
                    }
                    results.add(row)
                }
            }
        } catch (e: SQLException) {
            CobbleGymsMod.LOGGER.error("Database query failed: ${e.message}")
        }
        return results
    }

    fun executeUpdate(sql: String, vararg params: Any?): Int {
        val conn = connection ?: return 0
        return try {
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, param -> stmt.setObject(i + 1, param) }
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            CobbleGymsMod.LOGGER.error("Database update failed: ${e.message}")
            0
        }
    }

    fun close() {
        try {
            connection?.close()
            connection = null
        } catch (e: Exception) {
            CobbleGymsMod.LOGGER.error("Error closing database: ${e.message}")
        }
    }
}
