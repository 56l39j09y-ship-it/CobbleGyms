package com.cobblegyms.commands

import com.cobblegyms.CobbleGyms
import com.cobblegyms.battle.TeamValidator
import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.GymRepository
import com.cobblegyms.data.models.*
import com.cobblegyms.pokepaste.PokepasteImporter
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object GymsAdminCommand {
    
    private const val PERMISSION_LEVEL = 2 // OP level 2
    
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("gymsadmin")
                .requires { src -> src.hasPermissionLevel(PERMISSION_LEVEL) }
                
                // ===== GYM MANAGEMENT =====
                .then(CommandManager.literal("gym")
                    .then(CommandManager.literal("assign")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("type", StringArgumentType.word())
                                .suggests { _, builder ->
                                    PokemonType.entries.forEach { builder.suggest(it.name.lowercase()) }
                                    builder.buildFuture()
                                }
                                .executes { ctx -> assignGymLeader(ctx,
                                    EntityArgumentType.getPlayer(ctx, "player"),
                                    StringArgumentType.getString(ctx, "type")) })))
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes { ctx -> removeGymLeader(ctx, EntityArgumentType.getPlayer(ctx, "player")) }))
                    .then(CommandManager.literal("setteam")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 3))
                                .then(CommandManager.argument("pokepaste_or_url", StringArgumentType.greedyString())
                                    .executes { ctx -> setGymTeam(ctx,
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                        StringArgumentType.getString(ctx, "pokepaste_or_url")) }))))
                    .then(CommandManager.literal("setformat")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("format", StringArgumentType.word())
                                .suggests { _, builder ->
                                    builder.suggest("singles")
                                    builder.suggest("doubles")
                                    builder.buildFuture()
                                }
                                .executes { ctx -> setGymFormat(ctx,
                                    EntityArgumentType.getPlayer(ctx, "player"),
                                    StringArgumentType.getString(ctx, "format")) })))
                    .then(CommandManager.literal("setlocation")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes { ctx -> setGymLocation(ctx, EntityArgumentType.getPlayer(ctx, "player")) }))
                    .then(CommandManager.literal("multiteam")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("enabled", StringArgumentType.word())
                                .suggests { _, builder ->
                                    builder.suggest("true")
                                    builder.suggest("false")
                                    builder.buildFuture()
                                }
                                .executes { ctx -> setMultiTeam(ctx,
                                    EntityArgumentType.getPlayer(ctx, "player"),
                                    StringArgumentType.getString(ctx, "enabled").toBoolean()) })))
                    .then(CommandManager.literal("list")
                        .executes { ctx -> listGymLeaders(ctx) }))
                
                // ===== ELITE FOUR MANAGEMENT =====
                .then(CommandManager.literal("e4")
                    .then(CommandManager.literal("assign")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("type1", StringArgumentType.word())
                                .suggests { _, builder ->
                                    PokemonType.entries.forEach { builder.suggest(it.name.lowercase()) }
                                    builder.buildFuture()
                                }
                                .then(CommandManager.argument("type2", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        PokemonType.entries.forEach { builder.suggest(it.name.lowercase()) }
                                        builder.buildFuture()
                                    }
                                    .executes { ctx -> assignE4(ctx,
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "type1"),
                                        StringArgumentType.getString(ctx, "type2")) }))))
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes { ctx -> removeE4(ctx, EntityArgumentType.getPlayer(ctx, "player")) }))
                    .then(CommandManager.literal("setteam")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 3))
                                .then(CommandManager.argument("pokepaste_or_url", StringArgumentType.greedyString())
                                    .executes { ctx -> setE4Team(ctx,
                                        EntityArgumentType.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                        StringArgumentType.getString(ctx, "pokepaste_or_url")) }))))
                    .then(CommandManager.literal("setformat")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("format", StringArgumentType.word())
                                .executes { ctx -> setE4Format(ctx,
                                    EntityArgumentType.getPlayer(ctx, "player"),
                                    StringArgumentType.getString(ctx, "format")) })))
                    .then(CommandManager.literal("list")
                        .executes { ctx -> listE4(ctx) }))
                
                // ===== CHAMPION MANAGEMENT =====
                .then(CommandManager.literal("champion")
                    .then(CommandManager.literal("assign")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes { ctx -> assignChampion(ctx, EntityArgumentType.getPlayer(ctx, "player")) }))
                    .then(CommandManager.literal("remove")
                        .executes { ctx -> removeChampion(ctx) })
                    .then(CommandManager.literal("setteam")
                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 3))
                            .then(CommandManager.argument("pokepaste_or_url", StringArgumentType.greedyString())
                                .executes { ctx -> setChampionTeam(ctx,
                                    IntegerArgumentType.getInteger(ctx, "slot"),
                                    StringArgumentType.getString(ctx, "pokepaste_or_url")) })))
                    .then(CommandManager.literal("setformat")
                        .then(CommandManager.argument("format", StringArgumentType.word())
                            .executes { ctx -> setChampionFormat(ctx, StringArgumentType.getString(ctx, "format")) })))
                
                // ===== BATTLE MANAGEMENT =====
                .then(CommandManager.literal("redo")
                    .then(CommandManager.argument("leader", EntityArgumentType.player())
                        .executes { ctx -> redoBattle(ctx, EntityArgumentType.getPlayer(ctx, "leader")) }))
                
                // ===== SEASON MANAGEMENT =====
                .then(CommandManager.literal("season")
                    .then(CommandManager.literal("end")
                        .executes { ctx -> endSeason(ctx) })
                    .then(CommandManager.literal("info")
                        .executes { ctx -> seasonInfo(ctx) }))
                
                // ===== RULES MANAGEMENT =====
                .then(CommandManager.literal("rules")
                    .then(CommandManager.literal("banpokemon")
                        .then(CommandManager.argument("pokemon", StringArgumentType.word())
                            .executes { ctx -> banPokemon(ctx, StringArgumentType.getString(ctx, "pokemon")) }))
                    .then(CommandManager.literal("unbanpokemon")
                        .then(CommandManager.argument("pokemon", StringArgumentType.word())
                            .executes { ctx -> unbanPokemon(ctx, StringArgumentType.getString(ctx, "pokemon")) }))
                    .then(CommandManager.literal("banmove")
                        .then(CommandManager.argument("move", StringArgumentType.greedyString())
                            .executes { ctx -> banMove(ctx, StringArgumentType.getString(ctx, "move")) }))
                    .then(CommandManager.literal("unbanmove")
                        .then(CommandManager.argument("move", StringArgumentType.greedyString())
                            .executes { ctx -> unbanMove(ctx, StringArgumentType.getString(ctx, "move")) }))
                    .then(CommandManager.literal("list")
                        .executes { ctx -> listRules(ctx) }))
                
                // ===== RECORDS =====
                .then(CommandManager.literal("records")
                    .then(CommandManager.literal("gym")
                        .then(CommandManager.argument("leader", EntityArgumentType.player())
                            .executes { ctx -> viewRecords(ctx, EntityArgumentType.getPlayer(ctx, "leader")) }))
                    .then(CommandManager.literal("all")
                        .executes { ctx -> viewAllRecords(ctx) }))
                
                // ===== LEADERBOARD =====
                .then(CommandManager.literal("leaderboard")
                    .executes { ctx -> viewLeaderboard(ctx) })
        )
    }
    
    private fun assignGymLeader(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, typeName: String): Int {
        val type = PokemonType.fromString(typeName) ?: run {
            ctx.source.sendError(Text.literal("Invalid type: $typeName"))
            return 0
        }
        
        // Check if type is already taken by another leader
        val existing = GymRepository.getGymLeaderByType(type)
        if (existing != null && existing.playerUuid != player.uuid) {
            ctx.source.sendError(Text.literal("${type.displayName} gym is already taken by ${existing.playerName}!"))
            return 0
        }
        
        val success = CobbleGyms.gymManager.assignGymLeader(player, type)
        if (success) {
            ctx.source.sendFeedback({ Text.literal("§a${player.name.string} assigned as ${type.displayName} Gym Leader!") }, true)
            player.sendMessage(Text.literal("§a§lCongratulations! §7You are now the ${type.displayName} Gym Leader!"))
            CobbleGyms.discordBot.logAdminAction("Assign Gym Leader", ctx.source.name, "${player.name.string} -> ${type.displayName} Gym")
        } else {
            ctx.source.sendError(Text.literal("Failed to assign gym leader."))
        }
        return 1
    }
    
    private fun removeGymLeader(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity): Int {
        val leader = GymRepository.getGymLeader(player.uuid)
        if (leader == null) {
            ctx.source.sendError(Text.literal("${player.name.string} is not a Gym Leader."))
            return 0
        }
        CobbleGyms.gymManager.removeGymLeader(player.uuid)
        ctx.source.sendFeedback({ Text.literal("§a${player.name.string} removed as Gym Leader.") }, true)
        player.sendMessage(Text.literal("§cYour Gym Leader role has been removed by a moderator."))
        return 1
    }
    
    private fun setGymTeam(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, slot: Int, input: String): Int {
        val leader = GymRepository.getGymLeader(player.uuid)
        if (leader == null) {
            ctx.source.sendError(Text.literal("${player.name.string} is not a Gym Leader."))
            return 0
        }
        
        // Import pokepaste (from URL or raw)
        val pokepaste = if (input.startsWith("http")) {
            PokepasteImporter.importFromUrl(input) ?: run {
                ctx.source.sendError(Text.literal("Failed to import team from URL."))
                return 0
            }
        } else {
            input
        }
        
        // Validate team for gym type
        val allowedTypes = listOf(leader.gymType)
        val validation = TeamValidator.validateLeaderTeam(pokepaste, allowedTypes, leader.extraBannedPokemon)
        if (!validation.valid) {
            ctx.source.sendError(Text.literal("Team validation failed:"))
            validation.violations.forEach { ctx.source.sendError(Text.literal(it)) }
        }
        
        CobbleGyms.gymManager.setGymTeam(player.uuid, slot, pokepaste)
        ctx.source.sendFeedback({ Text.literal("§aTeam slot $slot set for ${player.name.string}!") }, true)
        player.sendMessage(Text.literal("§a§lTeam Slot $slot Updated! §7Use /challenge equip $slot to activate."))
        return 1
    }
    
    private fun setGymFormat(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, formatStr: String): Int {
        val format = BattleFormat.entries.find { it.name.equals(formatStr, ignoreCase = true) } ?: run {
            ctx.source.sendError(Text.literal("Invalid format. Use 'singles' or 'doubles'."))
            return 0
        }
        CobbleGyms.gymManager.setGymFormat(player.uuid, format)
        ctx.source.sendFeedback({ Text.literal("§a${player.name.string}'s gym format set to ${format.name}!") }, true)
        return 1
    }
    
    private fun setGymLocation(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity): Int {
        val leader = GymRepository.getGymLeader(player.uuid)
        if (leader == null) {
            ctx.source.sendError(Text.literal("${player.name.string} is not a Gym Leader."))
            return 0
        }
        
        val pos = player.pos
        val world = player.world.registryKey.value.toString()
        val loc = GymLocation(world, pos.x, pos.y, pos.z, player.yaw, player.pitch)
        CobbleGyms.gymManager.setGymLocation(player.uuid, loc)
        ctx.source.sendFeedback({ Text.literal("§aGym location set to ${player.name.string}'s current position!") }, true)
        return 1
    }
    
    private fun setMultiTeam(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, enabled: Boolean): Int {
        CobbleGyms.gymManager.setMultiTeamEnabled(player.uuid, enabled)
        ctx.source.sendFeedback({ Text.literal("§aMulti-team ${if (enabled) "enabled" else "disabled"} for ${player.name.string}!") }, true)
        return 1
    }
    
    private fun listGymLeaders(ctx: CommandContext<ServerCommandSource>): Int {
        val leaders = GymRepository.getAllGymLeaders()
        ctx.source.sendFeedback({ Text.literal("§6§l=== Gym Leaders (${leaders.size}) ===") }, false)
        leaders.forEach { leader ->
            val status = if (leader.isOpen) "§aOpen" else "§cClosed"
            ctx.source.sendFeedback({ Text.literal("§f${leader.gymType.displayName}: §7${leader.playerName} [$status] ${leader.battleFormat.name}") }, false)
        }
        return 1
    }
    
    private fun assignE4(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, typeName1: String, typeName2: String): Int {
        val type1 = PokemonType.fromString(typeName1) ?: run {
            ctx.source.sendError(Text.literal("Invalid type: $typeName1"))
            return 0
        }
        val type2 = PokemonType.fromString(typeName2) ?: run {
            ctx.source.sendError(Text.literal("Invalid type: $typeName2"))
            return 0
        }
        
        CobbleGyms.gymManager.assignEliteFour(player, type1, type2)
        ctx.source.sendFeedback({ Text.literal("§a${player.name.string} assigned as Elite Four (${type1.displayName}/${type2.displayName})!") }, true)
        player.sendMessage(Text.literal("§a§lCongratulations! §7You are now Elite Four (${type1.displayName}/${type2.displayName})!"))
        return 1
    }
    
    private fun removeE4(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity): Int {
        CobbleGyms.gymManager.removeEliteFour(player.uuid)
        ctx.source.sendFeedback({ Text.literal("§a${player.name.string} removed from Elite Four.") }, true)
        player.sendMessage(Text.literal("§cYour Elite Four role has been removed."))
        return 1
    }
    
    private fun setE4Team(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, slot: Int, input: String): Int {
        val e4 = GymRepository.getEliteFour(player.uuid)
        if (e4 == null) {
            ctx.source.sendError(Text.literal("${player.name.string} is not an Elite Four member."))
            return 0
        }
        
        val pokepaste = if (input.startsWith("http")) {
            PokepasteImporter.importFromUrl(input) ?: run {
                ctx.source.sendError(Text.literal("Failed to import team from URL."))
                return 0
            }
        } else {
            input
        }
        
        CobbleGyms.gymManager.setE4Team(player.uuid, slot, pokepaste)
        ctx.source.sendFeedback({ Text.literal("§aE4 team slot $slot set for ${player.name.string}!") }, true)
        player.sendMessage(Text.literal("§a§lTeam Slot $slot Updated!"))
        return 1
    }
    
    private fun setE4Format(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity, formatStr: String): Int {
        val format = BattleFormat.entries.find { it.name.equals(formatStr, ignoreCase = true) } ?: run {
            ctx.source.sendError(Text.literal("Invalid format. Use 'singles' or 'doubles'."))
            return 0
        }
        CobbleGyms.gymManager.setE4Format(player.uuid, format)
        ctx.source.sendFeedback({ Text.literal("§a${player.name.string}'s E4 format set to ${format.name}!") }, true)
        return 1
    }
    
    private fun listE4(ctx: CommandContext<ServerCommandSource>): Int {
        val e4List = GymRepository.getAllEliteFour()
        ctx.source.sendFeedback({ Text.literal("§6§l=== Elite Four (${e4List.size}) ===") }, false)
        e4List.forEach { e4 ->
            val status = if (e4.isOpen) "§aOpen" else "§cClosed"
            ctx.source.sendFeedback({ Text.literal("§f${e4.playerName}: §7${e4.type1.displayName}/${e4.type2.displayName} [$status] ${e4.battleFormat.name}") }, false)
        }
        return 1
    }
    
    private fun assignChampion(ctx: CommandContext<ServerCommandSource>, player: ServerPlayerEntity): Int {
        val existing = GymRepository.getChampion()
        if (existing != null) {
            ctx.source.sendFeedback({ Text.literal("§eReplacing existing champion: ${existing.playerName}") }, true)
            val oldChamp = CobbleGyms.server.playerManager.getPlayer(existing.playerUuid)
            oldChamp?.sendMessage(Text.literal("§cYour Champion title has been removed by a moderator."))
        }
        
        CobbleGyms.gymManager.assignChampion(player)
        ctx.source.sendFeedback({ Text.literal("§a${player.name.string} assigned as Champion!") }, true)
        player.sendMessage(Text.literal("§6§l★ CHAMPION! §7You have been designated as the Pokémon Champion!"))
        CobbleGyms.discordBot.notifyNewChampion(player.name.string)
        return 1
    }
    
    private fun removeChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val champ = GymRepository.getChampion()
        if (champ == null) {
            ctx.source.sendError(Text.literal("No Champion currently set."))
            return 0
        }
        CobbleGyms.gymManager.removeChampion()
        ctx.source.sendFeedback({ Text.literal("§aChampion ${champ.playerName} removed.") }, true)
        val champPlayer = CobbleGyms.server.playerManager.getPlayer(champ.playerUuid)
        champPlayer?.sendMessage(Text.literal("§cYour Champion title has been removed."))
        return 1
    }
    
    private fun setChampionTeam(ctx: CommandContext<ServerCommandSource>, slot: Int, input: String): Int {
        val champ = GymRepository.getChampion() ?: run {
            ctx.source.sendError(Text.literal("No Champion set."))
            return 0
        }
        
        val pokepaste = if (input.startsWith("http")) {
            PokepasteImporter.importFromUrl(input) ?: run {
                ctx.source.sendError(Text.literal("Failed to import team from URL."))
                return 0
            }
        } else {
            input
        }
        
        CobbleGyms.gymManager.setChampionTeam(champ.playerUuid, slot, pokepaste)
        ctx.source.sendFeedback({ Text.literal("§aChampion team slot $slot updated!") }, true)
        CobbleGyms.server.playerManager.getPlayer(champ.playerUuid)
            ?.sendMessage(Text.literal("§a§lChampion Team Slot $slot Updated!"))
        return 1
    }
    
    private fun setChampionFormat(ctx: CommandContext<ServerCommandSource>, formatStr: String): Int {
        val champ = GymRepository.getChampion() ?: run {
            ctx.source.sendError(Text.literal("No Champion set."))
            return 0
        }
        val format = BattleFormat.entries.find { it.name.equals(formatStr, ignoreCase = true) } ?: run {
            ctx.source.sendError(Text.literal("Invalid format."))
            return 0
        }
        CobbleGyms.gymManager.setChampionFormat(champ.playerUuid, format)
        ctx.source.sendFeedback({ Text.literal("§aChampion format set to ${format.name}!") }, true)
        return 1
    }
    
    private fun redoBattle(ctx: CommandContext<ServerCommandSource>, leader: ServerPlayerEntity): Int {
        val battle = CobbleGyms.battleManager.getActiveBattleForLeader(leader.uuid)
        if (battle == null) {
            ctx.source.sendError(Text.literal("No active battle found for ${leader.name.string}."))
            return 0
        }
        
        val result = CobbleGyms.battleManager.redoBattle(battle.battleId, ctx.source.player?.uuid ?: leader.uuid)
        when (result) {
            BattleManager.RedoResult.SUCCESS -> {
                ctx.source.sendFeedback({ Text.literal("§aBattle redone! Cooldowns reset.") }, true)
            }
            is BattleManager.RedoResult.TOO_MANY_TURNS -> {
                ctx.source.sendError(Text.literal("Cannot redo: battle has ${result.turns} turns (max 1)."))
            }
            BattleManager.RedoResult.NOT_FOUND -> {
                ctx.source.sendError(Text.literal("Battle not found."))
            }
        }
        return 1
    }
    
    private fun endSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        if (season == null) {
            ctx.source.sendError(Text.literal("No active season."))
            return 0
        }
        CobbleGyms.seasonManager.endSeason(season)
        ctx.source.sendFeedback({ Text.literal("§aSeason ${season.number} ended manually!") }, true)
        return 1
    }
    
    private fun seasonInfo(ctx: CommandContext<ServerCommandSource>): Int {
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        if (season == null) {
            ctx.source.sendFeedback({ Text.literal("§7No active season.") }, false)
            return 1
        }
        ctx.source.sendFeedback({ Text.literal("§6Season ${season.number} | Remaining: ${CobbleGyms.seasonManager.formatRemainingTime()}") }, false)
        return 1
    }
    
    private fun banPokemon(ctx: CommandContext<ServerCommandSource>, pokemon: String): Int {
        val config = GymConfig.config
        val newBanned = config.smogon.bannedPokemon + pokemon
        GymConfig.update(config.copy(smogon = config.smogon.copy(bannedPokemon = newBanned)))
        ctx.source.sendFeedback({ Text.literal("§a$pokemon added to global ban list.") }, true)
        return 1
    }
    
    private fun unbanPokemon(ctx: CommandContext<ServerCommandSource>, pokemon: String): Int {
        val config = GymConfig.config
        val newBanned = config.smogon.bannedPokemon.filter { !it.equals(pokemon, ignoreCase = true) }
        GymConfig.update(config.copy(smogon = config.smogon.copy(bannedPokemon = newBanned)))
        ctx.source.sendFeedback({ Text.literal("§a$pokemon removed from global ban list.") }, true)
        return 1
    }
    
    private fun banMove(ctx: CommandContext<ServerCommandSource>, move: String): Int {
        val config = GymConfig.config
        val newBanned = config.smogon.bannedMoves + move
        GymConfig.update(config.copy(smogon = config.smogon.copy(bannedMoves = newBanned)))
        ctx.source.sendFeedback({ Text.literal("§aMove '$move' added to ban list.") }, true)
        return 1
    }
    
    private fun unbanMove(ctx: CommandContext<ServerCommandSource>, move: String): Int {
        val config = GymConfig.config
        val newBanned = config.smogon.bannedMoves.filter { !it.equals(move, ignoreCase = true) }
        GymConfig.update(config.copy(smogon = config.smogon.copy(bannedMoves = newBanned)))
        ctx.source.sendFeedback({ Text.literal("§aMove '$move' removed from ban list.") }, true)
        return 1
    }
    
    private fun listRules(ctx: CommandContext<ServerCommandSource>): Int {
        val smogon = GymConfig.config.smogon
        ctx.source.sendFeedback({ Text.literal("§6§l=== Current Rules ===") }, false)
        ctx.source.sendFeedback({ Text.literal("§eBanned Pokémon: §f${smogon.bannedPokemon.joinToString(", ")}") }, false)
        ctx.source.sendFeedback({ Text.literal("§eBanned Moves: §f${smogon.bannedMoves.joinToString(", ")}") }, false)
        ctx.source.sendFeedback({ Text.literal("§eBanned Abilities: §f${smogon.bannedAbilities.joinToString(", ")}") }, false)
        ctx.source.sendFeedback({ Text.literal("§eClauses: §f${smogon.clauses.joinToString(", ")}") }, false)
        return 1
    }
    
    private fun viewRecords(ctx: CommandContext<ServerCommandSource>, leader: ServerPlayerEntity): Int {
        val records = GymRepository.getBattleRecordsForLeader(leader.uuid, 20)
        ctx.source.sendFeedback({ Text.literal("§6§l=== Records for ${leader.name.string} ===") }, false)
        records.forEach { record ->
            val winner = if (record.winner == "LEADER") "Leader won" else "Challenger won"
            ctx.source.sendFeedback({ Text.literal("§7vs ${record.challengerName}: $winner (${record.turns} turns)") }, false)
        }
        return 1
    }
    
    private fun viewAllRecords(ctx: CommandContext<ServerCommandSource>): Int {
        ctx.source.sendFeedback({ Text.literal("§7Use /gymsadmin records gym <player> to view specific records.") }, false)
        return 1
    }
    
    private fun viewLeaderboard(ctx: CommandContext<ServerCommandSource>): Int {
        val weekStart = getWeekStart()
        ctx.source.sendFeedback({ Text.literal("§6§l=== Admin Leaderboard ===") }, false)
        
        val gymStats = GymRepository.getWeeklyLeaderboard("GYM", weekStart)
        ctx.source.sendFeedback({ Text.literal("§eGym Leaders:") }, false)
        gymStats.forEachIndexed { i, stats ->
            ctx.source.sendFeedback({ Text.literal("§f${i+1}. ${stats.playerName}: §a${stats.wins}W §c${stats.losses}L §7(${String.format("%.0f", stats.winrate * 100)}% WR, ${stats.battles} total)") }, false)
        }
        
        val e4Stats = GymRepository.getWeeklyLeaderboard("ELITE_FOUR", weekStart)
        ctx.source.sendFeedback({ Text.literal("§eElite Four:") }, false)
        e4Stats.forEachIndexed { i, stats ->
            ctx.source.sendFeedback({ Text.literal("§f${i+1}. ${stats.playerName}: §a${stats.wins}W §c${stats.losses}L") }, false)
        }
        
        return 1
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
