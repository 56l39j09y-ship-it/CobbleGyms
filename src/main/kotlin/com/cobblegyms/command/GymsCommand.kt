package com.cobblegyms.command

import com.cobblegyms.CobbleGymsMod
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object GymsCommand {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("gyms")
                .executes { ctx -> showMenu(ctx) }
                .then(literal("status").executes { ctx -> showStatus(ctx) })
                .then(literal("leaders").executes { ctx -> showLeaders(ctx) })
                .then(literal("elite4").executes { ctx -> showEliteFour(ctx) })
                .then(literal("champion").executes { ctx -> showChampion(ctx) })
                .then(literal("season").executes { ctx -> showSeason(ctx) })
                .then(literal("rules").executes { ctx -> showRules(ctx) })
                .then(literal("queue").executes { ctx -> showMyQueue(ctx) })
        )
    }

    private fun showMenu(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        source.sendFeedback({ Text.literal("§6=== CobbleGyms ===") }, false)
        source.sendFeedback({ Text.literal("§e/gyms status §7- Show gym system status") }, false)
        source.sendFeedback({ Text.literal("§e/gyms leaders §7- Show all gym leaders") }, false)
        source.sendFeedback({ Text.literal("§e/gyms elite4 §7- Show Elite Four members") }, false)
        source.sendFeedback({ Text.literal("§e/gyms champion §7- Show current champion") }, false)
        source.sendFeedback({ Text.literal("§e/gyms season §7- Show current season info") }, false)
        source.sendFeedback({ Text.literal("§e/gyms rules §7- Show battle rules and bans") }, false)
        source.sendFeedback({ Text.literal("§e/gyms queue §7- Show your queue position") }, false)
        source.sendFeedback({ Text.literal("§e/challenge <type> <player> §7- Challenge a gym leader/E4/champion") }, false)
        return 1
    }

    private fun showStatus(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val leaders = CobbleGymsMod.gymLeaderManager.getAllGymLeaders()
        val e4Members = CobbleGymsMod.eliteFourManager.getAllMembers()
        val champion = CobbleGymsMod.championManager.getChampion()
        val season = CobbleGymsMod.seasonManager.getCurrentSeason()

        source.sendFeedback({ Text.literal("§6=== CobbleGyms Status ===") }, false)
        source.sendFeedback({ Text.literal("§eGym Leaders: §f${leaders.size}") }, false)
        source.sendFeedback({ Text.literal("§eElite Four: §f${e4Members.size}/4") }, false)
        source.sendFeedback({ Text.literal("§eChampion: §f${champion?.playerName ?: "§7None"}") }, false)
        if (season != null) {
            source.sendFeedback({ Text.literal("§eSeason #${season.id}: §f${CobbleGymsMod.seasonManager.getDaysRemaining()} days remaining") }, false)
        }
        return 1
    }

    private fun showLeaders(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val leaders = CobbleGymsMod.gymLeaderManager.getAllGymLeaders()
        source.sendFeedback({ Text.literal("§6=== Gym Leaders ===") }, false)
        if (leaders.isEmpty()) {
            source.sendFeedback({ Text.literal("§7No gym leaders set.") }, false)
        } else {
            leaders.forEach { leader ->
                source.sendFeedback({
                    Text.literal("§e${leader.playerName} §7(${leader.typeSpecialty}) §fW:${leader.wins} L:${leader.losses}")
                }, false)
            }
        }
        return 1
    }

    private fun showEliteFour(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val members = CobbleGymsMod.eliteFourManager.getAllMembers()
        source.sendFeedback({ Text.literal("§6=== Elite Four ===") }, false)
        if (members.isEmpty()) {
            source.sendFeedback({ Text.literal("§7No Elite Four members set.") }, false)
        } else {
            members.forEach { member ->
                source.sendFeedback({
                    Text.literal("§e#${member.position} ${member.playerName} §7(${member.typeSpecialty}) §fW:${member.wins} L:${member.losses}")
                }, false)
            }
        }
        return 1
    }

    private fun showChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val champion = CobbleGymsMod.championManager.getChampion()
        source.sendFeedback({ Text.literal("§6=== Champion ===") }, false)
        if (champion == null) {
            source.sendFeedback({ Text.literal("§7No champion set.") }, false)
        } else {
            source.sendFeedback({ Text.literal("§e${champion.playerName}") }, false)
            source.sendFeedback({ Text.literal("§7Record: §fW:${champion.wins} L:${champion.losses} (${String.format("%.1f", champion.winRate)}%)") }, false)
        }
        return 1
    }

    private fun showSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val season = CobbleGymsMod.seasonManager.getCurrentSeason()
        source.sendFeedback({ Text.literal("§6=== Season Info ===") }, false)
        if (season == null) {
            source.sendFeedback({ Text.literal("§7No active season.") }, false)
        } else {
            source.sendFeedback({ Text.literal("§eSeason #${season.id}") }, false)
            source.sendFeedback({ Text.literal("§7Days remaining: §f${CobbleGymsMod.seasonManager.getDaysRemaining()}") }, false)
        }
        return 1
    }

    private fun showRules(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        source.sendFeedback({ Text.literal("§6=== Battle Rules ===") }, false)
        source.sendFeedback({ Text.literal("§eFormat: §fSingles (6v6)") }, false)
        source.sendFeedback({ Text.literal("§eBanned Pokémon:") }, false)
        val banned = com.cobblegyms.system.validation.SmogonValidator.getBannedList()
        source.sendFeedback({ Text.literal("§7${banned.joinToString(", ")}") }, false)
        return 1
    }

    private fun showMyQueue(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player ?: return 0
        val playerUuid = player.uuid
        val queueSizes = com.cobblegyms.system.battle.BattleQueueManager.getAllActiveQueueSizes()
        source.sendFeedback({ Text.literal("§6=== Your Queue Status ===") }, false)
        var foundInQueue = false
        for ((key, size) in queueSizes) {
            val parts = key.split(":")
            if (parts.size >= 2) {
                val targetType = parts[0]
                val targetUuid = java.util.UUID.fromString(parts[1])
                val pos = com.cobblegyms.system.battle.BattleQueueManager.getQueuePosition(playerUuid, targetUuid, targetType)
                if (pos != null) {
                    source.sendFeedback({ Text.literal("§e$targetType §7queue position: §f$pos/$size") }, false)
                    foundInQueue = true
                }
            }
        }
        if (!foundInQueue) {
            source.sendFeedback({ Text.literal("§7You are not in any queues.") }, false)
        }
        return 1
    }
}
