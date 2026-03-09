package com.cobblegyms.commands

import com.cobblegyms.CobbleGyms
import com.cobblegyms.battle.BattleManager
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.BattleFormat
import com.cobblegyms.gui.ChallengeMenu
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object ChallengeCommand {
    
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("challenge")
                .executes { ctx -> openChallengeMenu(ctx) }
                .then(CommandManager.literal("open")
                    .executes { ctx -> toggleOpen(ctx, true) })
                .then(CommandManager.literal("close")
                    .executes { ctx -> toggleOpen(ctx, false) })
                .then(CommandManager.literal("equip")
                    .executes { ctx -> equipTeam(ctx) }
                    .then(CommandManager.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 3))
                        .executes { ctx -> equipTeamSlot(ctx, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot")) }))
                .then(CommandManager.literal("unequip")
                    .executes { ctx -> unequipTeam(ctx) })
                .then(CommandManager.literal("start")
                    .then(CommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests { _, builder ->
                            // Suggest players in the leader's queue
                            builder.buildFuture()
                        }
                        .executes { ctx -> startBattle(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "player")) }))
                .then(CommandManager.literal("cancel")
                    .then(CommandManager.literal("all")
                        .executes { ctx -> cancelAll(ctx) })
                    .then(CommandManager.literal("current")
                        .executes { ctx -> cancelCurrent(ctx) }))
                .then(CommandManager.literal("ban")
                    .then(CommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(CommandManager.argument("hours", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 720))
                            .executes { ctx -> banPlayer(ctx,
                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "player"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "hours")) })))
                .then(CommandManager.literal("unban")
                    .then(CommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes { ctx -> unbanPlayer(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "player")) }))
                .then(CommandManager.literal("records")
                    .executes { ctx -> showRecords(ctx) })
                .then(CommandManager.literal("queue")
                    .executes { ctx -> showMyQueue(ctx) })
                .then(CommandManager.literal("stats")
                    .executes { ctx -> showStats(ctx) })
        )
    }
    
    private fun openChallengeMenu(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(player.uuid)) {
            player.sendMessage(Text.literal("§cYou must be a Gym Leader, Elite Four, or Champion to use this command."))
            return 0
        }
        ChallengeMenu.open(player)
        return 1
    }
    
    private fun toggleOpen(ctx: CommandContext<ServerCommandSource>, open: Boolean): Int {
        val player = ctx.source.player ?: return 0
        val uuid = player.uuid
        
        val success = when {
            CobbleGyms.gymManager.isGymLeader(uuid) -> {
                if (open) CobbleGyms.gymManager.openGym(uuid) else CobbleGyms.gymManager.closeGym(uuid)
            }
            CobbleGyms.gymManager.isEliteFour(uuid) -> {
                if (open) CobbleGyms.gymManager.openE4(uuid) else CobbleGyms.gymManager.closeE4(uuid)
            }
            CobbleGyms.gymManager.isChampion(uuid) -> {
                if (open) CobbleGyms.gymManager.openChampion(uuid) else CobbleGyms.gymManager.closeChampion(uuid)
            }
            else -> {
                player.sendMessage(Text.literal("§cYou don't have a Gym/E4/Champion role."))
                return 0
            }
        }
        
        if (success) {
            val status = if (open) "§aOPEN" else "§cCLOSED"
            player.sendMessage(Text.literal("§7Your gym/arena is now $status§7!"))
            
            // Announce to server
            CobbleGyms.server.playerManager.playerList.forEach { p ->
                if (p.uuid != uuid) {
                    val leaderName = player.name.string
                    if (open) {
                        p.sendMessage(Text.literal("§a$leaderName §7has opened their gym/arena!"))
                    }
                }
            }
        } else {
            player.sendMessage(Text.literal("§cFailed to change status."))
        }
        
        return 1
    }
    
    private fun equipTeam(ctx: CommandContext<ServerCommandSource>): Int {
        return equipTeamSlot(ctx, 1)
    }
    
    private fun equipTeamSlot(ctx: CommandContext<ServerCommandSource>, slot: Int): Int {
        val player = ctx.source.player ?: return 0
        val uuid = player.uuid
        
        val team = when {
            CobbleGyms.gymManager.isGymLeader(uuid) -> {
                val leader = GymRepository.getGymLeader(uuid)
                when (slot) {
                    1 -> leader?.team1
                    2 -> leader?.team2
                    3 -> leader?.team3
                    else -> null
                }
            }
            CobbleGyms.gymManager.isEliteFour(uuid) -> {
                val e4 = GymRepository.getEliteFour(uuid)
                when (slot) {
                    1 -> e4?.team1
                    2 -> e4?.team2
                    3 -> e4?.team3
                    else -> null
                }
            }
            CobbleGyms.gymManager.isChampion(uuid) -> {
                val champ = GymRepository.getChampion()
                when (slot) {
                    1 -> champ?.team1
                    2 -> champ?.team2
                    3 -> champ?.team3
                    else -> null
                }
            }
            else -> {
                player.sendMessage(Text.literal("§cYou don't have a Gym/E4/Champion role."))
                return 0
            }
        }
        
        if (team == null) {
            player.sendMessage(Text.literal("§cNo team configured for slot $slot. Ask a moderator to assign your team."))
            return 0
        }
        
        CobbleGyms.battleManager.equipTeam(uuid, team)
        player.sendMessage(Text.literal("§a§lTeam Equipped! §7Slot $slot is now active."))
        
        return 1
    }
    
    private fun unequipTeam(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(player.uuid)) {
            player.sendMessage(Text.literal("§cNo role assigned."))
            return 0
        }
        CobbleGyms.battleManager.unequipTeam(player.uuid)
        player.sendMessage(Text.literal("§7Team unequipped. Your personal team has been restored."))
        return 1
    }
    
    private fun startBattle(ctx: CommandContext<ServerCommandSource>, playerName: String): Int {
        val leader = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(leader.uuid)) {
            leader.sendMessage(Text.literal("§cNo role assigned."))
            return 0
        }
        
        val challenger = CobbleGyms.server.playerManager.playerList
            .find { it.name.string.equals(playerName, ignoreCase = true) }
        
        if (challenger == null) {
            leader.sendMessage(Text.literal("§cPlayer $playerName not found or not online."))
            return 0
        }
        
        // Verify challenger is in queue
        if (!GymRepository.isInQueue(leader.uuid, challenger.uuid)) {
            leader.sendMessage(Text.literal("§c${challenger.name.string} is not in your queue."))
            return 0
        }
        
        val battleType = when {
            CobbleGyms.gymManager.isGymLeader(leader.uuid) -> BattleManager.BattleType.GYM
            CobbleGyms.gymManager.isEliteFour(leader.uuid) -> BattleManager.BattleType.ELITE_FOUR
            CobbleGyms.gymManager.isChampion(leader.uuid) -> BattleManager.BattleType.CHAMPION
            else -> return 0
        }
        
        val result = CobbleGyms.battleManager.startBattle(leader, challenger, battleType)
        
        when (result) {
            is BattleManager.BattleStartResult.SUCCESS -> {
                leader.sendMessage(Text.literal("§a§lBattle started with ${challenger.name.string}!"))
            }
            BattleManager.BattleStartResult.LEADER_BUSY -> {
                leader.sendMessage(Text.literal("§cYou are already in a battle!"))
            }
            else -> {
                leader.sendMessage(Text.literal("§cCould not start battle."))
            }
        }
        
        return 1
    }
    
    private fun cancelAll(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(player.uuid)) return 0
        
        CobbleGyms.battleManager.cancelAllBattlesForLeader(player.uuid)
        player.sendMessage(Text.literal("§7All battles and queue entries cancelled."))
        return 1
    }
    
    private fun cancelCurrent(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        val battle = CobbleGyms.battleManager.getActiveBattleForLeader(player.uuid)
        
        if (battle == null) {
            player.sendMessage(Text.literal("§cNo active battle found."))
            return 0
        }
        
        CobbleGyms.battleManager.endBattle(battle.battleId, player.uuid, battle.turns, null, null)
        player.sendMessage(Text.literal("§7Current battle cancelled."))
        return 1
    }
    
    private fun banPlayer(ctx: CommandContext<ServerCommandSource>, playerName: String, hours: Int): Int {
        val leader = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(leader.uuid)) {
            leader.sendMessage(Text.literal("§cNo role assigned."))
            return 0
        }
        
        val target = CobbleGyms.server.playerManager.playerList
            .find { it.name.string.equals(playerName, ignoreCase = true) }
        
        if (target == null) {
            leader.sendMessage(Text.literal("§cPlayer $playerName not found online."))
            return 0
        }
        
        CobbleGyms.gymManager.banPlayer(leader.uuid, target.uuid, target.name.string, null, hours)
        leader.sendMessage(Text.literal("§a${target.name.string} has been banned from your gym for ${hours}h."))
        target.sendMessage(Text.literal("§cYou have been banned from ${leader.name.string}'s gym for ${hours} hours."))
        
        return 1
    }
    
    private fun unbanPlayer(ctx: CommandContext<ServerCommandSource>, playerName: String): Int {
        val leader = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(leader.uuid)) return 0
        
        val targetUuid = GymRepository.getQueue(leader.uuid)
            .find { it.challengerName.equals(playerName, ignoreCase = true) }?.challengerUuid
            ?: run {
                leader.sendMessage(Text.literal("§cPlayer $playerName not found."))
                return 0
            }
        
        CobbleGyms.gymManager.unbanPlayer(leader.uuid, targetUuid)
        leader.sendMessage(Text.literal("§a$playerName has been unbanned from your gym."))
        return 1
    }
    
    private fun showRecords(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(player.uuid)) return 0
        
        val records = GymRepository.getBattleRecordsForLeader(player.uuid, 10)
        
        if (records.isEmpty()) {
            player.sendMessage(Text.literal("§7No battle records yet."))
            return 1
        }
        
        player.sendMessage(Text.literal("§6§l=== Your Recent Battle Records ==="))
        records.forEach { record ->
            val winner = if (record.winner == "LEADER") "§aYou won" else "§c${record.challengerName} won"
            val date = java.time.Instant.ofEpochSecond(record.battleTime).toString().take(10)
            player.sendMessage(Text.literal("§7[$date] vs §f${record.challengerName}: $winner §7(${record.turns} turns)"))
        }
        
        return 1
    }
    
    private fun showMyQueue(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(player.uuid)) return 0
        
        val queue = GymRepository.getQueue(player.uuid)
        
        if (queue.isEmpty()) {
            player.sendMessage(Text.literal("§7No challengers in your queue."))
        } else {
            player.sendMessage(Text.literal("§6§l=== Your Queue (${queue.size} challengers) ==="))
            queue.forEachIndexed { i, entry ->
                player.sendMessage(Text.literal("§f${i+1}. §7${entry.challengerName}"))
            }
        }
        
        return 1
    }
    
    private fun showStats(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        if (!isLeaderOrHigher(player.uuid)) return 0
        
        val weekStart = getWeekStart()
        val stats = GymRepository.getWeeklyStats(player.uuid, weekStart)
        
        player.sendMessage(Text.literal("§6§l=== Your Weekly Stats ==="))
        if (stats == null) {
            player.sendMessage(Text.literal("§7No battles this week yet."))
        } else {
            player.sendMessage(Text.literal("§7Battles: §f${stats.battles}"))
            player.sendMessage(Text.literal("§7Wins: §a${stats.wins} §7| Losses: §c${stats.losses}"))
            player.sendMessage(Text.literal("§7Win Rate: §e${String.format("%.1f", stats.winrate * 100)}%"))
        }
        
        return 1
    }
    
    private fun isLeaderOrHigher(uuid: java.util.UUID): Boolean {
        return CobbleGyms.gymManager.isGymLeader(uuid) ||
                CobbleGyms.gymManager.isEliteFour(uuid) ||
                CobbleGyms.gymManager.isChampion(uuid)
    }
    
    private fun getWeekStart(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000
    }
}
