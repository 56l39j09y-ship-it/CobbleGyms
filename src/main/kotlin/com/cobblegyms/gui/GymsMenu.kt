package com.cobblegyms.gui

import com.cobblegyms.CobbleGyms
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.PokemonType
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

/**
 * GUI Menu system for /gyms command.
 * 
 * Uses chest-based GUI menus for interactive player interaction.
 * 
 * Note: Full GUI implementation requires either a GUI library mod
 * or manual screen handler implementation. This provides the structure
 * and data layer for GUI integration.
 */
object GymsMenu {
    
    fun openMain(player: ServerPlayerEntity) {
        // Display text-based menu (full GUI would use screen handlers)
        player.sendMessage(Text.literal("§6§l╔═══════════════════════╗"))
        player.sendMessage(Text.literal("§6§l║  §eCobbleGyms Main Menu  §6║"))
        player.sendMessage(Text.literal("§6§l╚═══════════════════════╝"))
        player.sendMessage(Text.literal("§e[1] §f🏅 View Badges & Progress"))
        player.sendMessage(Text.literal("§e[2] §f⚔ Challenge Gym Leader"))
        player.sendMessage(Text.literal("§e[3] §f🏆 Challenge Elite Four"))
        player.sendMessage(Text.literal("§e[4] §f👑 Challenge Champion"))
        player.sendMessage(Text.literal("§e[5] §f✔ Validate My Team"))
        player.sendMessage(Text.literal("§e[6] §f📜 View Rules"))
        player.sendMessage(Text.literal("§e[7] §f⏱ Season Info"))
        player.sendMessage(Text.literal("§e[8] §f📊 Leaderboard"))
        player.sendMessage(Text.literal("§7Use the corresponding /gyms commands to navigate:"))
        player.sendMessage(Text.literal("§7• /gyms badges  • /gyms challenge  • /gyms validate"))
        player.sendMessage(Text.literal("§7• /gyms rules   • /gyms season     • /gyms leaderboard"))
    }
    
    fun openBadgeMenu(player: ServerPlayerEntity) {
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        if (season == null) {
            player.sendMessage(Text.literal("§cNo active season."))
            return
        }
        
        val badges = GymRepository.getPlayerBadges(player.uuid, season.id)
        val allLeaders = GymRepository.getAllGymLeaders()
        val e4Victories = GymRepository.getPlayerE4Victories(player.uuid, season.id)
        val allE4 = GymRepository.getAllEliteFour()
        
        player.sendMessage(Text.literal("§6§l=== Season ${season.number} Badge Case ==="))
        
        if (allLeaders.isEmpty()) {
            player.sendMessage(Text.literal("§7No gyms have been set up yet."))
        } else {
            val badgeRow = allLeaders.joinToString(" ") { leader ->
                val hasBadge = badges.any { it.gymType == leader.gymType }
                if (hasBadge) "§a[${leader.gymType.displayName[0]}]" else "§8[${leader.gymType.displayName[0]}]"
            }
            player.sendMessage(Text.literal("Gym Badges: $badgeRow"))
            
            allLeaders.forEach { leader ->
                val hasBadge = badges.any { it.gymType == leader.gymType }
                val icon = if (hasBadge) "§a✓ " else "§c✗ "
                player.sendMessage(Text.literal("$icon${leader.gymType.color}${leader.gymType.displayName} §7- ${leader.playerName}"))
            }
        }
        
        if (allE4.isNotEmpty()) {
            player.sendMessage(Text.literal("§6§lElite Four:"))
            allE4.forEach { e4 ->
                val beaten = e4Victories.any { it.e4Uuid == e4.playerUuid }
                player.sendMessage(Text.literal("${if (beaten) "§a✓" else "§c✗"} §f${e4.playerName} §7(${e4.type1.displayName}/${e4.type2.displayName})"))
            }
        }
        
        val champion = GymRepository.getChampion()
        if (champion != null) {
            val beaten = GymRepository.hasBeatenAllE4(player.uuid, season.id)
            player.sendMessage(Text.literal("§6§lChampion: §f${champion.playerName} ${if (beaten) "§a✓" else "§c✗"}"))
        }
        
        // Show progress stats
        val badgesEarned = badges.size
        val totalBadges = allLeaders.size
        val e4Beaten = e4Victories.size
        val totalE4 = allE4.size
        player.sendMessage(Text.literal("§7Progress: §f$badgesEarned/$totalBadges §7Badges | §f$e4Beaten/$totalE4 §7E4 defeated"))
    }
    
    fun openGymChallengeMenu(player: ServerPlayerEntity) {
        val leaders = GymRepository.getAllGymLeaders()
        
        player.sendMessage(Text.literal("§6§l=== Choose a Gym to Challenge ==="))
        
        if (leaders.isEmpty()) {
            player.sendMessage(Text.literal("§7No Gym Leaders available."))
            return
        }
        
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        val playerBadges = if (season != null) GymRepository.getPlayerBadges(player.uuid, season.id) else emptyList()
        
        leaders.forEach { leader ->
            val hasBadge = playerBadges.any { it.gymType == leader.gymType }
            val status = if (leader.isOpen) "§aOpen" else "§cClosed"
            val badge = if (hasBadge) " §a✓" else ""
            val online = CobbleGyms.server.playerManager.getPlayer(leader.playerUuid) != null
            val onlineStatus = if (online) "§a●" else "§7●"
            
            player.sendMessage(Text.literal(
                "$onlineStatus ${leader.gymType.color}${leader.gymType.displayName} §7(${leader.playerName}) [$status] [${leader.battleFormat.name}]$badge"
            ))
            if (leader.isOpen) {
                player.sendMessage(Text.literal("  §7➜ /gyms challenge gym ${leader.gymType.name.lowercase()}"))
            }
        }
    }
    
    fun openE4ChallengeMenu(player: ServerPlayerEntity) {
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        
        if (season != null && !GymRepository.hasAllBadges(player.uuid, season.id)) {
            player.sendMessage(Text.literal("§c§lYou need all Gym Badges to challenge the Elite Four!"))
            player.sendMessage(Text.literal("§7Get all badges first before attempting the Elite Four."))
            return
        }
        
        val e4List = GymRepository.getAllEliteFour()
        
        if (e4List.isEmpty()) {
            player.sendMessage(Text.literal("§7No Elite Four members configured."))
            return
        }
        
        val victories = if (season != null) GymRepository.getPlayerE4Victories(player.uuid, season.id) else emptyList()
        
        player.sendMessage(Text.literal("§6§l=== Elite Four Challenge ==="))
        e4List.forEach { e4 ->
            val beaten = victories.any { it.e4Uuid == e4.playerUuid }
            val status = if (e4.isOpen) "§aOpen" else "§cClosed"
            val beatenStr = if (beaten) " §a(Defeated)" else ""
            val online = CobbleGyms.server.playerManager.getPlayer(e4.playerUuid) != null
            val onlineStatus = if (online) "§a●" else "§7●"
            
            player.sendMessage(Text.literal(
                "$onlineStatus §f${e4.playerName} §7[${e4.type1.displayName}/${e4.type2.displayName}] [$status] [${e4.battleFormat.name}]$beatenStr"
            ))
            if (e4.isOpen) {
                player.sendMessage(Text.literal("  §7➜ /gyms challenge e4 ${e4.playerName}"))
            }
        }
    }
    
    fun openChampionChallengeMenu(player: ServerPlayerEntity) {
        val champion = GymRepository.getChampion()
        
        if (champion == null) {
            player.sendMessage(Text.literal("§7No Champion has been designated yet."))
            return
        }
        
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        
        if (season != null && !GymRepository.hasBeatenAllE4(player.uuid, season.id)) {
            player.sendMessage(Text.literal("§c§lYou must defeat all Elite Four members first!"))
            return
        }
        
        val status = if (champion.isOpen) "§aOpen" else "§cClosed"
        val online = CobbleGyms.server.playerManager.getPlayer(champion.playerUuid) != null
        
        player.sendMessage(Text.literal("§6§l=== Pokémon Champion ==="))
        player.sendMessage(Text.literal("§f${champion.playerName} §7[$status] [${champion.battleFormat.name}]"))
        
        if (!online) {
            player.sendMessage(Text.literal("§7The Champion is not online right now."))
        } else if (!champion.isOpen) {
            player.sendMessage(Text.literal("§7The Champion is not available right now."))
        } else {
            player.sendMessage(Text.literal("§7➜ /gyms challenge champion"))
        }
    }
}
