package com.cobblegyms.system.season

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.config.CobbleGymsConfig
import com.cobblegyms.database.DatabaseManager
import java.util.UUID

data class Season(
    val id: Int,
    val startDate: Long,
    var endDate: Long? = null,
    var active: Boolean = true,
    var championUuid: UUID? = null,
    var championName: String? = null
) {
    fun durationDays(): Long {
        val end = endDate ?: System.currentTimeMillis()
        return (end - startDate) / (24 * 60 * 60 * 1000L)
    }

    fun isExpired(): Boolean {
        val seasonEndMs = startDate + (CobbleGymsConfig.seasonDurationDays * 24 * 60 * 60 * 1000L)
        return System.currentTimeMillis() > seasonEndMs
    }
}

class SeasonManager {
    private var currentSeason: Season? = null

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        val rows = DatabaseManager.executeQuery(
            "SELECT id, start_date, end_date, active, champion_uuid, champion_name FROM seasons WHERE active = 1 LIMIT 1"
        )
        if (rows.isNotEmpty()) {
            val row = rows[0]
            currentSeason = Season(
                id = (row["id"] as Number).toInt(),
                startDate = (row["start_date"] as Number).toLong(),
                endDate = (row["end_date"] as? Number)?.toLong(),
                active = true,
                championUuid = (row["champion_uuid"] as? String)?.let { UUID.fromString(it) },
                championName = row["champion_name"] as? String
            )
        }

        if (currentSeason == null) {
            startNewSeason()
        } else if (currentSeason!!.isExpired()) {
            CobbleGymsMod.LOGGER.info("Current season has expired. Starting a new season.")
            endCurrentSeason()
            startNewSeason()
        }
    }

    fun startNewSeason(): Season {
        val now = System.currentTimeMillis()
        DatabaseManager.executeUpdate(
            "INSERT INTO seasons (start_date, active) VALUES (?, 1)",
            now
        )
        val rows = DatabaseManager.executeQuery("SELECT id FROM seasons ORDER BY id DESC LIMIT 1")
        val id = if (rows.isNotEmpty()) (rows[0]["id"] as Number).toInt() else 1
        val season = Season(id, now)
        currentSeason = season
        CobbleGymsMod.LOGGER.info("New season #$id started.")
        return season
    }

    fun endCurrentSeason(championUuid: UUID? = null, championName: String? = null): Boolean {
        val season = currentSeason ?: return false
        val now = System.currentTimeMillis()
        season.endDate = now
        season.active = false
        season.championUuid = championUuid
        season.championName = championName
        DatabaseManager.executeUpdate(
            "UPDATE seasons SET active = 0, end_date = ?, champion_uuid = ?, champion_name = ? WHERE id = ?",
            now, championUuid?.toString(), championName, season.id
        )
        CobbleGymsMod.LOGGER.info("Season #${season.id} ended.")
        currentSeason = null
        return true
    }

    fun getCurrentSeason(): Season? = currentSeason

    fun isSeasonActive(): Boolean = currentSeason != null && !currentSeason!!.isExpired()

    fun getDaysRemaining(): Long {
        val season = currentSeason ?: return 0
        val seasonEndMs = season.startDate + (CobbleGymsConfig.seasonDurationDays * 24 * 60 * 60 * 1000L)
        val remaining = seasonEndMs - System.currentTimeMillis()
        return if (remaining > 0) remaining / (24 * 60 * 60 * 1000L) else 0
    }

    fun getSeasonHistory(): List<Season> {
        val rows = DatabaseManager.executeQuery(
            "SELECT id, start_date, end_date, active, champion_uuid, champion_name FROM seasons ORDER BY id DESC LIMIT 10"
        )
        return rows.map { row ->
            Season(
                id = (row["id"] as Number).toInt(),
                startDate = (row["start_date"] as Number).toLong(),
                endDate = (row["end_date"] as? Number)?.toLong(),
                active = (row["active"] as Number).toInt() == 1,
                championUuid = (row["champion_uuid"] as? String)?.let { UUID.fromString(it) },
                championName = row["champion_name"] as? String
            )
        }
    }
}
