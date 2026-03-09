package com.cobblegyms.managers

import com.cobblegyms.config.CobbleGymsConfig
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * FIFO battle queue system with automatic timeout.
 *
 * Each queue is keyed by the defender's UUID (gym leader / E4 member / champion).
 * Players join the queue and are removed after [queueTimeoutSeconds] if they haven't
 * been called to battle.
 */
class BattleManager(
    private val config: CobbleGymsConfig.BattleConfig = CobbleGymsConfig.get().battle
) {

    private val logger = LoggerFactory.getLogger("CobbleGyms/BattleManager")

    /** Type of battle the queue is for. */
    enum class BattleType { GYM, ELITE_FOUR, CHAMPION }

    /**
     * A single entry in a battle queue.
     */
    data class QueueEntry(
        val challengerUuid: UUID,
        val challengerName: String,
        val battleType: BattleType,
        val defenderUuid: UUID,
        val enqueuedAt: Instant = Instant.now()
    )

    /** Map from defender UUID -> ordered queue of challengers. */
    private val queues: ConcurrentHashMap<UUID, LinkedList<QueueEntry>> = ConcurrentHashMap()

    /** Scheduler used for timeout handling. */
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "CobbleGyms-BattleQueue").also { it.isDaemon = true }
    }

    /** Tracks scheduled timeout futures so they can be cancelled on early removal. */
    private val timeoutFutures: ConcurrentHashMap<Pair<UUID, UUID>, ScheduledFuture<*>> = ConcurrentHashMap()

    // -------------------------------------------------------------------------
    // Queue management
    // -------------------------------------------------------------------------

    /**
     * Add a challenger to the queue for a specific defender.
     * @return null if added successfully, or an error message string if rejected.
     */
    fun enqueue(entry: QueueEntry): String? {
        val queue = queues.getOrPut(entry.defenderUuid) { LinkedList() }

        synchronized(queue) {
            // Already queued against this defender?
            if (queue.any { it.challengerUuid == entry.challengerUuid }) {
                return "You are already in the queue for this battle."
            }
            // Max queue size
            if (queue.size >= config.maxQueueSize) {
                return "The queue is full. Please try again later."
            }
            queue.add(entry)
        }

        // Schedule automatic removal after timeout
        val key = Pair(entry.challengerUuid, entry.defenderUuid)
        val future = scheduler.schedule({
            removeFromQueue(entry.challengerUuid, entry.defenderUuid, reason = "timeout")
        }, config.queueTimeoutSeconds, TimeUnit.SECONDS)
        timeoutFutures[key] = future

        logger.info(
            "${entry.challengerName} joined ${entry.battleType} queue for ${entry.defenderUuid} " +
                "(position ${getPosition(entry.challengerUuid, entry.defenderUuid)})"
        )
        return null
    }

    /**
     * Remove a challenger from a defender's queue.
     * @param reason "manual" | "battle_started" | "timeout"
     */
    fun removeFromQueue(challengerUuid: UUID, defenderUuid: UUID, reason: String = "manual"): Boolean {
        val queue = queues[defenderUuid] ?: return false
        val removed: Boolean
        synchronized(queue) {
            removed = queue.removeIf { it.challengerUuid == challengerUuid }
        }
        if (removed) {
            // Cancel pending timeout if removal was not due to timeout itself
            if (reason != "timeout") {
                val key = Pair(challengerUuid, defenderUuid)
                timeoutFutures.remove(key)?.cancel(false)
            }
            logger.info("$challengerUuid removed from queue for $defenderUuid (reason=$reason)")
        }
        return removed
    }

    /**
     * Peek at the next challenger in queue without removing them.
     */
    fun peekNext(defenderUuid: UUID): QueueEntry? {
        val queue = queues[defenderUuid] ?: return null
        return synchronized(queue) { queue.peek() }
    }

    /**
     * Dequeue (and return) the next challenger for a given defender.
     * Call this when the battle is about to start.
     */
    fun dequeueNext(defenderUuid: UUID): QueueEntry? {
        val queue = queues[defenderUuid] ?: return null
        val entry = synchronized(queue) { queue.poll() } ?: return null

        // Cancel timeout future
        val key = Pair(entry.challengerUuid, defenderUuid)
        timeoutFutures.remove(key)?.cancel(false)

        return entry
    }

    /**
     * Get the 1-based position of a challenger in a defender's queue,
     * or null if not queued.
     */
    fun getPosition(challengerUuid: UUID, defenderUuid: UUID): Int? {
        val queue = queues[defenderUuid] ?: return null
        return synchronized(queue) {
            val idx = queue.indexOfFirst { it.challengerUuid == challengerUuid }
            if (idx == -1) null else idx + 1
        }
    }

    /** Returns a snapshot of all entries in the queue for a given defender. */
    fun getQueue(defenderUuid: UUID): List<QueueEntry> {
        val queue = queues[defenderUuid] ?: return emptyList()
        return synchronized(queue) { queue.toList() }
    }

    /** Returns the total number of players waiting across all queues. */
    fun totalWaiting(): Int = queues.values.sumOf { synchronized(it) { it.size } }

    /** Remove a player from ALL queues they are in (e.g. when they disconnect). */
    fun removeFromAllQueues(challengerUuid: UUID) {
        queues.keys.forEach { defenderUuid ->
            removeFromQueue(challengerUuid, defenderUuid, reason = "disconnect")
        }
    }

    /** Graceful shutdown of the scheduler. */
    fun shutdown() {
        scheduler.shutdown()
    }
}
