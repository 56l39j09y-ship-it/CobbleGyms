package com.cobblegyms.command;

import com.cobblegyms.gui.GuiManager;
import com.cobblegyms.util.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GymsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("gyms")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                ctx.getSource().sendError(net.minecraft.text.Text.literal("Only players can use this command."));
                                return 0;
                            }
                            GuiManager.getInstance().openGymsMenu(player);
                            return 1;
                        })
                        .then(CommandManager.literal("badges")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    com.cobblegyms.gui.MedalGui.open(player);
                                    return 1;
                                }))
                        .then(CommandManager.literal("validate")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    com.cobblegyms.system.ValidationManager.getInstance().validateTeam(player, null);
                                    return 1;
                                }))
                        .then(CommandManager.literal("season")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    var season = com.cobblegyms.system.SeasonManager.getInstance().getCurrentSeason();
                                    MessageUtil.sendInfo(player, "Current season: " + (season != null ? season.getId() : "None"));
                                    MessageUtil.sendInfo(player, "Time remaining: "
                                            + com.cobblegyms.system.SeasonManager.getInstance().getRemainingTime());
                                    return 1;
                                }))
                        .then(CommandManager.literal("rules")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    com.cobblegyms.gui.RulesGui.open(player);
                                    return 1;
                                }))
                        .then(CommandManager.literal("rankings")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                                    com.cobblegyms.gui.RankingGui.open(player);
                                    return 1;
                                }))
        );
    }
}
