package com.cobblegyms.command;

import com.cobblegyms.gui.ChallengeGui;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.util.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ChallengeCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("challenge")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                ctx.getSource().sendError(net.minecraft.text.Text.literal("Only players can use this command."));
                                return 0;
                            }
                            if (!GymManager.getInstance().isGymLeader(player.getUuid())) {
                                MessageUtil.sendError(player, "You are not a gym leader, E4, or Champion!");
                                return 0;
                            }
                            ChallengeGui.open(player);
                            return 1;
                        })
                        .then(CommandManager.literal("open")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    if (!GymManager.getInstance().isGymLeader(player.getUuid())) {
                                        MessageUtil.sendError(player, "You are not a gym leader!");
                                        return 0;
                                    }
                                    GymManager.getInstance().openGym(player.getUuid());
                                    return 1;
                                }))
                        .then(CommandManager.literal("close")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    if (!GymManager.getInstance().isGymLeader(player.getUuid())) {
                                        MessageUtil.sendError(player, "You are not a gym leader!");
                                        return 0;
                                    }
                                    GymManager.getInstance().closeGym(player.getUuid());
                                    return 1;
                                }))
                        .then(CommandManager.literal("next")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    if (!GymManager.getInstance().isGymLeader(player.getUuid())) {
                                        MessageUtil.sendError(player, "You are not a gym leader!");
                                        return 0;
                                    }
                                    com.cobblegyms.system.QueueManager.getInstance().startNextBattle(player.getUuid());
                                    return 1;
                                }))
                        .then(CommandManager.literal("queue")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    if (!GymManager.getInstance().isGymLeader(player.getUuid())) {
                                        MessageUtil.sendError(player, "You are not a gym leader!");
                                        return 0;
                                    }
                                    var queue = com.cobblegyms.system.QueueManager.getInstance().getQueue(player.getUuid());
                                    MessageUtil.sendInfo(player, "Queue size: " + queue.size());
                                    for (int i = 0; i < queue.size(); i++) {
                                        MessageUtil.sendInfo(player, (i + 1) + ". " + queue.get(i).getChallengerName());
                                    }
                                    return 1;
                                }))
                        .then(CommandManager.literal("stats")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    com.cobblegyms.gui.StatsGui.open(player, player.getUuid());
                                    return 1;
                                }))
        );
    }
}
