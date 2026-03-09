package com.cobblegyms.commands

import com.cobblegyms.CobbleGyms
import com.cobblegyms.battle.BattleManager
import com.cobblegyms.battle.TeamValidator
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.PokemonType
import com.cobblegyms.gui.GymsMenu
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.UUID

object GymsCommand {
    
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("gyms")
                .executes { ctx -> openMainMenu(ctx) }
                .then(CommandManager.literal("badges")
                    .executes { ctx -> showBadges(ctx) })
                .then(CommandManager.literal("challenge")
                    .then(CommandManager.literal("gym")
                        .then(CommandManager.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests { _, builder ->
                                PokemonType.entries.forEach { builder.suggest(it.name.lowercase()) }
                                builder.buildFuture()
                            }
                            .executes { ctx -> challengeGym(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type")) }))
                    .then(CommandManager.literal("e4")
                        .then(CommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests { _, builder ->
                                GymRepository.getAllEliteFour().forEach { builder.suggest(it.playerName) }
                                builder.buildFuture()
                            }
                            .executes { ctx -> challengeE4(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "player")) }))
                    .then(CommandManager.literal("champion")
                        .executes { ctx -> challengeChampion(ctx) }))
                .then(CommandManager.literal("validate")
                    .executes { ctx -> validateTeam(ctx) })
                .then(CommandManager.literal("queue")
                    .then(CommandManager.argument("target", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes { ctx -> showQueue(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "target")) }))
                .then(CommandManager.literal("rules")
                    .executes { ctx -> showRules(ctx) })
                .then(CommandManager.literal("season")
                    .executes { ctx -> showSeason(ctx) })
                .then(CommandManager.literal("leaderboard")
                    .executes { ctx -> showLeaderboard(ctx) })
        )
    }
    
    private fun openMainMenu(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendError(Text.literal("This command must be run by a player."))
            return 0
        }
        GymsMenu.openMain(player)
        return 1
    }
    
    private fun showBadges(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        if (season == null) {
            player.sendMessage(Text.literal("§cNo active season found."))
            return 0
        }
        
        val badges = GymRepository.getPlayerBadges(player.uuid, season.id)
        val allLeaders = GymRepository.getAllGymLeaders()
        
        player.sendMessage(Text.literal("§6§l=== Your Badges (Season ${season.number}) ==="))
        
        if (allLeaders.isEmpty()) {
            player.sendMessage(Text.literal("§7No gyms configured yet."))
        } else {
            allLeaders.forEach { leader ->
                val hasBadge = badges.any { it.gymType == leader.gymType }
                val badgeIcon = if (hasBadge) "§a✓" else "§c✗"
                player.sendMessage(Text.literal("$badgeIcon §f${leader.gymType.displayName} Badge - §7${leader.playerName}"))
            }
        }
        
        val e4Victories = GymRepository.getPlayerE4Victories(player.uuid, season.id)
        val allE4 = GymRepository.getAllEliteFour()
        
        if (allE4.isNotEmpty()) {
            player.sendMessage(Text.literal("§6§l=== Elite Four ==="))
            allE4.forEach { e4 ->
                val beaten = e4Victories.any { it.e4Uuid == e4.playerUuid }
                val icon = if (beaten) "§a✓" else "§c✗"
                player.sendMessage(Text.literal("$icon §f${e4.playerName} §7(${e4.type1.displayName}/${e4.type2.displayName})"))
            }
        }
        
        val champion = GymRepository.getChampion()
        if (champion != null) {
            val hasBeatenChampion = GymRepository.hasBeatenAllE4(player.uuid, season.id)
            player.sendMessage(Text.literal("§6§l=== Champion ==="))
            player.sendMessage(Text.literal("§f${champion.playerName} §7- ${if (hasBeatenChampion) "§aDefeated" else "§cNot yet defeated"}"))
        }
        
        return 1
    }
    
    private fun challengeGym(ctx: CommandContext<ServerCommandSource>, typeName: String): Int {
        val player = ctx.source.player ?: return 0
        val type = PokemonType.fromString(typeName)
        if (type == null) {
            player.sendMessage(Text.literal("§cInvalid type: $typeName"))
            return 0
        }
        
        val leader = GymRepository.getGymLeaderByType(type)
        if (leader == null) {
            player.sendMessage(Text.literal("§cNo Gym Leader found for type: ${type.displayName}"))
            return 0
        }
        
        if (!leader.isOpen) {
            player.sendMessage(Text.literal("§c${leader.playerName}'s Gym is currently closed."))
            return 0
        }
        
        // Check if player is the leader themselves
        if (player.uuid == leader.playerUuid) {
            player.sendMessage(Text.literal("§cYou cannot challenge your own gym!"))
            return 0
        }
        
        // Check if player is banned
        if (GymRepository.isPlayerBanned(leader.playerUuid, player.uuid)) {
            player.sendMessage(Text.literal("§cYou are banned from ${leader.playerName}'s Gym."))
            return 0
        }
        
        // Check if gym leader has a team
        val leaderTeam = CobbleGyms.gymManager.getActiveTeam(leader.playerUuid)
        if (leaderTeam == null) {
            player.sendMessage(Text.literal("§c${leader.playerName} has not set up their gym team yet."))
            return 0
        }
        
        // Validate challenger's team
        val challengerTeam = getPlayerTeamAsPokepaste(player)
        if (challengerTeam != null) {
            val validation = TeamValidator.validateTeam(
                challengerTeam,
                leader.battleFormat,
                null,
                leader.extraBannedPokemon
            )
            if (!validation.valid) {
                player.sendMessage(Text.literal("§c§lTeam Validation Failed!"))
                validation.violations.forEach { player.sendMessage(Text.literal(it)) }
                player.sendMessage(Text.literal("§7Fix your team before challenging."))
                return 0
            }
        }
        
        // Check season and badges for gym leaders (they have their own type badge)
        
        // Join queue
        val leaderPlayer = CobbleGyms.server.playerManager.getPlayer(leader.playerUuid)
        val result = CobbleGyms.battleManager.joinQueue(leader.playerUuid, "GYM", player)
        
        when (result) {
            is BattleManager.QueueJoinResult.SUCCESS -> {
                player.sendMessage(Text.literal("§a§lJoined queue for ${leader.playerName}'s ${type.displayName} Gym! §7Position: ${result.position}"))
                leaderPlayer?.sendMessage(Text.literal("§e${player.name.string} §7has joined your gym queue! (Position ${result.position})"))
            }
            is BattleManager.QueueJoinResult.ON_COOLDOWN -> {
                val hours = result.remainingSeconds / 3600
                val minutes = (result.remainingSeconds % 3600) / 60
                player.sendMessage(Text.literal("§cYou must wait §e${hours}h ${minutes}m §cbefore challenging again."))
            }
            BattleManager.QueueJoinResult.ALREADY_IN_QUEUE -> player.sendMessage(Text.literal("§cYou're already in this gym's queue!"))
            BattleManager.QueueJoinResult.BANNED -> player.sendMessage(Text.literal("§cYou are banned from this gym."))
            BattleManager.QueueJoinResult.QUEUE_FULL -> player.sendMessage(Text.literal("§cThe queue is full. Try again later."))
            else -> player.sendMessage(Text.literal("§cCould not join queue."))
        }
        
        return 1
    }
    
    private fun challengeE4(ctx: CommandContext<ServerCommandSource>, playerName: String): Int {
        val player = ctx.source.player ?: return 0
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        
        if (season == null) {
            player.sendMessage(Text.literal("§cNo active season."))
            return 0
        }
        
        // Must have all gym badges
        if (!GymRepository.hasAllBadges(player.uuid, season.id)) {
            player.sendMessage(Text.literal("§cYou need all Gym badges to challenge the Elite Four!"))
            return 0
        }
        
        val e4 = GymRepository.getAllEliteFour().find { it.playerName.equals(playerName, ignoreCase = true) }
        if (e4 == null) {
            player.sendMessage(Text.literal("§cElite Four member not found: $playerName"))
            return 0
        }
        
        if (!e4.isOpen) {
            player.sendMessage(Text.literal("§c${e4.playerName} is not available right now."))
            return 0
        }
        
        val result = CobbleGyms.battleManager.joinQueue(e4.playerUuid, "ELITE_FOUR", player)
        handleQueueResult(player, result, e4.playerName)
        
        return 1
    }
    
    private fun challengeChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        
        if (season == null) {
            player.sendMessage(Text.literal("§cNo active season."))
            return 0
        }
        
        // Must have beaten all E4
        if (!GymRepository.hasBeatenAllE4(player.uuid, season.id)) {
            player.sendMessage(Text.literal("§cYou must defeat all Elite Four members first!"))
            return 0
        }
        
        val champion = GymRepository.getChampion()
        if (champion == null) {
            player.sendMessage(Text.literal("§cNo Champion has been designated yet."))
            return 0
        }
        
        if (!champion.isOpen) {
            player.sendMessage(Text.literal("§cThe Champion is not available right now."))
            return 0
        }
        
        val result = CobbleGyms.battleManager.joinQueue(champion.playerUuid, "CHAMPION", player)
        handleQueueResult(player, result, champion.playerName)
        
        return 1
    }
    
    private fun validateTeam(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        
        val team = getPlayerTeamAsPokepaste(player)
        if (team == null) {
            player.sendMessage(Text.literal("§cCould not read your team. Make sure you have Pokémon in your party."))
            return 0
        }
        
        val result = TeamValidator.validateTeam(team, com.cobblegyms.data.models.BattleFormat.SINGLES, null, null)
        
        if (result.valid) {
            player.sendMessage(Text.literal("§a§lTeam Valid! §7Your team passes all Smogon rules."))
        } else {
            player.sendMessage(Text.literal("§c§lTeam has violations:"))
            result.violations.forEach { player.sendMessage(Text.literal(it)) }
        }
        
        return 1
    }
    
    private fun showQueue(ctx: CommandContext<ServerCommandSource>, targetName: String): Int {
        val player = ctx.source.player ?: return 0
        
        // Find target by name (leader, e4, champion)
        val leader = GymRepository.getAllGymLeaders().find { it.playerName.equals(targetName, ignoreCase = true) }
        val e4 = GymRepository.getAllEliteFour().find { it.playerName.equals(targetName, ignoreCase = true) }
        val champion = GymRepository.getChampion()?.takeIf { it.playerName.equals(targetName, ignoreCase = true) }
        
        val targetUuid = leader?.playerUuid ?: e4?.playerUuid ?: champion?.playerUuid
        if (targetUuid == null) {
            player.sendMessage(Text.literal("§cTarget not found: $targetName"))
            return 0
        }
        
        val queue = GymRepository.getQueue(targetUuid)
        
        if (queue.isEmpty()) {
            player.sendMessage(Text.literal("§7No challengers in queue for $targetName."))
        } else {
            player.sendMessage(Text.literal("§6§l=== Queue for $targetName ==="))
            queue.forEachIndexed { i, entry ->
                val isYou = entry.challengerUuid == player.uuid
                val marker = if (isYou) " §e(You)" else ""
                player.sendMessage(Text.literal("§f${i + 1}. §7${entry.challengerName}$marker"))
            }
        }
        
        return 1
    }
    
    private fun showRules(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        val config = CobbleGyms.gymManager.let { com.cobblegyms.config.GymConfig.config.smogon }
        
        player.sendMessage(Text.literal("§6§l=== CobbleGyms Rules ==="))
        player.sendMessage(Text.literal("§e§lFormat: §f${config.format}"))
        player.sendMessage(Text.literal("§e§lClauses: §f${config.clauses.joinToString(", ")}"))
        player.sendMessage(Text.literal("§e§lBanned Pokémon: §f${config.bannedPokemon.take(10).joinToString(", ")}..."))
        player.sendMessage(Text.literal("§e§lBanned Moves: §f${config.bannedMoves.joinToString(", ")}"))
        player.sendMessage(Text.literal("§e§lBanned Abilities: §f${config.bannedAbilities.joinToString(", ")}"))
        player.sendMessage(Text.literal("§7Use §f/gyms validate §7to check your team."))
        
        return 1
    }
    
    private fun showSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        
        if (season == null) {
            player.sendMessage(Text.literal("§cNo active season."))
            return 0
        }
        
        player.sendMessage(Text.literal("§6§l=== Season ${season.number} ==="))
        player.sendMessage(Text.literal("§7Time remaining: §e${CobbleGyms.seasonManager.formatRemainingTime()}"))
        player.sendMessage(Text.literal("§7Started: §f${java.time.Instant.ofEpochSecond(season.startTime)}"))
        
        return 1
    }
    
    private fun showLeaderboard(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        val weekStart = getWeekStart()
        
        player.sendMessage(Text.literal("§6§l=== Weekly Leaderboard ==="))
        
        val gymStats = GymRepository.getWeeklyLeaderboard("GYM", weekStart).take(5)
        player.sendMessage(Text.literal("§e§lTop Gym Leaders:"))
        gymStats.forEachIndexed { i, stats ->
            player.sendMessage(Text.literal("§f${i+1}. §7${stats.playerName}: §a${stats.wins}W §c${stats.losses}L §7(${String.format("%.0f", stats.winrate * 100)}%)"))
        }
        
        val e4Stats = GymRepository.getWeeklyLeaderboard("ELITE_FOUR", weekStart).take(3)
        player.sendMessage(Text.literal("§e§lTop Elite Four:"))
        e4Stats.forEachIndexed { i, stats ->
            player.sendMessage(Text.literal("§f${i+1}. §7${stats.playerName}: §a${stats.wins}W §c${stats.losses}L"))
        }
        
        return 1
    }
    
    private fun handleQueueResult(player: ServerPlayerEntity, result: BattleManager.QueueJoinResult, targetName: String) {
        when (result) {
            is BattleManager.QueueJoinResult.SUCCESS -> {
                player.sendMessage(Text.literal("§a§lJoined queue for $targetName! §7Position: ${result.position}"))
            }
            is BattleManager.QueueJoinResult.ON_COOLDOWN -> {
                val hours = result.remainingSeconds / 3600
                val minutes = (result.remainingSeconds % 3600) / 60
                player.sendMessage(Text.literal("§cCooldown: §e${hours}h ${minutes}m §cremaining."))
            }
            BattleManager.QueueJoinResult.ALREADY_IN_QUEUE -> player.sendMessage(Text.literal("§cAlready in queue!"))
            BattleManager.QueueJoinResult.BANNED -> player.sendMessage(Text.literal("§cYou are banned."))
            BattleManager.QueueJoinResult.QUEUE_FULL -> player.sendMessage(Text.literal("§cQueue is full."))
            else -> player.sendMessage(Text.literal("§cFailed to join queue."))
        }
    }
    
    private fun getPlayerTeamAsPokepaste(player: ServerPlayerEntity): String? {
        // TODO: Implement Cobblemon party integration.
        // This requires CobblemonAPI.getParty(player)?.toShowdownFormat() once the
        // Cobblemon dependency exposes a showdown-format export.  Until that is
        // available, team validation via /gyms validate is not functional.
        return null
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
