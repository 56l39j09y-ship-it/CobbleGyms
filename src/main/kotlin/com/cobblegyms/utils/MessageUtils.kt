package com.cobblegyms.utils

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.UUID

/**
 * Shared helpers used across commands and managers.
 */
object MessageUtils {

    /** Send a styled info message to a player. */
    fun sendInfo(player: ServerPlayerEntity, message: String) =
        player.sendMessage(Text.literal("§7[§6CobbleGyms§7] §f$message"), false)

    /** Send a styled success message to a player. */
    fun sendSuccess(player: ServerPlayerEntity, message: String) =
        player.sendMessage(Text.literal("§7[§6CobbleGyms§7] §a$message"), false)

    /** Send a styled error message to a player. */
    fun sendError(player: ServerPlayerEntity, message: String) =
        player.sendMessage(Text.literal("§7[§6CobbleGyms§7] §c$message"), false)

    /** Send a styled warning message to a player. */
    fun sendWarning(player: ServerPlayerEntity, message: String) =
        player.sendMessage(Text.literal("§7[§6CobbleGyms§7] §e$message"), false)

    /** Broadcast a message to all online players. */
    fun broadcast(server: net.minecraft.server.MinecraftServer, message: String) {
        val text = Text.literal("§7[§6CobbleGyms§7] §f$message")
        server.playerManager.playerList.forEach { it.sendMessage(text, false) }
    }

    /** Build a decorative header line. */
    fun header(title: String): String = "§6§l═══════ §e$title §6§l═══════"

    /** Format a UUID to a short 8-character string for display. */
    fun shortUuid(uuid: UUID): String = uuid.toString().take(8)

    /** Convert win/loss to a formatted ratio string. */
    fun ratio(wins: Int, losses: Int): String {
        val total = wins + losses
        val rate = if (total == 0) 0.0 else wins.toDouble() / total * 100.0
        return "W:$wins L:$losses (%.1f%%)".format(rate)
    }
}
