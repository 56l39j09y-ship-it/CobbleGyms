package com.cobblegyms.commands

import com.cobblegyms.managers.BattleManager
import com.cobblegyms.managers.BattleManager.BattleType
import com.cobblegyms.managers.ChampionManager
import com.cobblegyms.managers.E4Manager
import com.cobblegyms.managers.GymManager
import com.cobblegyms.utils.MessageUtils
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

/**
 * /challenge command – lets players join battle queues for gym leaders,
 * Elite Four members, and the Champion.
 *
 * Usage:
 *   /challenge leader <leaderName>
 *   /challenge e4 <position>
 *   /challenge champion
 *   /challenge leave
 *   /challenge status
 */
class ChallengeCommand(
    private val gymManager: GymManager,
    private val e4Manager: E4Manager,
    private val championManager: ChampionManager,
    private val battleManager: BattleManager
) {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val root = CommandManager.literal("challenge")
            .then(
                CommandManager.literal("leader")
                    .then(
                        CommandManager.argument("name", StringArgumentType.word())
                            .executes(::challengeLeader)
                    )
            )
            .then(
                CommandManager.literal("e4")
                    .then(
                        CommandManager.argument("position", StringArgumentType.word())
                            .executes(::challengeE4)
                    )
            )
            .then(CommandManager.literal("champion").executes(::challengeChampion))
            .then(CommandManager.literal("leave").executes(::leaveQueue))
            .then(CommandManager.literal("status").executes(::queueStatus))
            .executes { ctx ->
                ctx.source.sendMessage(
                    Text.literal("§7Usage: /challenge <leader|e4|champion|leave|status>")
                )
                0
            }
            .build()

        dispatcher.root.addChild(root)
    }

    // -------------------------------------------------------------------------
    // Sub-command handlers
    // -------------------------------------------------------------------------

    private fun challengeLeader(ctx: CommandContext<ServerCommandSource>): Int {
        val player = requirePlayer(ctx) ?: return 0
        val name = StringArgumentType.getString(ctx, "name")

        val leader = gymManager.getActiveLeaders()
            .firstOrNull { it.playerName.equals(name, ignoreCase = true) }

        if (leader == null) {
            MessageUtils.sendError(player, "Gym leader '$name' not found or is inactive.")
            return 0
        }

        if (leader.playerUuid == player.uuid) {
            MessageUtils.sendError(player, "You cannot challenge yourself!")
            return 0
        }

        val entry = BattleManager.QueueEntry(
            challengerUuid = player.uuid,
            challengerName = player.name.string,
            battleType = BattleType.GYM,
            defenderUuid = leader.playerUuid
        )

        val error = battleManager.enqueue(entry)
        if (error != null) {
            MessageUtils.sendError(player, error)
            return 0
        }

        val position = battleManager.getPosition(player.uuid, leader.playerUuid)
        MessageUtils.sendSuccess(
            player,
            "Joined ${leader.playerName}'s queue! §7Position: §e#$position §7| Timeout: §e${com.cobblegyms.config.CobbleGymsConfig.get().battle.queueTimeoutSeconds}s"
        )
        return 1
    }

    private fun challengeE4(ctx: CommandContext<ServerCommandSource>): Int {
        val player = requirePlayer(ctx) ?: return 0
        val posStr = StringArgumentType.getString(ctx, "position")

        val position = posStr.toIntOrNull()
        if (position == null || position !in 1..4) {
            MessageUtils.sendError(player, "Position must be 1, 2, 3, or 4.")
            return 0
        }

        val member = e4Manager.getActiveMembers().firstOrNull { it.position == position }
        if (member == null) {
            MessageUtils.sendError(player, "No Elite Four member at position $position.")
            return 0
        }

        if (member.playerUuid == player.uuid) {
            MessageUtils.sendError(player, "You cannot challenge yourself!")
            return 0
        }

        val entry = BattleManager.QueueEntry(
            challengerUuid = player.uuid,
            challengerName = player.name.string,
            battleType = BattleType.ELITE_FOUR,
            defenderUuid = member.playerUuid
        )

        val error = battleManager.enqueue(entry)
        if (error != null) {
            MessageUtils.sendError(player, error)
            return 0
        }

        val pos = battleManager.getPosition(player.uuid, member.playerUuid)
        MessageUtils.sendSuccess(
            player,
            "Joined Elite Four #$position (${member.playerName}) queue! §7Position: §e#$pos"
        )
        return 1
    }

    private fun challengeChampion(ctx: CommandContext<ServerCommandSource>): Int {
        val player = requirePlayer(ctx) ?: return 0

        val champion = championManager.getCurrentChampion()
        if (champion == null) {
            MessageUtils.sendError(player, "There is currently no champion to challenge!")
            return 0
        }

        if (champion.playerUuid == player.uuid) {
            MessageUtils.sendError(player, "You are the champion! Wait for challengers.")
            return 0
        }

        val entry = BattleManager.QueueEntry(
            challengerUuid = player.uuid,
            challengerName = player.name.string,
            battleType = BattleType.CHAMPION,
            defenderUuid = champion.playerUuid
        )

        val error = battleManager.enqueue(entry)
        if (error != null) {
            MessageUtils.sendError(player, error)
            return 0
        }

        val pos = battleManager.getPosition(player.uuid, champion.playerUuid)
        MessageUtils.sendSuccess(
            player,
            "Joined Champion (${champion.playerName}) queue! §7Position: §e#$pos"
        )
        return 1
    }

    private fun leaveQueue(ctx: CommandContext<ServerCommandSource>): Int {
        val player = requirePlayer(ctx) ?: return 0
        battleManager.removeFromAllQueues(player.uuid)
        MessageUtils.sendInfo(player, "You have left all battle queues.")
        return 1
    }

    private fun queueStatus(ctx: CommandContext<ServerCommandSource>): Int {
        val player = requirePlayer(ctx) ?: return 0

        // Check leader queues
        val leaderStatuses = gymManager.getActiveLeaders().mapNotNull { leader ->
            val pos = battleManager.getPosition(player.uuid, leader.playerUuid)
            if (pos != null) "§e${leader.playerName} §7(Gym) - Position §e#$pos" else null
        }

        // Check E4 queues
        val e4Statuses = e4Manager.getActiveMembers().mapNotNull { member ->
            val pos = battleManager.getPosition(player.uuid, member.playerUuid)
            if (pos != null) "§5${member.playerName} §7(E4) - Position §e#$pos" else null
        }

        // Check champion queue
        val champion = championManager.getCurrentChampion()
        val champStatus = champion?.let {
            val pos = battleManager.getPosition(player.uuid, it.playerUuid)
            if (pos != null) "§6${it.playerName} §7(Champion) - Position §e#$pos" else null
        }

        val allStatuses = leaderStatuses + e4Statuses + listOfNotNull(champStatus)
        if (allStatuses.isEmpty()) {
            MessageUtils.sendInfo(player, "You are not in any queue.")
        } else {
            player.sendMessage(Text.literal(MessageUtils.header("Your Queue Status")), false)
            allStatuses.forEach { line ->
                player.sendMessage(Text.literal("  $line"), false)
            }
        }
        return 1
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun requirePlayer(ctx: CommandContext<ServerCommandSource>): ServerPlayerEntity? {
        val player = ctx.source.player
        if (player == null) {
            ctx.source.sendMessage(Text.literal("§cThis command can only be run by a player."))
        }
        return player
    }
}
