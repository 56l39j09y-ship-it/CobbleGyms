package com.cobblegyms.data

import com.cobblegyms.CobbleGyms
import net.fabricmc.loader.api.FabricLoader
import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {
    private lateinit var connection: Connection
    private val dbFile = FabricLoader.getInstance().gameDir.resolve("cobblegyms.db").toFile()
    
    fun initialize() {
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.autoCommit = true
        createTables()
        CobbleGyms.LOGGER.info("Database initialized at ${dbFile.absolutePath}")
    }
    
    fun getConnection(): Connection = connection
    
    fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }
    
    private fun createTables() {
        val stmt = connection.createStatement()
        
        // Seasons table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS seasons (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number INTEGER NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                active INTEGER NOT NULL DEFAULT 1
            )
        """)
        
        // Gym leaders table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS gym_leaders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL UNIQUE,
                player_name TEXT NOT NULL,
                gym_type TEXT NOT NULL,
                battle_format TEXT NOT NULL DEFAULT 'SINGLES',
                is_open INTEGER NOT NULL DEFAULT 0,
                gym_location TEXT,
                extra_banned_pokemon TEXT,
                extra_ban_season INTEGER DEFAULT 0,
                multi_team_enabled INTEGER NOT NULL DEFAULT 0,
                team1 TEXT,
                team2 TEXT,
                team3 TEXT,
                current_team_slot INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """)
        
        // Elite Four table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS elite_four (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL UNIQUE,
                player_name TEXT NOT NULL,
                type1 TEXT NOT NULL,
                type2 TEXT NOT NULL,
                battle_format TEXT NOT NULL DEFAULT 'SINGLES',
                is_open INTEGER NOT NULL DEFAULT 0,
                arena_location TEXT,
                multi_team_enabled INTEGER NOT NULL DEFAULT 0,
                team1 TEXT,
                team2 TEXT,
                team3 TEXT,
                current_team_slot INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """)
        
        // Champion table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS champion (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL UNIQUE,
                player_name TEXT NOT NULL,
                battle_format TEXT NOT NULL DEFAULT 'SINGLES',
                is_open INTEGER NOT NULL DEFAULT 0,
                arena_location TEXT,
                multi_team_enabled INTEGER NOT NULL DEFAULT 0,
                team1 TEXT,
                team2 TEXT,
                team3 TEXT,
                current_team_slot INTEGER NOT NULL DEFAULT 1,
                appointed_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """)
        
        // Player badges table (season-specific)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS player_badges (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                season_id INTEGER NOT NULL,
                gym_type TEXT NOT NULL,
                earned_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                UNIQUE(player_uuid, season_id, gym_type)
            )
        """)
        
        // Player E4 victories table (season-specific)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS player_e4_victories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                season_id INTEGER NOT NULL,
                e4_uuid TEXT NOT NULL,
                e4_name TEXT NOT NULL,
                earned_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                UNIQUE(player_uuid, season_id, e4_uuid)
            )
        """)
        
        // Player champion victories
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS player_champion_victories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                season_id INTEGER NOT NULL,
                earned_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """)
        
        // Battle records table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS battle_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                season_id INTEGER NOT NULL,
                battle_type TEXT NOT NULL,
                leader_uuid TEXT NOT NULL,
                leader_name TEXT NOT NULL,
                challenger_uuid TEXT NOT NULL,
                challenger_name TEXT NOT NULL,
                winner TEXT NOT NULL,
                challenger_team TEXT,
                leader_team TEXT,
                turns INTEGER NOT NULL DEFAULT 0,
                battle_time INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                notes TEXT
            )
        """)
        
        // Challenge cooldowns table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS challenge_cooldowns (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                challenger_uuid TEXT NOT NULL,
                target_uuid TEXT NOT NULL,
                target_type TEXT NOT NULL,
                last_challenge INTEGER NOT NULL,
                UNIQUE(challenger_uuid, target_uuid, target_type)
            )
        """)
        
        // Gym bans table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS gym_bans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                gym_leader_uuid TEXT NOT NULL,
                banned_player_uuid TEXT NOT NULL,
                banned_player_name TEXT NOT NULL,
                reason TEXT,
                ban_until INTEGER NOT NULL,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """)
        
        // Weekly stats table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS weekly_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                role TEXT NOT NULL,
                week_start INTEGER NOT NULL,
                battles INTEGER NOT NULL DEFAULT 0,
                wins INTEGER NOT NULL DEFAULT 0,
                losses INTEGER NOT NULL DEFAULT 0,
                UNIQUE(player_uuid, week_start)
            )
        """)
        
        // Active battles table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS active_battles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                battle_id TEXT NOT NULL UNIQUE,
                battle_type TEXT NOT NULL,
                leader_uuid TEXT NOT NULL,
                challenger_uuid TEXT NOT NULL,
                start_time INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                turns INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'ACTIVE'
            )
        """)
        
        // Challenge queue table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS challenge_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                target_uuid TEXT NOT NULL,
                target_type TEXT NOT NULL,
                challenger_uuid TEXT NOT NULL,
                challenger_name TEXT NOT NULL,
                queued_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                position INTEGER NOT NULL
            )
        """)
        
        stmt.close()
    }
}
