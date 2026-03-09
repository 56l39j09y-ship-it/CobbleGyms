package com.cobblegyms.system.battle

import java.util.*
import kotlin.collections.LinkedHashMap

data class QueueEntry(
    val playerUuid: UUID,
    val playerName: String,
    val targetUuid: UUID,
    val targetType: String,
    val queueTime: Long = System.currentTimeMillis(),
    var battleStartTime: Long? = null
)

object BattleQueueManager {
    private val queues = LinkedHashMap<String, MutableList<QueueEntry>>()
    private val activeBattles = mutableMapOf<String, QueueEntry>()
    private val queueTimeout = 5 * 60 * 1000L // 5 minutes

    fun initialize() {
        // Initialize queues
    }

    fun addToQueue(entry: QueueEntry): Boolean {
        val key = "${entry.targetType}:${entry.targetUuid}"
        queues.computeIfAbsent(key) { mutableListOf() }
        queues[key]!!.add(entry)
        return true
    }

    fun removeFromQueue(playerUuid: UUID, targetUuid: UUID): Boolean {
        for ((_, queue) in queues) {
            if (queue.removeIf { it.playerUuid == playerUuid && it.targetUuid == targetUuid }) {
                return true
            }
        }
        return false
    }

    fun getQueueForTarget(targetUuid: UUID, targetType: String): List<QueueEntry> {
        val key = "$targetType:$targetUuid"
        return queues[key]?.toList() ?: emptyList()
    }

    fun peekNext(targetUuid: UUID, targetType: String): QueueEntry? {
        val key = "$targetType:$targetUuid"
        val queue = queues[key] ?: return null

        // Remove timed out entries
        queue.removeIf { System.currentTimeMillis() - it.queueTime > queueTimeout }

        return if (queue.isNotEmpty()) queue.first() else null
    }

    fun popNext(targetUuid: UUID, targetType: String): QueueEntry? {
        val key = "$targetType:$targetUuid"
        val queue = queues[key] ?: return null

        // Remove timed out entries
        queue.removeIf { System.currentTimeMillis() - it.queueTime > queueTimeout }

        return if (queue.isNotEmpty()) queue.removeAt(0) else null
    }

    fun startBattle(entry: QueueEntry): Boolean {
        val key = "${entry.targetUuid}:${entry.playerUuid}"
        entry.battleStartTime = System.currentTimeMillis()
        activeBattles[key] = entry
        return true
    }

    fun endBattle(playerUuid: UUID, targetUuid: UUID): Boolean {
        val key = "$targetUuid:$playerUuid"
        return activeBattles.remove(key) != null
    }

    fun getActiveBattle(playerUuid: UUID, targetUuid: UUID): QueueEntry? {
        val key = "$targetUuid:$playerUuid"
        return activeBattles[key]
    }

    fun getAllActiveQueueSizes(): Map<String, Int> {
        return queues.mapValues { it.value.size }
    }

    fun clearQueue(targetUuid: UUID, targetType: String) {
        val key = "$targetType:$targetUuid"
        queues.remove(key)
    }

    fun getQueuePosition(playerUuid: UUID, targetUuid: UUID, targetType: String): Int? {
        val key = "$targetType:$targetUuid"
        val queue = queues[key] ?: return null
        return queue.indexOfFirst { it.playerUuid == playerUuid }.takeIf { it >= 0 }
    }

    fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        queues.forEach { (_, queue) ->
            queue.removeIf { now - it.queueTime > queueTimeout }
        }
        activeBattles.forEach { (key, entry) ->
            if (now - (entry.battleStartTime ?: now) > 60 * 60 * 1000L) { // 1 hour
                activeBattles.remove(key)
            }
        }
    }
}