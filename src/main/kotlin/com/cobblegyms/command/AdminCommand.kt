package com.cobblegyms.command

import com.cobblegyms.CobbleGymsMod
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object AdminCommand {
    private const val PERMISSION_LEVEL = 3

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("gymsadmin")
                .requires { it.hasPermissionLevel(PERMISSION_LEVEL) }
                .then(
                    literal("setleader")
                        .then(
                            argument("player", StringArgumentType.word())
                                .then(
                                    argument("type", StringArgumentType.word())
                                        .executes { ctx -> setGymLeader(ctx) }
                                )
                        )
                )
                .then(
                    literal("removeleader")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> removeGymLeader(ctx) }
                        )
                )
                .then(
                    literal("sete4")
                        .then(
                            argument("player", StringArgumentType.word())
                                .then(
                                    argument("position", IntegerArgumentType.integer(1, 4))
                                        .then(
                                            argument("type", StringArgumentType.word())
                                                .executes { ctx -> setEliteFour(ctx) }
                                        )
                                )
                        )
                )
                .then(
                    literal("removee4")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> removeEliteFour(ctx) }
                        )
                )
                .then(
                    literal("setchampion")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> setChampion(ctx) }
                        )
                )
                .then(
                    literal("removechampion")
                        .executes { ctx -> removeChampion(ctx) }
                )
                .then(
                    literal("ban")
                        .then(
                            argument("player", StringArgumentType.word())
                                .then(
                                    argument("reason", StringArgumentType.greedyString())
                                        .executes { ctx -> banPlayer(ctx, -1) }
                                )
                        )
                )
                .then(
                    literal("tempban")
                        .then(
                            argument("player", StringArgumentType.word())
                                .then(
                                    argument("days", IntegerArgumentType.integer(1))
                                        .then(
                                            argument("reason", StringArgumentType.greedyString())
                                                .executes { ctx ->
                                                    banPlayer(ctx, IntegerArgumentType.getInteger(ctx, "days"))
                                                }
                                        )
                                )
                        )
                )
                .then(
                    literal("unban")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> unbanPlayer(ctx) }
                        )
                )
                .then(
                    literal("season")
                        .then(literal("new").executes { ctx -> newSeason(ctx) })
                        .then(literal("end").executes { ctx -> endSeason(ctx) })
                        .then(literal("info").executes { ctx -> seasonInfo(ctx) })
                )
                .then(
                    literal("clearqueue")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> clearPlayerQueues(ctx) }
                        )
                )
                .then(literal("reload").executes { ctx -> reloadConfig(ctx) })
        )
    }

    private fun setGymLeader(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val typeSpecialty = StringArgumentType.getString(ctx, "type")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        val success = CobbleGymsMod.gymLeaderManager.setGymLeader(
            targetPlayer.uuid, targetPlayer.name.string, typeSpecialty
        )
        if (success) {
            source.sendFeedback({
                Text.literal("§a${targetPlayer.name.string} has been set as Gym Leader (${typeSpecialty}).")
            }, true)
            targetPlayer.sendMessage(Text.literal("§aYou have been set as a Gym Leader (${typeSpecialty})!"))
        } else {
            source.sendFeedback({ Text.literal("§cFailed to set gym leader. Maximum leaders may be reached.") }, false)
        }
        return 1
    }

    private fun removeGymLeader(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        val success = CobbleGymsMod.gymLeaderManager.removeGymLeader(targetPlayer.uuid)
        if (success) {
            source.sendFeedback({
                Text.literal("§a${targetPlayer.name.string} has been removed as Gym Leader.")
            }, true)
            targetPlayer.sendMessage(Text.literal("§cYou have been removed as a Gym Leader."))
        } else {
            source.sendFeedback({ Text.literal("§cThat player is not a Gym Leader.") }, false)
        }
        return 1
    }

    private fun setEliteFour(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val position = IntegerArgumentType.getInteger(ctx, "position")
        val typeSpecialty = StringArgumentType.getString(ctx, "type")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        val success = CobbleGymsMod.eliteFourManager.setMember(
            targetPlayer.uuid, targetPlayer.name.string, position, typeSpecialty
        )
        if (success) {
            source.sendFeedback({
                Text.literal("§a${targetPlayer.name.string} has been set as Elite Four #$position ($typeSpecialty).")
            }, true)
            targetPlayer.sendMessage(Text.literal("§aYou have been set as Elite Four member #$position ($typeSpecialty)!"))
        } else {
            source.sendFeedback({ Text.literal("§cInvalid position (1-4).") }, false)
        }
        return 1
    }

    private fun removeEliteFour(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        val success = CobbleGymsMod.eliteFourManager.removeMember(targetPlayer.uuid)
        if (success) {
            source.sendFeedback({
                Text.literal("§a${targetPlayer.name.string} has been removed from the Elite Four.")
            }, true)
        } else {
            source.sendFeedback({ Text.literal("§cThat player is not in the Elite Four.") }, false)
        }
        return 1
    }

    private fun setChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        CobbleGymsMod.championManager.setChampion(targetPlayer.uuid, targetPlayer.name.string)
        source.sendFeedback({
            Text.literal("§a${targetPlayer.name.string} has been set as the Champion!")
        }, true)
        targetPlayer.sendMessage(Text.literal("§6★ You are now the Champion! ★"))
        return 1
    }

    private fun removeChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val success = CobbleGymsMod.championManager.removeChampion()
        if (success) {
            source.sendFeedback({ Text.literal("§aThe champion has been removed.") }, true)
        } else {
            source.sendFeedback({ Text.literal("§cThere is no current champion.") }, false)
        }
        return 1
    }

    private fun banPlayer(ctx: CommandContext<ServerCommandSource>, days: Int): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val reason = StringArgumentType.getString(ctx, "reason")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        val bannedBy = source.name
        CobbleGymsMod.championManager.banPlayer(
            targetPlayer.uuid, targetPlayer.name.string, reason, bannedBy, days
        )

        val durationText = if (days > 0) " for $days days" else " permanently"
        source.sendFeedback({
            Text.literal("§a${targetPlayer.name.string} has been banned from the gym system$durationText. Reason: $reason")
        }, true)
        targetPlayer.sendMessage(Text.literal("§cYou have been banned from the gym system$durationText. Reason: $reason"))
        return 1
    }

    private fun unbanPlayer(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        val success = CobbleGymsMod.championManager.unbanPlayer(targetPlayer.uuid)
        if (success) {
            source.sendFeedback({
                Text.literal("§a${targetPlayer.name.string} has been unbanned from the gym system.")
            }, true)
        } else {
            source.sendFeedback({ Text.literal("§cThat player is not banned.") }, false)
        }
        return 1
    }

    private fun newSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val champion = CobbleGymsMod.championManager.getChampion()
        CobbleGymsMod.seasonManager.endCurrentSeason(champion?.uuid, champion?.playerName)
        val season = CobbleGymsMod.seasonManager.startNewSeason()
        source.sendFeedback({
            Text.literal("§aNew season #${season.id} has started!")
        }, true)
        source.server.playerManager.broadcast(
            Text.literal("§6A new CobbleGyms season (#${season.id}) has started! Use §e/gyms§6 to participate."),
            false
        )
        return 1
    }

    private fun endSeason(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val champion = CobbleGymsMod.championManager.getChampion()
        val success = CobbleGymsMod.seasonManager.endCurrentSeason(champion?.uuid, champion?.playerName)
        if (success) {
            source.sendFeedback({ Text.literal("§aCurrent season has ended.") }, true)
        } else {
            source.sendFeedback({ Text.literal("§cNo active season to end.") }, false)
        }
        return 1
    }

    private fun seasonInfo(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val season = CobbleGymsMod.seasonManager.getCurrentSeason()
        if (season == null) {
            source.sendFeedback({ Text.literal("§7No active season.") }, false)
        } else {
            source.sendFeedback({ Text.literal("§6Season #${season.id}") }, false)
            source.sendFeedback({ Text.literal("§7Days remaining: §f${CobbleGymsMod.seasonManager.getDaysRemaining()}") }, false)
            source.sendFeedback({ Text.literal("§7Champion: §f${season.championName ?: "None"}") }, false)
        }
        return 1
    }

    private fun clearPlayerQueues(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val playerName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(playerName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$playerName' is not online.") }, false)
            return 0
        }

        com.cobblegyms.system.battle.BattleQueueManager.removeAllQueuesForPlayer(targetPlayer.uuid)
        source.sendFeedback({
            Text.literal("§aCleared all queue entries for ${targetPlayer.name.string}.")
        }, false)
        return 1
    }

    private fun reloadConfig(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        com.cobblegyms.config.CobbleGymsConfig.load()
        source.sendFeedback({ Text.literal("§aCobbleGyms configuration reloaded.") }, false)
        return 1
    }
}
