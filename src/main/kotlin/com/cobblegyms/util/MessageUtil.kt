package com.cobblegyms.util

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text

/**
 * Utility class for sending formatted messages to players.
 */
object MessageUtil {
    
    fun sendSuccess(player: ServerPlayerEntity, message: String) {
        player.sendMessage(Text.literal("§a✓ $message"))
    }
    
    fun sendError(player: ServerPlayerEntity, message: String) {
        player.sendMessage(Text.literal("§c✗ $message"))
    }
    
    fun sendWarning(player: ServerPlayerEntity, message: String) {
        player.sendMessage(Text.literal("§e⚠ $message"))
    }
    
    fun sendInfo(player: ServerPlayerEntity, message: String) {
        player.sendMessage(Text.literal("§7ℹ $message"))
    }
    
    fun sendTitle(player: ServerPlayerEntity, title: String) {
        player.sendMessage(Text.literal("§6§l$title"))
    }
    
    fun sendDivider(player: ServerPlayerEntity) {
        player.sendMessage(Text.literal("§8§m──────────────────────────────"))
    }
    
    fun sendClickable(player: ServerPlayerEntity, text: String, command: String, hover: String) {
        val style = Style.EMPTY
            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover)))
        player.sendMessage(Text.literal(text).setStyle(style))
    }
    
    fun formatTime(seconds: Long): String = TimeUtil.formatDuration(seconds)
    
    fun formatWinrate(wins: Int, battles: Int): String {
        if (battles == 0) return "0%"
        return "${String.format("%.1f", wins.toDouble() / battles * 100)}%"
    }
    
    /**
     * Broadcast a message to all online players.
     */
    fun broadcast(server: net.minecraft.server.MinecraftServer, message: String) {
        server.playerManager.playerList.forEach { player ->
            player.sendMessage(Text.literal(message))
        }
    }
    
    /**
     * Broadcast a message to all players with a specific permission level.
     */
    fun broadcastToOps(server: net.minecraft.server.MinecraftServer, message: String) {
        server.playerManager.playerList
            .filter { server.playerManager.isOperator(it.gameProfile) }
            .forEach { it.sendMessage(Text.literal(message)) }
    }
}
