package com.cobblegyms.discord

import com.cobblegyms.CobbleGyms
import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.models.BattleRecord
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.utils.cache.CacheFlag

class DiscordBotManager {
    private var jda: JDA? = null
    
    fun start() {
        val config = GymConfig.config.discord
        if (!config.enabled || config.token.isBlank()) {
            CobbleGyms.LOGGER.info("Discord bot disabled or no token configured.")
            return
        }
        
        try {
            jda = JDABuilder.createLight(config.token)
                .disableCache(CacheFlag.values().toList())
                .build()
                .awaitReady()
            
            CobbleGyms.LOGGER.info("Discord bot connected successfully!")
        } catch (e: Exception) {
            CobbleGyms.LOGGER.error("Failed to start Discord bot: ${e.message}")
        }
    }
    
    fun shutdown() {
        jda?.shutdown()
        jda = null
    }
    
    fun logBattleRecord(record: BattleRecord) {
        val config = GymConfig.config.discord
        if (!config.enabled || jda == null) return
        
        val channel = getBattleLogChannel() ?: return
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("Battle Record - ${record.battleType}")
            .setColor(if (record.winner == "LEADER") 0xFF0000 else 0x00FF00)
            .addField("Leader", record.leaderName, true)
            .addField("Challenger", record.challengerName, true)
            .addField("Winner", record.winner, true)
            .addField("Turns", record.turns.toString(), true)
            .addField("Season", record.seasonId.toString(), true)
            .apply {
                if (record.challengerTeam != null) {
                    addField("Challenger Team", "```\n${record.challengerTeam.take(1000)}\n```", false)
                }
            }
            .setTimestamp(java.time.Instant.ofEpochSecond(record.battleTime))
            .build()
        
        channel.sendMessageEmbeds(embed).queue(null, { err ->
            CobbleGyms.LOGGER.error("Failed to send battle log to Discord: ${err.message}")
        })
    }
    
    fun logAdminAction(action: String, adminName: String, details: String) {
        val config = GymConfig.config.discord
        if (!config.enabled || jda == null) return
        
        val channel = getAdminChannel() ?: return
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("Admin Action: $action")
            .setColor(0xFFAA00)
            .addField("Admin", adminName, true)
            .addField("Details", details, false)
            .setTimestamp(java.time.Instant.now())
            .build()
        
        channel.sendMessageEmbeds(embed).queue(null, { err ->
            CobbleGyms.LOGGER.error("Failed to send admin log to Discord: ${err.message}")
        })
    }
    
    fun notifyNewChampion(championName: String) {
        val config = GymConfig.config.discord
        if (!config.enabled || jda == null) return
        
        val channel = getBattleLogChannel() ?: return
        
        val embed = net.dv8tion.jda.api.EmbedBuilder()
            .setTitle("★ NEW POKÉMON CHAMPION! ★")
            .setDescription("**$championName** has become the new Pokémon Champion!")
            .setColor(0xFFD700)
            .setTimestamp(java.time.Instant.now())
            .build()
        
        channel.sendMessageEmbeds(embed).queue()
    }
    
    private fun getBattleLogChannel(): TextChannel? {
        val channelId = GymConfig.config.discord.battleLogChannelId
        return if (channelId.isNotBlank()) jda?.getTextChannelById(channelId) else null
    }
    
    private fun getAdminChannel(): TextChannel? {
        val channelId = GymConfig.config.discord.adminChannelId
        return if (channelId.isNotBlank()) jda?.getTextChannelById(channelId) else null
    }
}
