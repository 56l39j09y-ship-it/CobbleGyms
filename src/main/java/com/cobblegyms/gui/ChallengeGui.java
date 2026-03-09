package com.cobblegyms.gui;

import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.system.*;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class ChallengeGui {

    public static void open(ServerPlayerEntity player) {
        GymLeaderData leader = GymManager.getInstance().getLeader(player.getUuid());
        if (leader == null) {
            MessageUtil.sendError(player, "You are not a gym leader, E4, or Champion.");
            return;
        }

        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        boolean isOpen = leader.isActive();
        int queueSize = QueueManager.getInstance().getQueue(player.getUuid()).size();

        // Open/Close toggle
        inv.setStack(10, GuiManager.createItem(
                isOpen ? Items.GREEN_WOOL : Items.RED_WOOL,
                isOpen ? "\u00a7a\u2714 Gym is OPEN" : "\u00a7c\u2718 Gym is CLOSED",
                List.of(isOpen ? "\u00a77Click to CLOSE your gym" : "\u00a77Click to OPEN your gym")));

        // Equip/Unequip team
        inv.setStack(12, GuiManager.createItem(Items.IRON_CHESTPLATE,
                "\u00a76\uD83D\uDC55 Equip Gym Team",
                List.of("\u00a77Save your personal team", "\u00a77and equip your gym team")));

        // Start next battle
        inv.setStack(14, GuiManager.createItem(Items.EMERALD,
                "\u00a7a\u25BA Start Next Battle",
                List.of("\u00a77Queue size: \u00a7e" + queueSize,
                        "\u00a77Start battle with next challenger")));

        // Cancel current battle
        inv.setStack(16, GuiManager.createItem(Items.BARRIER,
                "\u00a7c\u26D4 Cancel Current Battle",
                List.of("\u00a77Cancel the currently active battle")));

        // Cancel all pending
        inv.setStack(19, GuiManager.createItem(Items.TNT,
                "\u00a7c\uD83D\uDDD1 Cancel All Pending",
                List.of("\u00a77Cancel all waiting challengers")));

        // View battle records
        inv.setStack(21, GuiManager.createItem(Items.BOOK,
                "\u00a7f\uD83D\uDCCB Battle Records",
                List.of("\u00a77View your recent battle history")));

        // Weekly stats
        inv.setStack(23, GuiManager.createItem(Items.PAPER,
                "\u00a77\uD83D\uDCCA Weekly Stats",
                List.of("\u00a77View your weekly statistics")));

        // Rankings
        inv.setStack(25, GuiManager.createItem(Items.GOLD_INGOT,
                "\u00a76\uD83C\uDFC5 Rankings",
                List.of("\u00a77View the leaderboard")));

        // Ban player
        inv.setStack(28, GuiManager.createItem(Items.RED_DYE,
                "\u00a7c\uD83D\uDEAB Ban Player",
                List.of("\u00a77Temporarily ban a player", "\u00a77from your gym")));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (!(clicker instanceof ServerPlayerEntity sp)) return;
                        handleClick(sp, slotIndex);
                    }
                },
                Text.literal("\u00a7c\u2694 Challenge Menu - " + leader.getUsername())
        ));
    }

    private static void handleClick(ServerPlayerEntity player, int slot) {
        GymLeaderData leader = GymManager.getInstance().getLeader(player.getUuid());
        if (leader == null) return;

        switch (slot) {
            case 10 -> {
                if (leader.isActive()) {
                    GymManager.getInstance().closeGym(player.getUuid());
                } else {
                    GymManager.getInstance().openGym(player.getUuid());
                }
                player.closeHandledScreen();
                open(player);
            }
            case 12 -> {
                player.closeHandledScreen();
                TeamManager.getInstance().equipGymTeam(player.getUuid());
            }
            case 14 -> {
                player.closeHandledScreen();
                QueueManager.getInstance().startNextBattle(player.getUuid());
            }
            case 16 -> {
                var active = QueueManager.getInstance().getActiveEntry(player.getUuid());
                if (active != null) {
                    QueueManager.getInstance().markDone(active.getId());
                    MessageUtil.sendInfo(player, "Current battle cancelled.");
                } else {
                    MessageUtil.sendInfo(player, "No active battle to cancel.");
                }
                player.closeHandledScreen();
            }
            case 19 -> {
                player.closeHandledScreen();
                QueueManager.getInstance().cancelQueue(player.getUuid());
            }
            case 21 -> {
                player.closeHandledScreen();
                RecordsGui.open(player, player.getUuid());
            }
            case 23 -> {
                player.closeHandledScreen();
                StatsGui.open(player, player.getUuid());
            }
            case 25 -> {
                player.closeHandledScreen();
                RankingGui.open(player);
            }
            case 28 -> {
                player.closeHandledScreen();
                MessageUtil.sendInfo(player, "To ban a player, use: /gymsadmin ban <player> <hours> [reason]");
            }
        }
    }
}
