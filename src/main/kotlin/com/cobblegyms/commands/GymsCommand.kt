package com.cobblegyms.commands

import com.cobblegyms.managers.BattleManager
import com.cobblegyms.managers.ChampionManager
import com.cobblegyms.managers.E4Manager
import com.cobblegyms.managers.GymManager
import com.cobblegyms.managers.SeasonManager
import com.cobblegyms.utils.MessageUtils
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * /gyms command – displays the status of the gym league, Elite Four,
 * current champion, and active season leaderboard.
 *
 * Usage:
 *   /gyms
 *   /gyms leaders
 *   /gyms e4
 *   /gyms champion
 *   /gyms leaderboard
 *   /gyms season
 *   /gyms rules
 */
class GymsCommand(
    private val gymManager: GymManager,
    private val e4Manager: E4Manager,
    private val championManager: ChampionManager,
    private val seasonManager: SeasonManager
) {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val root = CommandManager.literal("gyms")
            .executes(::showAll)
            .then(CommandManager.literal("leaders").executes(::showLeaders))
            .then(CommandManager.literal("e4").executes(::showE4))
            .then(CommandManager.literal("champion").executes(::showChampion))
            .then(CommandManager.literal("leaderboard").executes(::showLeaderboard))
            .then(CommandManager.literal("season").executes(::showSeason))
            .then(CommandManager.literal("rules").executes(::showRules))
            .build()

        dispatcher.root.addChild(root)
    }

    // -------------------------------------------------------------------------
    // Sub-command handlers
    // -------------------------------------------------------------------------

    private fun showAll(ctx: CommandContext<ServerCommandSource>): Int {
        showLeaders(ctx)
        showE4(ctx)
        showChampion(ctx)
        return 1
    }

    private fun showLeaders(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        source.sendMessage(Text.literal(MessageUtils.header("Gym Leaders")))

        val leaders = gymManager.getActiveLeaders()
        if (leaders.isEmpty()) {
            source.sendMessage(Text.literal("§7  No gym leaders are currently registered."))
        } else {
            leaders.forEach { leader ->
                val ratio = MessageUtils.ratio(leader.wins, leader.losses)
                source.sendMessage(
                    Text.literal(
                        "§e  [${leader.gymType}] §f${leader.playerName} §7- Badge: §b${leader.badgeName} §7| $ratio"
                    )
                )
            }
        }
        return 1
    }

    private fun showE4(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        source.sendMessage(Text.literal(MessageUtils.header("Elite Four")))

        val members = e4Manager.getActiveMembers()
        if (members.isEmpty()) {
            source.sendMessage(Text.literal("§7  No Elite Four members are currently registered."))
        } else {
            members.forEach { member ->
                val ratio = MessageUtils.ratio(member.wins, member.losses)
                source.sendMessage(
                    Text.literal("§5  [${member.position}] §f${member.playerName} §7| $ratio")
                )
            }
        }
        return 1
    }

    private fun showChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        source.sendMessage(Text.literal(MessageUtils.header("Champion")))

        val champion = championManager.getCurrentChampion()
        if (champion == null) {
            source.sendMessage(Text.literal("§7  The Champion title is vacant. Can you claim it?"))
        } else {
            source.sendMessage(
                Text.literal(
                    "§6  👑 ${champion.playerName} §7- Defenses: §a${champion.defenses} §7| Since: §f${champion.becameChampion}"
                )
            )
        }
        return 1
    }

    private fun showLeaderboard(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val season = seasonManager.getActiveSeason()
        if (season == null) {
            source.sendMessage(Text.literal("§7  No active season found."))
            return 0
        }

        source.sendMessage(Text.literal(MessageUtils.header("Season ${season.number} Leaderboard")))
        val board = seasonManager.getLeaderboard(season.id, 10)
        if (board.isEmpty()) {
            source.sendMessage(Text.literal("§7  No battles recorded yet this season."))
        } else {
            board.forEachIndexed { idx, stats ->
                val totalWins = stats.gymWins + stats.e4Wins + stats.championWins
                source.sendMessage(
                    Text.literal(
                        "§e  ${idx + 1}. §f${stats.playerName} §7- §bBadges: ${stats.badgesEarned} §7| Wins: §a$totalWins"
                    )
                )
            }
        }
        return 1
    }

    private fun showSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val season = seasonManager.getActiveSeason()
        if (season == null) {
            source.sendMessage(Text.literal("§7  No active season is running."))
            return 0
        }
        source.sendMessage(Text.literal(MessageUtils.header("Season ${season.number}")))
        source.sendMessage(Text.literal("§7  Started: §f${season.startDate}"))
        source.sendMessage(Text.literal("§7  Status: §aActive"))
        return 1
    }

    private fun showRules(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val cfg = com.cobblegyms.config.CobbleGymsConfig.get().smogon

        source.sendMessage(Text.literal(MessageUtils.header("Battle Rules")))
        source.sendMessage(Text.literal("§7  Format: §e${cfg.format.uppercase()}"))
        source.sendMessage(Text.literal("§7  Team Size: §e${cfg.teamSize} Pokémon"))
        source.sendMessage(Text.literal("§7  Max Level: §e${cfg.maxLevel}"))
        source.sendMessage(Text.literal("§7  Ban List: §e${if (cfg.banListEnabled) "Enabled" else "Disabled"}"))
        if (cfg.banListEnabled && cfg.bannedPokemon.isNotEmpty()) {
            source.sendMessage(Text.literal("§7  Banned Pokémon (${cfg.bannedPokemon.size}): §c${cfg.bannedPokemon.joinToString(", ")}"))
        }
        return 1
    }
}
