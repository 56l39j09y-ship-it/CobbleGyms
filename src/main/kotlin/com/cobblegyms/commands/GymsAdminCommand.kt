package com.cobblegyms.commands

import com.cobblegyms.managers.ChampionManager
import com.cobblegyms.managers.E4Manager
import com.cobblegyms.managers.GymManager
import com.cobblegyms.managers.SeasonManager
import com.cobblegyms.utils.MessageUtils
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * /gymsadmin command – privileged management of the gym league.
 *
 * Requires the `cobblegyms.admin` permission level (operator by default).
 *
 * Sub-commands:
 *   setleader <player> <type> <badgeName>   – Register/update a gym leader
 *   removeleader <player>                   – Remove a gym leader
 *   sete4 <player> <position(1-4)>          – Assign Elite Four position
 *   removee4 <player>                       – Remove an Elite Four member
 *   setchampion <player>                    – Crown the champion
 *   startseason                             – Start a new season
 *   endseason                               – End the current season
 *   leaderboard [limit]                     – Print the season leaderboard
 *   reload                                  – Reload configuration
 */
class GymsAdminCommand(
    private val gymManager: GymManager,
    private val e4Manager: E4Manager,
    private val championManager: ChampionManager,
    private val seasonManager: SeasonManager
) {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val root = CommandManager.literal("gymsadmin")
            .requires { it.hasPermissionLevel(2) }

            // setleader <player> <type> <badgeName>
            .then(
                CommandManager.literal("setleader")
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .then(
                                CommandManager.argument("type", StringArgumentType.word())
                                    .then(
                                        CommandManager.argument("badgeName", StringArgumentType.word())
                                            .executes(::setLeader)
                                    )
                            )
                    )
            )
            // removeleader <player>
            .then(
                CommandManager.literal("removeleader")
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .executes(::removeLeader)
                    )
            )
            // sete4 <player> <position>
            .then(
                CommandManager.literal("sete4")
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .then(
                                CommandManager.argument("position", IntegerArgumentType.integer(1, 4))
                                    .executes(::setE4)
                            )
                    )
            )
            // removee4 <player>
            .then(
                CommandManager.literal("removee4")
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .executes(::removeE4)
                    )
            )
            // setchampion <player>
            .then(
                CommandManager.literal("setchampion")
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .executes(::setChampion)
                    )
            )
            // startseason
            .then(CommandManager.literal("startseason").executes(::startSeason))
            // endseason
            .then(CommandManager.literal("endseason").executes(::endSeason))
            // leaderboard [limit]
            .then(
                CommandManager.literal("leaderboard")
                    .executes { showLeaderboard(it, 10) }
                    .then(
                        CommandManager.argument("limit", IntegerArgumentType.integer(1, 100))
                            .executes { showLeaderboard(it, IntegerArgumentType.getInteger(it, "limit")) }
                    )
            )
            // reload
            .then(CommandManager.literal("reload").executes(::reloadConfig))

            // Default: show help
            .executes(::showHelp)
            .build()

        dispatcher.root.addChild(root)
    }

    // -------------------------------------------------------------------------
    // Sub-command handlers
    // -------------------------------------------------------------------------

    private fun setLeader(ctx: CommandContext<ServerCommandSource>): Int {
        val target = EntityArgumentType.getPlayer(ctx, "player")
        val type = StringArgumentType.getString(ctx, "type")
        val badge = StringArgumentType.getString(ctx, "badgeName")

        val isNew = gymManager.setGymLeader(target.uuid, target.name.string, type, badge)
        val verb = if (isNew) "registered as" else "updated to"
        MessageUtils.sendSuccess(
            ctx.source.player ?: return sendConsoleSuccess(ctx.source, "${target.name.string} $verb $type gym leader"),
            "${target.name.string} $verb $type Gym Leader (badge: $badge)"
        )
        target.sendMessage(
            Text.literal("§6✦ You have been assigned as the §e$type §6Gym Leader! Badge: §b$badge"), false
        )
        return 1
    }

    private fun removeLeader(ctx: CommandContext<ServerCommandSource>): Int {
        val target = EntityArgumentType.getPlayer(ctx, "player")
        return if (gymManager.removeGymLeader(target.uuid)) {
            ctx.source.sendMessage(Text.literal("§a✓ ${target.name.string} removed as Gym Leader."))
            target.sendMessage(Text.literal("§cYou have been removed as a Gym Leader."), false)
            1
        } else {
            ctx.source.sendMessage(Text.literal("§c${target.name.string} is not a registered Gym Leader."))
            0
        }
    }

    private fun setE4(ctx: CommandContext<ServerCommandSource>): Int {
        val target = EntityArgumentType.getPlayer(ctx, "player")
        val position = IntegerArgumentType.getInteger(ctx, "position")

        val isNew = e4Manager.setEliteFour(target.uuid, target.name.string, position)
        val verb = if (isNew) "assigned to" else "updated to"
        ctx.source.sendMessage(Text.literal("§a✓ ${target.name.string} $verb Elite Four position $position."))
        target.sendMessage(
            Text.literal("§5✦ You have been assigned as Elite Four member #$position!"), false
        )
        return 1
    }

    private fun removeE4(ctx: CommandContext<ServerCommandSource>): Int {
        val target = EntityArgumentType.getPlayer(ctx, "player")
        return if (e4Manager.removeEliteFour(target.uuid)) {
            ctx.source.sendMessage(Text.literal("§a✓ ${target.name.string} removed from Elite Four."))
            target.sendMessage(Text.literal("§cYou have been removed from the Elite Four."), false)
            1
        } else {
            ctx.source.sendMessage(Text.literal("§c${target.name.string} is not an Elite Four member."))
            0
        }
    }

    private fun setChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val target = EntityArgumentType.getPlayer(ctx, "player")
        championManager.setChampion(target.uuid, target.name.string)
        ctx.source.sendMessage(Text.literal("§a✓ ${target.name.string} has been crowned Champion!"))
        target.sendMessage(
            Text.literal("§6§l✦ You are now the CHAMPION! ✦ §r§6Defend your title!"), false
        )
        // Broadcast to server
        ctx.source.server.playerManager.playerList.forEach { p ->
            p.sendMessage(
                Text.literal("§6§l★ NEW CHAMPION ★ §r§e${target.name.string} §6is now the Pokémon Champion!"), false
            )
        }
        return 1
    }

    private fun startSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val number = seasonManager.startNewSeason()
        ctx.source.sendMessage(Text.literal("§a✓ Season $number has started!"))
        ctx.source.server.playerManager.playerList.forEach { p ->
            p.sendMessage(Text.literal("§6§l⚡ Season $number has begun! §r§fMay the best trainer win!"), false)
        }
        return 1
    }

    private fun endSeason(ctx: CommandContext<ServerCommandSource>): Int {
        return if (seasonManager.endCurrentSeason()) {
            ctx.source.sendMessage(Text.literal("§a✓ Current season ended."))
            ctx.source.server.playerManager.playerList.forEach { p ->
                p.sendMessage(Text.literal("§c§l⚡ The current season has ended! §r§7Stay tuned for the next season."), false)
            }
            1
        } else {
            ctx.source.sendMessage(Text.literal("§cNo active season to end."))
            0
        }
    }

    private fun showLeaderboard(ctx: CommandContext<ServerCommandSource>, limit: Int): Int {
        val season = seasonManager.getActiveSeason()
        if (season == null) {
            ctx.source.sendMessage(Text.literal("§cNo active season."))
            return 0
        }
        ctx.source.sendMessage(Text.literal(MessageUtils.header("Season ${season.number} Top $limit")))
        val board = seasonManager.getLeaderboard(season.id, limit)
        if (board.isEmpty()) {
            ctx.source.sendMessage(Text.literal("§7No data yet."))
        } else {
            board.forEachIndexed { idx, stats ->
                val wins = stats.gymWins + stats.e4Wins + stats.championWins
                ctx.source.sendMessage(
                    Text.literal(
                        "§e${idx + 1}. §f${stats.playerName} §7- Badges: §b${stats.badgesEarned} | Wins: §a$wins"
                    )
                )
            }
        }
        return 1
    }

    private fun reloadConfig(ctx: CommandContext<ServerCommandSource>): Int {
        com.cobblegyms.config.CobbleGymsConfig.load()
        ctx.source.sendMessage(Text.literal("§a✓ CobbleGyms configuration reloaded."))
        return 1
    }

    private fun showHelp(ctx: CommandContext<ServerCommandSource>): Int {
        ctx.source.sendMessage(Text.literal(MessageUtils.header("CobbleGyms Admin Help")))
        listOf(
            "/gymsadmin setleader <player> <type> <badge>",
            "/gymsadmin removeleader <player>",
            "/gymsadmin sete4 <player> <1-4>",
            "/gymsadmin removee4 <player>",
            "/gymsadmin setchampion <player>",
            "/gymsadmin startseason",
            "/gymsadmin endseason",
            "/gymsadmin leaderboard [limit]",
            "/gymsadmin reload"
        ).forEach { ctx.source.sendMessage(Text.literal("§7  $it")) }
        return 1
    }

    private fun sendConsoleSuccess(source: ServerCommandSource, message: String): Int {
        source.sendMessage(Text.literal("§a✓ $message"))
        return 1
    }
}
