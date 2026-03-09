package com.cobblegyms.system.battle

import com.cobblegyms.config.CobbleGymsConfig
import java.util.UUID
import java.util.LinkedList

data class QueueEntry(
    val playerUuid: UUID,
    val playerName: String,
    val targetUuid: UUID,
    val targetType: String,
    val queueTime: Long = System.currentTimeMillis(),
    var battleStartTime: Long? = null
)

object BattleQueueManager {
    private val queues = mutableMapOf<String, LinkedList<QueueEntry>>()
    private val activeBattles = mutableMapOf<String, QueueEntry>()

    fun initialize() {
        queues.clear()
        activeBattles.clear()
    }

    fun addToQueue(entry: QueueEntry): Boolean {
        val key = "${entry.targetType}:${entry.targetUuid}"
        val queue = queues.computeIfAbsent(key) { LinkedList() }
        if (queue.any { it.playerUuid == entry.playerUuid }) return false
        queue.add(entry)
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

    fun removeAllQueuesForPlayer(playerUuid: UUID) {
        queues.forEach { (_, queue) ->
            queue.removeIf { it.playerUuid == playerUuid }
        }
    }

    fun getQueueForTarget(targetUuid: UUID, targetType: String): List<QueueEntry> {
        val key = "$targetType:$targetUuid"
        cleanupExpiredEntries(key)
        return queues[key]?.toList() ?: emptyList()
    }

    fun peekNext(targetUuid: UUID, targetType: String): QueueEntry? {
        val key = "$targetType:$targetUuid"
        cleanupExpiredEntries(key)
        return queues[key]?.peekFirst()
    }

    fun popNext(targetUuid: UUID, targetType: String): QueueEntry? {
        val key = "$targetType:$targetUuid"
        cleanupExpiredEntries(key)
        return queues[key]?.pollFirst()
    }

    fun startBattle(entry: QueueEntry): Boolean {
        val key = "${entry.targetUuid}:${entry.playerUuid}"
        if (activeBattles.containsKey(key)) return false
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

    fun isInActiveBattle(playerUuid: UUID): Boolean {
        return activeBattles.values.any { it.playerUuid == playerUuid || it.targetUuid == playerUuid }
    }

    fun getAllActiveQueueSizes(): Map<String, Int> = queues.mapValues { it.value.size }

    fun clearQueue(targetUuid: UUID, targetType: String) {
        val key = "$targetType:$targetUuid"
        queues.remove(key)
    }

    fun getQueuePosition(playerUuid: UUID, targetUuid: UUID, targetType: String): Int? {
        val key = "$targetType:$targetUuid"
        val queue = queues[key] ?: return null
        val index = queue.indexOfFirst { it.playerUuid == playerUuid }
        return if (index >= 0) index + 1 else null
    }

    private fun cleanupExpiredEntries(key: String) {
        val now = System.currentTimeMillis()
        queues[key]?.removeIf { now - it.queueTime > CobbleGymsConfig.queueTimeoutMs }
    }

    fun cleanupAllExpiredEntries() {
        val now = System.currentTimeMillis()
        queues.forEach { (_, queue) ->
            queue.removeIf { now - it.queueTime > CobbleGymsConfig.queueTimeoutMs }
        }
        queues.entries.removeIf { it.value.isEmpty() }
    }
}
