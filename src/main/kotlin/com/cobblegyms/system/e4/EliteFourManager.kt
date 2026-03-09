package com.cobblegyms.system.e4

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import java.util.UUID

class EliteFourManager {
    private val members = mutableMapOf<UUID, EliteFourMember>()

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        val rows = DatabaseManager.executeQuery(
            "SELECT uuid, player_name, position, type_specialty, wins, losses, last_reward_date, active, assigned_at FROM elite_four WHERE active = 1"
        )
        for (row in rows) {
            val uuid = UUID.fromString(row["uuid"] as String)
            val member = EliteFourMember(
                uuid = uuid,
                playerName = row["player_name"] as String,
                position = (row["position"] as Number).toInt(),
                typeSpecialty = row["type_specialty"] as String,
                wins = (row["wins"] as Number).toInt(),
                losses = (row["losses"] as Number).toInt(),
                lastRewardDate = (row["last_reward_date"] as Number).toLong(),
                active = (row["active"] as Number).toInt() == 1,
                assignedAt = (row["assigned_at"] as Number).toLong()
            )
            members[uuid] = member
        }
        CobbleGymsMod.LOGGER.info("Loaded ${members.size} Elite Four members from database.")
    }

    fun setMember(uuid: UUID, playerName: String, position: Int, typeSpecialty: String): Boolean {
        if (position < 1 || position > CobbleGymsConfig.maxEliteFourMembers) return false
        val member = EliteFourMember(uuid, playerName, position, typeSpecialty)
        members[uuid] = member
        DatabaseManager.executeUpdate(
            """INSERT INTO elite_four (uuid, player_name, position, type_specialty, wins, losses, last_reward_date, active, assigned_at)
               VALUES (?, ?, ?, ?, 0, 0, 0, 1, ?)
               ON CONFLICT(uuid) DO UPDATE SET player_name=excluded.player_name, position=excluded.position, type_specialty=excluded.type_specialty, active=1""",
            uuid.toString(), playerName, position, typeSpecialty, System.currentTimeMillis()
        )
        return true
    }

    fun removeMember(uuid: UUID): Boolean {
        if (!members.containsKey(uuid)) return false
        members.remove(uuid)
        DatabaseManager.executeUpdate(
            "UPDATE elite_four SET active = 0 WHERE uuid = ?",
            uuid.toString()
        )
        return true
    }

    fun getMember(uuid: UUID): EliteFourMember? = members[uuid]

    fun getAllMembers(): List<EliteFourMember> = members.values.sortedBy { it.position }

    fun isMember(uuid: UUID): Boolean = members.containsKey(uuid)

    fun getMemberCount(): Int = members.size

    fun recordWin(uuid: UUID) {
        members[uuid]?.let { member ->
            member.recordWin()
            DatabaseManager.executeUpdate(
                "UPDATE elite_four SET wins = ? WHERE uuid = ?",
                member.wins, uuid.toString()
            )
        }
    }

    fun recordLoss(uuid: UUID) {
        members[uuid]?.let { member ->
            member.recordLoss()
            DatabaseManager.executeUpdate(
                "UPDATE elite_four SET losses = ? WHERE uuid = ?",
                member.losses, uuid.toString()
            )
        }
    }

    fun updateLastRewardDate(uuid: UUID, date: Long) {
        members[uuid]?.let { member ->
            member.lastRewardDate = date
            DatabaseManager.executeUpdate(
                "UPDATE elite_four SET last_reward_date = ? WHERE uuid = ?",
                date, uuid.toString()
            )
        }
    }

    fun getStatistics(): Map<String, Any> {
        val allMembers = getAllMembers()
        return mapOf(
            "totalMembers" to allMembers.size,
            "totalBattles" to allMembers.sumOf { it.totalBattles },
            "totalWins" to allMembers.sumOf { it.wins },
            "averageWinRate" to if (allMembers.isNotEmpty()) allMembers.map { it.winRate }.average() else 0.0
        )
    }
}
