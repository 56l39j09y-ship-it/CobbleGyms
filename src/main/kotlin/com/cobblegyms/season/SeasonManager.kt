package com.cobblegyms.season

import com.cobblegyms.CobbleGyms
import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.SeasonData
import com.cobblegyms.util.MessageUtil
import com.cobblegyms.util.TimeUtil
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class SeasonManager {
    private var seasonTimer: Timer? = null
    
    fun onServerStart() {
        val current = GymRepository.getCurrentSeason()
        if (current == null) {
            // Start season 1
            val season = GymRepository.createSeason(1, System.currentTimeMillis() / 1000)
            CobbleGyms.LOGGER.info("Season 1 started!")
            broadcastMessage("§6§l★ Season 1 has begun! §7Good luck to all trainers! ★")
        } else {
            checkSeasonEnd(current)
        }
        
        // Schedule periodic season checks
        scheduleSeasonCheck()
    }
    
    private fun scheduleSeasonCheck() {
        seasonTimer?.cancel()
        seasonTimer = Timer("SeasonTimer", true)
        seasonTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    val current = GymRepository.getCurrentSeason() ?: return
                    checkSeasonEnd(current)
                } catch (e: Exception) {
                    CobbleGyms.LOGGER.error("Error in season check: ${e.message}")
                }
            }
        }, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1))
    }
    
    private fun checkSeasonEnd(season: SeasonData) {
        val config = GymConfig.config.season
        val seasonDurationSeconds = config.durationDays * 24 * 3600L
        val elapsed = System.currentTimeMillis() / 1000 - season.startTime
        
        if (config.autoReset && elapsed >= seasonDurationSeconds) {
            endSeason(season)
        }
    }
    
    fun endSeason(season: SeasonData) {
        CobbleGyms.LOGGER.info("Ending season ${season.number}")
        
        // End current season
        GymRepository.endSeason(season.id, System.currentTimeMillis() / 1000)
        
        // Start new season
        val newSeason = GymRepository.createSeason(season.number + 1, System.currentTimeMillis() / 1000)
        
        broadcastMessage("§6§l★ Season ${season.number} has ended! Season ${newSeason.number} begins now! ★")
        broadcastMessage("§7All badges and E4/Champion victories have been reset. Keep fighting!")
        
        // Trigger rewards
        CobbleGyms.rewardManager.processSeasonEndRewards(season.id)
    }
    
    fun getCurrentSeason(): SeasonData? = GymRepository.getCurrentSeason()
    
    fun getAllSeasons(): List<SeasonData> = GymRepository.getAllSeasons()
    
    fun getRemainingTime(): Long {
        val season = GymRepository.getCurrentSeason() ?: return 0L
        val config = GymConfig.config.season
        val seasonDurationSeconds = config.durationDays * 24 * 3600L
        val elapsed = System.currentTimeMillis() / 1000 - season.startTime
        return maxOf(0L, seasonDurationSeconds - elapsed)
    }
    
    fun formatRemainingTime(): String = TimeUtil.formatDuration(getRemainingTime())
    
    private fun broadcastMessage(message: String) {
        try {
            MessageUtil.broadcast(CobbleGyms.server, message)
        } catch (e: Exception) {
            CobbleGyms.LOGGER.warn("Could not broadcast message: ${e.message}")
        }
    }
}
