package com.cobblegyms.system.gym

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import java.util.UUID

class GymLeaderManager {
    private val gymLeaders = mutableMapOf<UUID, GymLeader>()

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        val rows = DatabaseManager.executeQuery(
            "SELECT uuid, player_name, type_specialty, wins, losses, last_reward_date, active, assigned_at FROM gym_leaders WHERE active = 1"
        )
        for (row in rows) {
            val uuid = UUID.fromString(row["uuid"] as String)
            val leader = GymLeader(
                uuid = uuid,
                playerName = row["player_name"] as String,
                typeSpecialty = row["type_specialty"] as String,
                wins = (row["wins"] as Number).toInt(),
                losses = (row["losses"] as Number).toInt(),
                lastRewardDate = (row["last_reward_date"] as Number).toLong(),
                active = (row["active"] as Number).toInt() == 1,
                assignedAt = (row["assigned_at"] as Number).toLong()
            )
            gymLeaders[uuid] = leader
        }
        CobbleGymsMod.LOGGER.info("Loaded ${gymLeaders.size} gym leaders from database.")
    }

    fun setGymLeader(uuid: UUID, playerName: String, typeSpecialty: String): Boolean {
        if (gymLeaders.size >= CobbleGymsConfig.maxGymLeaders && !gymLeaders.containsKey(uuid)) {
            return false
        }
        val leader = GymLeader(uuid, playerName, typeSpecialty)
        gymLeaders[uuid] = leader
        DatabaseManager.executeUpdate(
            """INSERT INTO gym_leaders (uuid, player_name, type_specialty, wins, losses, last_reward_date, active, assigned_at)
               VALUES (?, ?, ?, 0, 0, 0, 1, ?)
               ON CONFLICT(uuid) DO UPDATE SET player_name=excluded.player_name, type_specialty=excluded.type_specialty, active=1""",
            uuid.toString(), playerName, typeSpecialty, System.currentTimeMillis()
        )
        return true
    }

    fun removeGymLeader(uuid: UUID): Boolean {
        if (!gymLeaders.containsKey(uuid)) return false
        gymLeaders.remove(uuid)
        DatabaseManager.executeUpdate(
            "UPDATE gym_leaders SET active = 0 WHERE uuid = ?",
            uuid.toString()
        )
        return true
    }

    fun getGymLeader(uuid: UUID): GymLeader? = gymLeaders[uuid]

    fun getAllGymLeaders(): List<GymLeader> = gymLeaders.values.toList()

    fun isGymLeader(uuid: UUID): Boolean = gymLeaders.containsKey(uuid)

    fun recordWin(uuid: UUID) {
        gymLeaders[uuid]?.let { leader ->
            leader.recordWin()
            DatabaseManager.executeUpdate(
                "UPDATE gym_leaders SET wins = ? WHERE uuid = ?",
                leader.wins, uuid.toString()
            )
        }
    }

    fun recordLoss(uuid: UUID) {
        gymLeaders[uuid]?.let { leader ->
            leader.recordLoss()
            DatabaseManager.executeUpdate(
                "UPDATE gym_leaders SET losses = ? WHERE uuid = ?",
                leader.losses, uuid.toString()
            )
        }
    }

    fun updateLastRewardDate(uuid: UUID, date: Long) {
        gymLeaders[uuid]?.let { leader ->
            leader.lastRewardDate = date
            DatabaseManager.executeUpdate(
                "UPDATE gym_leaders SET last_reward_date = ? WHERE uuid = ?",
                date, uuid.toString()
            )
        }
    }

    fun getLeaderByType(typeSpecialty: String): GymLeader? =
        gymLeaders.values.firstOrNull { it.typeSpecialty.equals(typeSpecialty, ignoreCase = true) }
}
