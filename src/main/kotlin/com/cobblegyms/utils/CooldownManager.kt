package com.cobblegyms.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Tracks per-player cooldowns so rewards are only distributed once per period.
 */
object CooldownManager {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** In-memory cooldown map: playerUUID -> Instant the cooldown expires. */
    private val cooldowns: ConcurrentHashMap<UUID, Instant> = ConcurrentHashMap()

    /**
     * Returns true if the player's cooldown has expired (or was never set).
     * @param cooldownHours number of hours the cooldown lasts
     */
    fun isReady(uuid: UUID, cooldownHours: Int): Boolean {
        val expiry = cooldowns[uuid] ?: return true
        return Instant.now().isAfter(expiry)
    }

    /**
     * Set or refresh the cooldown for a player.
     * @param cooldownHours hours from now until the cooldown expires
     */
    fun setCooldown(uuid: UUID, cooldownHours: Int) {
        cooldowns[uuid] = Instant.now().plusSeconds(cooldownHours * 3600L)
    }

    /** Returns the remaining cooldown in minutes, or 0 if ready. */
    fun remainingMinutes(uuid: UUID): Long {
        val expiry = cooldowns[uuid] ?: return 0L
        val remaining = expiry.epochSecond - Instant.now().epochSecond
        return if (remaining <= 0) 0L else remaining / 60
    }

    /** Clear all cooldowns (e.g. at season reset). */
    fun clearAll() = cooldowns.clear()
}
