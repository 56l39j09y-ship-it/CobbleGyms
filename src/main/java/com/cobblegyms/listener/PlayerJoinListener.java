package com.cobblegyms.listener;

import com.cobblegyms.system.BattleManager;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.system.QueueManager;
import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.util.MessageUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerJoinListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleGyms");

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            onPlayerJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            onPlayerLeave(player);
        });
    }

    private static void onPlayerJoin(ServerPlayerEntity player) {
        try {
            GymLeaderData leaderData = GymManager.getInstance().getLeader(player.getUuid());
            if (leaderData != null) {
                if (!leaderData.isActive()) {
                    MessageUtil.sendInfo(player, "\u00a7eReminder: Your gym is currently CLOSED. Use /challenge to open it.");
                }
                int queueSize = QueueManager.getInstance().getQueue(player.getUuid()).size();
                if (queueSize > 0) {
                    MessageUtil.sendInfo(player, "\u00a7eYou have \u00a76" + queueSize
                            + "\u00a7e challenger(s) in your queue!");
                }
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Error on player join for {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    private static void onPlayerLeave(ServerPlayerEntity player) {
        try {
            // If a gym leader disconnects during an active battle, handle the disconnect
            if (GymManager.getInstance().isGymLeader(player.getUuid())) {
                var activeBattle = BattleManager.getInstance().getActiveBattle(player.getUuid());
                if (activeBattle != null) {
                    int turns = BattleManager.getInstance().getTurns(player.getUuid(), activeBattle);
                    if (turns <= 1) {
                        // Disconnect before/at turn 1 → replay
                        LOGGER.info("[CobbleGyms] Gym leader {} disconnected at turn {}, flagging for replay",
                                player.getUuid(), turns);
                        BattleManager.getInstance().onBattleEnd(player.getUuid(), activeBattle,
                                "cancelled", turns, "", "");
                    } else {
                        // Disconnect after turn 1 → challenger wins
                        LOGGER.info("[CobbleGyms] Gym leader {} disconnected after turn 1, challenger wins",
                                player.getUuid());
                        BattleManager.getInstance().onBattleEnd(player.getUuid(), activeBattle,
                                "loss", turns, "", "");
                    }
                }
                GymManager.getInstance().closeGym(player.getUuid());
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleGyms] Error on player disconnect for {}: {}", player.getName().getString(), e.getMessage());
        }
    }
}
