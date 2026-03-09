package com.cobblegyms.system.champion

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.database.DatabaseManager
import java.util.UUID

data class Champion(
    val uuid: UUID,
    val playerName: String,
    var wins: Int = 0,
    var losses: Int = 0,
    var lastRewardDate: Long = 0L,
    val titleDate: Long = System.currentTimeMillis(),
    var active: Boolean = true
) {
    val totalBattles: Int get() = wins + losses
    val winRate: Double get() = if (totalBattles > 0) wins.toDouble() / totalBattles * 100 else 0.0
}

class ChampionManager {
    private var currentChampion: Champion? = null
    private val bannedPlayers = mutableMapOf<UUID, PlayerBan>()

    data class PlayerBan(
        val uuid: UUID,
        val playerName: String,
        val reason: String,
        val bannedBy: String,
        val banDate: Long = System.currentTimeMillis(),
        val expiryDate: Long = -1L
    ) {
        fun isExpired(): Boolean = expiryDate != -1L && System.currentTimeMillis() > expiryDate
    }

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        val rows = DatabaseManager.executeQuery(
            "SELECT uuid, player_name, wins, losses, last_reward_date, title_date, active FROM champion WHERE active = 1 LIMIT 1"
        )
        if (rows.isNotEmpty()) {
            val row = rows[0]
            val uuid = UUID.fromString(row["uuid"] as String)
            currentChampion = Champion(
                uuid = uuid,
                playerName = row["player_name"] as String,
                wins = (row["wins"] as Number).toInt(),
                losses = (row["losses"] as Number).toInt(),
                lastRewardDate = (row["last_reward_date"] as Number).toLong(),
                titleDate = (row["title_date"] as Number).toLong(),
                active = true
            )
        }

        val banRows = DatabaseManager.executeQuery(
            "SELECT uuid, player_name, reason, banned_by, ban_date, expiry_date FROM player_bans"
        )
        for (row in banRows) {
            val uuid = UUID.fromString(row["uuid"] as String)
            val ban = PlayerBan(
                uuid = uuid,
                playerName = row["player_name"] as String,
                reason = (row["reason"] as? String) ?: "",
                bannedBy = (row["banned_by"] as? String) ?: "unknown",
                banDate = (row["ban_date"] as Number).toLong(),
                expiryDate = (row["expiry_date"] as Number).toLong()
            )
            if (!ban.isExpired()) {
                bannedPlayers[uuid] = ban
            }
        }
        CobbleGymsMod.LOGGER.info("Champion system loaded. Current champion: ${currentChampion?.playerName ?: "None"}")
    }

    fun setChampion(uuid: UUID, playerName: String): Boolean {
        val previousChampion = currentChampion
        previousChampion?.let {
            DatabaseManager.executeUpdate(
                "UPDATE champion SET active = 0 WHERE uuid = ?",
                it.uuid.toString()
            )
        }

        val champion = Champion(uuid, playerName)
        currentChampion = champion
        DatabaseManager.executeUpdate(
            """INSERT INTO champion (uuid, player_name, wins, losses, last_reward_date, title_date, active)
               VALUES (?, ?, 0, 0, 0, ?, 1)
               ON CONFLICT(uuid) DO UPDATE SET player_name=excluded.player_name, wins=0, losses=0, title_date=excluded.title_date, active=1""",
            uuid.toString(), playerName, System.currentTimeMillis()
        )
        return true
    }

    fun removeChampion(): Boolean {
        val champion = currentChampion ?: return false
        DatabaseManager.executeUpdate(
            "UPDATE champion SET active = 0 WHERE uuid = ?",
            champion.uuid.toString()
        )
        currentChampion = null
        return true
    }

    fun getChampion(): Champion? = currentChampion

    fun isChampion(uuid: UUID): Boolean = currentChampion?.uuid == uuid

    fun recordWin(uuid: UUID) {
        val champion = currentChampion ?: return
        if (champion.uuid == uuid) {
            champion.wins++
            DatabaseManager.executeUpdate(
                "UPDATE champion SET wins = ? WHERE uuid = ?",
                champion.wins, uuid.toString()
            )
        }
    }

    fun recordLoss(uuid: UUID) {
        val champion = currentChampion ?: return
        if (champion.uuid == uuid) {
            champion.losses++
            DatabaseManager.executeUpdate(
                "UPDATE champion SET losses = ? WHERE uuid = ?",
                champion.losses, uuid.toString()
            )
        }
    }

    fun updateLastRewardDate(uuid: UUID, date: Long) {
        val champion = currentChampion ?: return
        if (champion.uuid == uuid) {
            champion.lastRewardDate = date
            DatabaseManager.executeUpdate(
                "UPDATE champion SET last_reward_date = ? WHERE uuid = ?",
                date, uuid.toString()
            )
        }
    }

    fun banPlayer(uuid: UUID, playerName: String, reason: String, bannedBy: String, durationDays: Int = -1): Boolean {
        val expiryDate = if (durationDays > 0) {
            System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000L)
        } else -1L
        val ban = PlayerBan(uuid, playerName, reason, bannedBy, expiryDate = expiryDate)
        bannedPlayers[uuid] = ban
        DatabaseManager.executeUpdate(
            """INSERT INTO player_bans (uuid, player_name, reason, banned_by, ban_date, expiry_date)
               VALUES (?, ?, ?, ?, ?, ?)
               ON CONFLICT(uuid) DO UPDATE SET reason=excluded.reason, banned_by=excluded.banned_by, ban_date=excluded.ban_date, expiry_date=excluded.expiry_date""",
            uuid.toString(), playerName, reason, bannedBy, ban.banDate, expiryDate
        )
        return true
    }

    fun unbanPlayer(uuid: UUID): Boolean {
        if (!bannedPlayers.containsKey(uuid)) return false
        bannedPlayers.remove(uuid)
        DatabaseManager.executeUpdate("DELETE FROM player_bans WHERE uuid = ?", uuid.toString())
        return true
    }

    fun isBanned(uuid: UUID): Boolean {
        val ban = bannedPlayers[uuid] ?: return false
        if (ban.isExpired()) {
            bannedPlayers.remove(uuid)
            return false
        }
        return true
    }

    fun getBan(uuid: UUID): PlayerBan? {
        val ban = bannedPlayers[uuid] ?: return null
        if (ban.isExpired()) {
            bannedPlayers.remove(uuid)
            return null
        }
        return ban
    }

    fun getAllBans(): List<PlayerBan> {
        bannedPlayers.entries.removeIf { it.value.isExpired() }
        return bannedPlayers.values.toList()
    }
}
