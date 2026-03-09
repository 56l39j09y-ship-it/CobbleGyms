package com.cobblegyms.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeUtil {
    
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneOffset.UTC)
    
    fun nowSeconds(): Long = System.currentTimeMillis() / 1000
    
    fun formatTimestamp(epochSeconds: Long): String {
        return DATE_FORMAT.format(Instant.ofEpochSecond(epochSeconds))
    }
    
    fun formatDuration(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (days == 0L && hours == 0L && minutes == 0L) append("${secs}s")
        }.trim()
    }
    
    fun getWeekStartSeconds(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000
    }
    
    fun isOnCooldown(lastTime: Long, cooldownHours: Int): Boolean {
        val elapsed = nowSeconds() - lastTime
        return elapsed < cooldownHours * 3600L
    }
    
    fun getCooldownRemaining(lastTime: Long, cooldownHours: Int): Long {
        val elapsed = nowSeconds() - lastTime
        return maxOf(0L, cooldownHours * 3600L - elapsed)
    }
}
