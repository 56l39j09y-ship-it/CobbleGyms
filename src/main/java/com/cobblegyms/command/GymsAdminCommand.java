package com.cobblegyms.command;

import com.cobblegyms.gui.AdminGui;
import com.cobblegyms.model.BattleFormat;
import com.cobblegyms.model.GymRole;
import com.cobblegyms.model.PokemonType;
import com.cobblegyms.system.*;
import com.cobblegyms.util.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class GymsAdminCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("gymsadmin")
                        .requires(src -> src.hasPermissionLevel(3))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) return 0;
                            AdminGui.open(player);
                            return 1;
                        })

                        // Set gym leader
                        .then(CommandManager.literal("setleader")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("type", StringArgumentType.word())
                                                .then(CommandManager.argument("format", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                            String typeStr = StringArgumentType.getString(ctx, "type");
                                                            String formatStr = StringArgumentType.getString(ctx, "format");
                                                            PokemonType type = PokemonType.fromString(typeStr);
                                                            if (type == null) {
                                                                ctx.getSource().sendError(net.minecraft.text.Text.literal("Invalid type: " + typeStr));
                                                                return 0;
                                                            }
                                                            BattleFormat format = BattleFormat.fromString(formatStr);
                                                            GymManager.getInstance().assignGymLeader(
                                                                    target.getUuid(), target.getName().getString(), type, format, GymRole.GYM_LEADER);
                                                            MessageUtil.sendSuccess(target, "You are now the " + type.getDisplayName() + " Gym Leader!");
                                                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity admin) {
                                                                MessageUtil.sendSuccess(admin, "Set " + target.getName().getString() + " as " + type.getDisplayName() + " Gym Leader.");
                                                            }
                                                            return 1;
                                                        }))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    String typeStr = StringArgumentType.getString(ctx, "type");
                                                    PokemonType type = PokemonType.fromString(typeStr);
                                                    if (type == null) {
                                                        ctx.getSource().sendError(net.minecraft.text.Text.literal("Invalid type: " + typeStr));
                                                        return 0;
                                                    }
                                                    GymManager.getInstance().assignGymLeader(
                                                            target.getUuid(), target.getName().getString(), type, BattleFormat.SINGLES, GymRole.GYM_LEADER);
                                                    MessageUtil.sendSuccess(target, "You are now the " + type.getDisplayName() + " Gym Leader!");
                                                    return 1;
                                                }))))

                        // Set E4
                        .then(CommandManager.literal("sete4")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("type1", StringArgumentType.word())
                                                .then(CommandManager.argument("type2", StringArgumentType.word())
                                                        .then(CommandManager.argument("format", StringArgumentType.word())
                                                                .executes(ctx -> {
                                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                                    PokemonType t1 = PokemonType.fromString(StringArgumentType.getString(ctx, "type1"));
                                                                    PokemonType t2 = PokemonType.fromString(StringArgumentType.getString(ctx, "type2"));
                                                                    BattleFormat fmt = BattleFormat.fromString(StringArgumentType.getString(ctx, "format"));
                                                                    if (t1 == null) { ctx.getSource().sendError(net.minecraft.text.Text.literal("Invalid type1")); return 0; }
                                                                    GymManager.getInstance().assignE4(target.getUuid(), target.getName().getString(), t1, t2, fmt);
                                                                    MessageUtil.sendSuccess(target, "You are now an Elite Four member!");
                                                                    return 1;
                                                                }))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                            PokemonType t1 = PokemonType.fromString(StringArgumentType.getString(ctx, "type1"));
                                                            PokemonType t2 = PokemonType.fromString(StringArgumentType.getString(ctx, "type2"));
                                                            if (t1 == null) { ctx.getSource().sendError(net.minecraft.text.Text.literal("Invalid type1")); return 0; }
                                                            GymManager.getInstance().assignE4(target.getUuid(), target.getName().getString(), t1, t2, BattleFormat.SINGLES);
                                                            MessageUtil.sendSuccess(target, "You are now an Elite Four member!");
                                                            return 1;
                                                        })))))

                        // Set Champion
                        .then(CommandManager.literal("setchampion")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("format", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    BattleFormat fmt = BattleFormat.fromString(StringArgumentType.getString(ctx, "format"));
                                                    GymManager.getInstance().assignChampion(target.getUuid(), target.getName().getString(), fmt);
                                                    MessageUtil.sendSuccess(target, "You are now the Champion!");
                                                    return 1;
                                                }))
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                            GymManager.getInstance().assignChampion(target.getUuid(), target.getName().getString(), BattleFormat.SINGLES);
                                            MessageUtil.sendSuccess(target, "You are now the Champion!");
                                            return 1;
                                        })))

                        // Remove leader
                        .then(CommandManager.literal("removeleader")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                            GymManager.getInstance().removeLeader(target.getUuid());
                                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity admin) {
                                                MessageUtil.sendSuccess(admin, "Removed " + target.getName().getString() + " from gym leadership.");
                                            }
                                            MessageUtil.sendInfo(target, "You have been removed from gym leadership.");
                                            return 1;
                                        })))

                        // Ban player
                        .then(CommandManager.literal("ban")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("hours", IntegerArgumentType.integer(1, 72))
                                                .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                            int hours = IntegerArgumentType.getInteger(ctx, "hours");
                                                            String reason = StringArgumentType.getString(ctx, "reason");
                                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity admin)) return 0;
                                                            GymRole role = GymManager.getInstance().getRole(admin.getUuid());
                                                            GymRole banRole = role != null ? role : GymRole.GYM_LEADER;
                                                            BanManager.getInstance().banFromGym(
                                                                    target.getUuid(), admin.getUuid(), banRole, hours, reason);
                                                            MessageUtil.sendSuccess(admin, "Banned " + target.getName().getString()
                                                                    + " from your gym for " + hours + " hours.");
                                                            return 1;
                                                        }))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                    int hours = IntegerArgumentType.getInteger(ctx, "hours");
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity admin)) return 0;
                                                    BanManager.getInstance().banFromGym(
                                                            target.getUuid(), admin.getUuid(), GymRole.GYM_LEADER, hours, "No reason given");
                                                    MessageUtil.sendSuccess(admin, "Banned " + target.getName().getString() + " for " + hours + " hours.");
                                                    return 1;
                                                }))))

                        // Unban player
                        .then(CommandManager.literal("unban")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity admin)) return 0;
                                            BanManager.getInstance().unbanFromGym(target.getUuid(), admin.getUuid());
                                            MessageUtil.sendSuccess(admin, "Unbanned " + target.getName().getString());
                                            return 1;
                                        })))

                        // End season
                        .then(CommandManager.literal("endseason")
                                .executes(ctx -> {
                                    SeasonManager.getInstance().forceEndSeason();
                                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Season ended forcefully."), true);
                                    return 1;
                                }))

                        // Approve replay
                        .then(CommandManager.literal("approvereplay")
                                .then(CommandManager.argument("battleId", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int battleId = IntegerArgumentType.getInteger(ctx, "battleId");
                                            UUID adminId = ctx.getSource().getEntity() instanceof ServerPlayerEntity p
                                                    ? p.getUuid() : UUID.randomUUID();
                                            BattleManager.getInstance().approveReplay(battleId, adminId);
                                            ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Replay approved for battle " + battleId), true);
                                            return 1;
                                        })))

                        // Import team
                        .then(CommandManager.literal("importteam")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 3))
                                                .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                                            String url = StringArgumentType.getString(ctx, "url");
                                                            TeamManager.getInstance().importFromPokePaste(target.getUuid(), url, slot);
                                                            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity admin) {
                                                                MessageUtil.sendInfo(admin, "Importing team for " + target.getName().getString() + " slot " + slot + "...");
                                                            }
                                                            return 1;
                                                        })))))

                        // Add ban
                        .then(CommandManager.literal("addban")
                                .then(CommandManager.argument("category", StringArgumentType.word())
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String category = StringArgumentType.getString(ctx, "category").toLowerCase();
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    com.cobblegyms.validation.BanListManager mgr = com.cobblegyms.validation.BanListManager.getInstance();
                                                    switch (category) {
                                                        case "pokemon" -> mgr.addPokemonBan(name);
                                                        case "move" -> mgr.addMoveBan(name);
                                                        case "ability" -> mgr.addAbilityBan(name);
                                                        case "item" -> mgr.addItemBan(name);
                                                        default -> {
                                                            ctx.getSource().sendError(net.minecraft.text.Text.literal("Unknown category: " + category));
                                                            return 0;
                                                        }
                                                    }
                                                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Banned " + category + ": " + name), true);
                                                    return 1;
                                                }))))

                        // Remove ban
                        .then(CommandManager.literal("removeban")
                                .then(CommandManager.argument("category", StringArgumentType.word())
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String category = StringArgumentType.getString(ctx, "category").toLowerCase();
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    com.cobblegyms.validation.BanListManager mgr = com.cobblegyms.validation.BanListManager.getInstance();
                                                    switch (category) {
                                                        case "pokemon" -> mgr.removePokemonBan(name);
                                                        case "move" -> mgr.removeMoveBan(name);
                                                        case "ability" -> mgr.removeAbilityBan(name);
                                                        case "item" -> mgr.removeItemBan(name);
                                                        default -> {
                                                            ctx.getSource().sendError(net.minecraft.text.Text.literal("Unknown category: " + category));
                                                            return 0;
                                                        }
                                                    }
                                                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Unbanned " + category + ": " + name), true);
                                                    return 1;
                                                }))))

                        // Set gym extra ban
                        .then(CommandManager.literal("setextraban")
                                .then(CommandManager.argument("leader", EntityArgumentType.player())
                                        .then(CommandManager.argument("pokemon", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity leader = EntityArgumentType.getPlayer(ctx, "leader");
                                                    String pokemon = StringArgumentType.getString(ctx, "pokemon");
                                                    com.cobblegyms.config.SmogonBanConfig.getInstance().setGymExtraBan(leader.getUuid().toString(), pokemon);
                                                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal(
                                                            "Set extra ban for " + leader.getName().getString() + ": " + pokemon), true);
                                                    return 1;
                                                }))))
        );
    }
}
