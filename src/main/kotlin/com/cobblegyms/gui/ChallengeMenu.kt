package com.cobblegyms.gui

import com.cobblegyms.CobbleGyms
import com.cobblegyms.data.GymRepository
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

/**
 * GUI Menu for /challenge command (Gym Leaders, Elite Four, Champion).
 */
object ChallengeMenu {
    
    fun open(player: ServerPlayerEntity) {
        val uuid = player.uuid
        val isLeader = CobbleGyms.gymManager.isGymLeader(uuid)
        val isE4 = CobbleGyms.gymManager.isEliteFour(uuid)
        val isChampion = CobbleGyms.gymManager.isChampion(uuid)
        
        if (!isLeader && !isE4 && !isChampion) {
            player.sendMessage(Text.literal("§cYou don't have a role."))
            return
        }
        
        val role = when {
            isChampion -> "§6Champion"
            isE4 -> {
                val e4 = GymRepository.getEliteFour(uuid)
                "§dElite Four (${e4?.type1?.displayName}/${e4?.type2?.displayName})"
            }
            else -> {
                val leader = GymRepository.getGymLeader(uuid)
                "${leader?.gymType?.color}${leader?.gymType?.displayName} Gym Leader"
            }
        }
        
        val isOpen = when {
            isChampion -> GymRepository.getChampion()?.isOpen ?: false
            isE4 -> GymRepository.getEliteFour(uuid)?.isOpen ?: false
            else -> GymRepository.getGymLeader(uuid)?.isOpen ?: false
        }
        
        val statusStr = if (isOpen) "§aOPEN" else "§cCLOSED"
        val queue = GymRepository.getQueue(uuid)
        val activeBattle = CobbleGyms.battleManager.getActiveBattleForLeader(uuid)
        
        player.sendMessage(Text.literal("§6§l╔══════════════════════════╗"))
        player.sendMessage(Text.literal("§6§l║   §eChallenge Panel        §6║"))
        player.sendMessage(Text.literal("§6§l╚══════════════════════════╝"))
        player.sendMessage(Text.literal("§7Role: $role"))
        player.sendMessage(Text.literal("§7Status: $statusStr §7| Queue: §f${queue.size} §7challengers"))
        
        if (activeBattle != null) {
            val challenger = CobbleGyms.server.playerManager.getPlayer(activeBattle.challengerUuid)
            player.sendMessage(Text.literal("§c§lACTIVE BATTLE: §7vs §e${challenger?.name?.string ?: "Unknown"} §7(Turn ${activeBattle.turns})"))
        }
        
        player.sendMessage(Text.literal(""))
        player.sendMessage(Text.literal("§e§lAvailable Actions:"))
        
        if (isOpen) {
            player.sendMessage(Text.literal("§c• /challenge close §7- Close your gym"))
        } else {
            player.sendMessage(Text.literal("§a• /challenge open §7- Open your gym"))
        }
        
        player.sendMessage(Text.literal("§e• /challenge equip [slot] §7- Equip gym team"))
        player.sendMessage(Text.literal("§e• /challenge unequip §7- Return to personal team"))
        
        if (queue.isNotEmpty()) {
            player.sendMessage(Text.literal("§a• /challenge start <player> §7- Start battle"))
        }
        
        player.sendMessage(Text.literal("§c• /challenge cancel all §7- Cancel all battles"))
        player.sendMessage(Text.literal("§c• /challenge cancel current §7- Cancel current battle"))
        player.sendMessage(Text.literal("§7• /challenge ban <player> <hours> §7- Ban from gym"))
        player.sendMessage(Text.literal("§7• /challenge records §7- View battle records"))
        player.sendMessage(Text.literal("§7• /challenge stats §7- View weekly stats"))
        player.sendMessage(Text.literal("§7• /challenge queue §7- View challenger queue"))
        
        if (queue.isNotEmpty()) {
            player.sendMessage(Text.literal(""))
            player.sendMessage(Text.literal("§6§lNext Challenger: §e${queue.first().challengerName}"))
        }
    }
}
