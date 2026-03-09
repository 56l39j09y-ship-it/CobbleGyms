package com.cobblegyms.command

import com.cobblegyms.CobbleGymsMod
import com.cobblegyms.system.battle.BattleQueueManager
import com.cobblegyms.system.battle.QueueEntry
import com.cobblegyms.system.validation.BattleValidator
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object ChallengeCommand {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("challenge")
                .then(
                    literal("leader")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> challengeLeader(ctx) }
                        )
                )
                .then(
                    literal("elite4")
                        .then(
                            argument("player", StringArgumentType.word())
                                .executes { ctx -> challengeEliteFour(ctx) }
                        )
                )
                .then(
                    literal("champion")
                        .executes { ctx -> challengeChampion(ctx) }
                )
                .then(
                    literal("leave")
                        .executes { ctx -> leaveQueue(ctx) }
                )
        )
    }

    private fun challengeLeader(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player ?: return 0
        val targetName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(targetName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$targetName' is not online.") }, false)
            return 0
        }

        val targetUuid = targetPlayer.uuid
        val validation = BattleValidator.canChallenge(player.uuid, targetUuid, "GYM_LEADER")
        if (!validation.valid) {
            validation.errors.forEach { source.sendFeedback({ Text.literal("§c$it") }, false) }
            return 0
        }

        val leader = CobbleGymsMod.gymLeaderManager.getGymLeader(targetUuid)!!
        val entry = QueueEntry(player.uuid, player.name.string, targetUuid, "GYM_LEADER")
        val added = BattleQueueManager.addToQueue(entry)

        if (added) {
            val pos = BattleQueueManager.getQueuePosition(player.uuid, targetUuid, "GYM_LEADER")
            source.sendFeedback({
                Text.literal("§aJoined queue for Gym Leader §e${leader.playerName} §a(${leader.typeSpecialty}). Position: §f$pos")
            }, false)
            targetPlayer.sendMessage(Text.literal("§e${player.name.string} §ahas joined your challenge queue! Queue size: §f${BattleQueueManager.getQueueForTarget(targetUuid, "GYM_LEADER").size}"))
        } else {
            source.sendFeedback({ Text.literal("§cYou are already in this queue.") }, false)
        }
        return 1
    }

    private fun challengeEliteFour(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player ?: return 0
        val targetName = StringArgumentType.getString(ctx, "player")
        val server = source.server
        val targetPlayer = server.playerManager.getPlayer(targetName)

        if (targetPlayer == null) {
            source.sendFeedback({ Text.literal("§cPlayer '$targetName' is not online.") }, false)
            return 0
        }

        val targetUuid = targetPlayer.uuid
        val validation = BattleValidator.canChallenge(player.uuid, targetUuid, "ELITE_FOUR")
        if (!validation.valid) {
            validation.errors.forEach { source.sendFeedback({ Text.literal("§c$it") }, false) }
            return 0
        }

        val member = CobbleGymsMod.eliteFourManager.getMember(targetUuid)!!
        val entry = QueueEntry(player.uuid, player.name.string, targetUuid, "ELITE_FOUR")
        val added = BattleQueueManager.addToQueue(entry)

        if (added) {
            val pos = BattleQueueManager.getQueuePosition(player.uuid, targetUuid, "ELITE_FOUR")
            source.sendFeedback({
                Text.literal("§aJoined queue for Elite Four §e${member.playerName} §a(#${member.position}). Position: §f$pos")
            }, false)
            targetPlayer.sendMessage(Text.literal("§e${player.name.string} §ahas joined your challenge queue! Queue size: §f${BattleQueueManager.getQueueForTarget(targetUuid, "ELITE_FOUR").size}"))
        } else {
            source.sendFeedback({ Text.literal("§cYou are already in this queue.") }, false)
        }
        return 1
    }

    private fun challengeChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player ?: return 0
        val champion = CobbleGymsMod.championManager.getChampion()

        if (champion == null) {
            source.sendFeedback({ Text.literal("§cThere is no current champion.") }, false)
            return 0
        }

        val validation = BattleValidator.canChallenge(player.uuid, champion.uuid, "CHAMPION")
        if (!validation.valid) {
            validation.errors.forEach { source.sendFeedback({ Text.literal("§c$it") }, false) }
            return 0
        }

        val entry = QueueEntry(player.uuid, player.name.string, champion.uuid, "CHAMPION")
        val added = BattleQueueManager.addToQueue(entry)

        if (added) {
            val pos = BattleQueueManager.getQueuePosition(player.uuid, champion.uuid, "CHAMPION")
            source.sendFeedback({
                Text.literal("§aJoined queue to challenge Champion §e${champion.playerName}§a. Position: §f$pos")
            }, false)
            val server = source.server
            server.playerManager.getPlayer(champion.uuid)?.sendMessage(
                Text.literal("§e${player.name.string} §ahas joined your challenge queue! Queue size: §f${BattleQueueManager.getQueueForTarget(champion.uuid, "CHAMPION").size}")
            )
        } else {
            source.sendFeedback({ Text.literal("§cYou are already in the champion queue.") }, false)
        }
        return 1
    }

    private fun leaveQueue(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player ?: return 0
        BattleQueueManager.removeAllQueuesForPlayer(player.uuid)
        source.sendFeedback({ Text.literal("§aYou have left all challenge queues.") }, false)
        return 1
    }
}
