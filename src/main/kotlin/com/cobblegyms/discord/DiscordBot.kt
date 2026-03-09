package com.cobblegyms.discord

import com.cobblegyms.config.CobbleGymsConfig
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.Instant

/**
 * Optional Discord bot integration for CobbleGyms.
 *
 * When enabled in config (discord.enabled=true), this class creates a JDA
 * client and sends rich embeds to the configured channel for significant
 * events: gym wins, E4 wins, champion changes, and season transitions.
 */
class DiscordBot(private val config: CobbleGymsConfig.DiscordConfig) {

    private val logger = LoggerFactory.getLogger("CobbleGyms/Discord")
    private var jda: JDA? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun start() {
        if (!config.enabled) {
            logger.info("Discord integration disabled – skipping bot startup")
            return
        }
        if (config.botToken.isBlank()) {
            logger.warn("Discord bot token is empty – skipping bot startup")
            return
        }

        try {
            jda = JDABuilder.createLight(config.botToken, GatewayIntent.GUILD_MESSAGES)
                .build()
                .awaitReady()
            logger.info("Discord bot connected successfully")
        } catch (e: Exception) {
            logger.error("Failed to start Discord bot: ${e.message}", e)
        }
    }

    fun stop() {
        jda?.shutdown()
        jda = null
        logger.info("Discord bot shut down")
    }

    // -------------------------------------------------------------------------
    // Notification helpers
    // -------------------------------------------------------------------------

    /** Notify when a challenger defeats a gym leader and earns a badge. */
    fun notifyGymWin(challengerName: String, gymType: String, badgeName: String) {
        if (!config.notifyGymWins) return
        val embed = EmbedBuilder()
            .setTitle("⚔ Gym Badge Earned!")
            .setColor(Color(255, 215, 0)) // gold
            .addField("Trainer", challengerName, true)
            .addField("Gym Type", gymType, true)
            .addField("Badge", badgeName, true)
            .setTimestamp(Instant.now())
            .build()
        sendEmbed(embed)
    }

    /** Notify when a challenger defeats an Elite Four member. */
    fun notifyE4Win(challengerName: String, memberName: String, position: Int) {
        if (!config.notifyE4Wins) return
        val embed = EmbedBuilder()
            .setTitle("★ Elite Four Defeated!")
            .setColor(Color(128, 0, 128)) // purple
            .addField("Trainer", challengerName, true)
            .addField("Elite Four", "$memberName (#$position)", true)
            .setTimestamp(Instant.now())
            .build()
        sendEmbed(embed)
    }

    /** Notify when a new champion is crowned. */
    fun notifyNewChampion(newChampionName: String, defeatedChampionName: String?) {
        if (!config.notifyChampion) return
        val desc = if (defeatedChampionName != null) {
            "**$newChampionName** defeated **$defeatedChampionName** and became the new Champion!"
        } else {
            "**$newChampionName** has claimed the Champion title!"
        }
        val embed = EmbedBuilder()
            .setTitle("👑 New Champion!")
            .setDescription(desc)
            .setColor(Color(255, 140, 0)) // orange
            .setTimestamp(Instant.now())
            .build()
        sendEmbed(embed)
    }

    /** Notify when a season starts. */
    fun notifySeasonStart(seasonNumber: Int) {
        if (!config.notifySeasonEnd) return
        val embed = EmbedBuilder()
            .setTitle("⚡ Season $seasonNumber Has Begun!")
            .setDescription("A new competitive season is underway. Battle gym leaders, defeat the Elite Four, and claim the Champion title!")
            .setColor(Color(0, 150, 255)) // blue
            .setTimestamp(Instant.now())
            .build()
        sendEmbed(embed)
    }

    /** Notify when a season ends (with optional top-3 leaderboard). */
    fun notifySeasonEnd(seasonNumber: Int, topPlayers: List<Pair<String, Int>>) {
        if (!config.notifySeasonEnd) return
        val embed = EmbedBuilder()
            .setTitle("🏁 Season $seasonNumber Has Ended!")
            .setColor(Color(200, 50, 50)) // red
            .setTimestamp(Instant.now())

        if (topPlayers.isNotEmpty()) {
            val podium = topPlayers.take(3).mapIndexed { idx, (name, badges) ->
                "${idx + 1}. **$name** – $badges badge(s)"
            }.joinToString("\n")
            embed.addField("🏆 Top Trainers", podium, false)
        }

        sendEmbed(embed.build())
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private fun sendEmbed(embed: net.dv8tion.jda.api.entities.MessageEmbed) {
        val channelId = config.channelId
        if (channelId.isBlank()) {
            logger.warn("Discord channel ID not configured – cannot send embed")
            return
        }
        try {
            val channel: TextChannel = jda?.getTextChannelById(channelId)
                ?: run {
                    logger.warn("Discord channel $channelId not found")
                    return
                }
            channel.sendMessageEmbeds(embed).queue(
                { logger.debug("Discord embed sent to #${channel.name}") },
                { err -> logger.error("Failed to send Discord embed: ${err.message}") }
            )
        } catch (e: Exception) {
            logger.error("Exception sending Discord embed: ${e.message}", e)
        }
    }
}
