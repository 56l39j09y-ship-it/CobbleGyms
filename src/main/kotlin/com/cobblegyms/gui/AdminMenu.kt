package com.cobblegyms.gui

import com.cobblegyms.CobbleGyms
import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.GymRepository
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

/**
 * GUI Menu for /gymsadmin command (Moderators).
 * Provides text-based panels for all admin operations.
 */
object AdminMenu {
    
    fun openMain(player: ServerPlayerEntity) {
        player.sendMessage(Text.literal("§6§l╔═══════════════════════════════╗"))
        player.sendMessage(Text.literal("§6§l║    §eGymAdmin Control Panel     §6║"))
        player.sendMessage(Text.literal("§6§l╚═══════════════════════════════╝"))
        player.sendMessage(Text.literal("§e§lGym Management:"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym list §7- View all leaders"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym assign <player> <type> §7- Assign leader"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym remove <player> §7- Remove leader"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym setteam <player> <slot> <pokepaste|url> §7- Set team"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym setformat <player> <singles|doubles> §7- Set format"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym setlocation <player> §7- Set arena at position"))
        player.sendMessage(Text.literal("§7• /gymsadmin gym multiteam <player> <true|false> §7- Enable multi-team"))
        
        player.sendMessage(Text.literal("§e§lElite Four Management:"))
        player.sendMessage(Text.literal("§7• /gymsadmin e4 list §7- View all E4"))
        player.sendMessage(Text.literal("§7• /gymsadmin e4 assign <player> <type1> <type2> §7- Assign E4"))
        player.sendMessage(Text.literal("§7• /gymsadmin e4 remove <player> §7- Remove E4"))
        player.sendMessage(Text.literal("§7• /gymsadmin e4 setteam <player> <slot> <pokepaste|url> §7- Set team"))
        player.sendMessage(Text.literal("§7• /gymsadmin e4 setformat <player> <singles|doubles> §7- Set format"))
        
        player.sendMessage(Text.literal("§e§lChampion Management:"))
        player.sendMessage(Text.literal("§7• /gymsadmin champion assign <player> §7- Assign champion"))
        player.sendMessage(Text.literal("§7• /gymsadmin champion remove §7- Remove champion"))
        player.sendMessage(Text.literal("§7• /gymsadmin champion setteam <slot> <pokepaste|url> §7- Set team"))
        
        player.sendMessage(Text.literal("§e§lSeason & Rules:"))
        player.sendMessage(Text.literal("§7• /gymsadmin season end §7- End current season"))
        player.sendMessage(Text.literal("§7• /gymsadmin rules banpokemon <name> §7- Ban a Pokémon"))
        player.sendMessage(Text.literal("§7• /gymsadmin rules banmove <name> §7- Ban a move"))
        player.sendMessage(Text.literal("§7• /gymsadmin redo <leader> §7- Redo a battle"))
        player.sendMessage(Text.literal("§7• /gymsadmin leaderboard §7- View full rankings"))
    }
    
    fun showGymOverview(player: ServerPlayerEntity) {
        val leaders = GymRepository.getAllGymLeaders()
        val e4List = GymRepository.getAllEliteFour()
        val champion = GymRepository.getChampion()
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        
        player.sendMessage(Text.literal("§6§l=== CobbleGyms Status Overview ==="))
        player.sendMessage(Text.literal("§7Season: §e${season?.number ?: "None"} §7| Remaining: §e${CobbleGyms.seasonManager.formatRemainingTime()}"))
        player.sendMessage(Text.literal("§7Gym Leaders: §f${leaders.size} §7(${leaders.count { it.isOpen }} open)"))
        player.sendMessage(Text.literal("§7Elite Four: §f${e4List.size} §7(${e4List.count { it.isOpen }} open)"))
        player.sendMessage(Text.literal("§7Champion: §f${champion?.playerName ?: "None"} ${if (champion?.isOpen == true) "§a(Open)" else "§c(Closed)"}"))
        
        player.sendMessage(Text.literal("§e§lGym Leaders:"))
        leaders.forEach { leader ->
            val online = CobbleGyms.server.playerManager.getPlayer(leader.playerUuid) != null
            val teamSet = (leader.team1 != null)
            player.sendMessage(Text.literal(
                "§f${leader.gymType.displayName}: §7${leader.playerName} " +
                "[${if (leader.isOpen) "§aOpen§7" else "§cClosed§7"}] " +
                "[${if (online) "§aOnline§7" else "§7Offline"}] " +
                "[${if (teamSet) "§aTeam Set§7" else "§cNo Team§7"}] " +
                "[${leader.battleFormat.name}]"
            ))
        }
        
        if (e4List.isNotEmpty()) {
            player.sendMessage(Text.literal("§e§lElite Four:"))
            e4List.forEach { e4 ->
                val online = CobbleGyms.server.playerManager.getPlayer(e4.playerUuid) != null
                player.sendMessage(Text.literal(
                    "§f${e4.playerName}: §7${e4.type1.displayName}/${e4.type2.displayName} " +
                    "[${if (e4.isOpen) "§aOpen§7" else "§cClosed§7"}] " +
                    "[${if (online) "§aOnline§7" else "§7Offline"}]"
                ))
            }
        }
    }
}
