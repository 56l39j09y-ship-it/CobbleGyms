package com.cobblegyms.gui;

import com.cobblegyms.model.WeeklyStats;
import com.cobblegyms.system.RewardManager;
import com.cobblegyms.config.CobbleGymsConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsGui {

    public static void open(ServerPlayerEntity player, UUID leaderId) {
        WeeklyStats stats = RewardManager.getInstance().getWeeklyStats(leaderId);
        CobbleGymsConfig config = CobbleGymsConfig.getInstance();

        SimpleInventory inv = new SimpleInventory(27);
        for (int i = 0; i < 27; i++) inv.setStack(i, GuiManager.fillerItem());

        boolean qualifies = stats.getBattles() >= config.minBattlesForReward
                && stats.getWinrate() >= config.minWinrateForReward;

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77Battles this week: \u00a7e" + stats.getBattles());
        lore.add("\u00a77Wins: \u00a7a" + stats.getWins());
        lore.add("\u00a77Losses: \u00a7c" + stats.getLosses());
        lore.add("\u00a77Win Rate: \u00a7e" + stats.getWinratePercent());
        lore.add("");
        lore.add("\u00a77Required for reward:");
        lore.add("\u00a77  Battles: " + (stats.getBattles() >= config.minBattlesForReward ? "\u00a7a" : "\u00a7c")
                + stats.getBattles() + "/" + config.minBattlesForReward);
        lore.add("\u00a77  Win Rate: " + (stats.getWinrate() >= config.minWinrateForReward ? "\u00a7a" : "\u00a7c")
                + stats.getWinratePercent() + "/" + String.format("%.0f%%", config.minWinrateForReward * 100));
        lore.add("");
        lore.add(qualifies ? "\u00a7a\u2714 You qualify for the weekly reward!" : "\u00a7c\u2718 Not yet qualifying");

        inv.setStack(13, GuiManager.createItem(Items.PAPER,
                "\u00a77\uD83D\uDCCA Weekly Stats", lore));

        inv.setStack(22, GuiManager.createItem(Items.ARROW, "\u00a77\u2190 Back", null));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (slotIndex == 22 && clicker instanceof ServerPlayerEntity sp) {
                            sp.closeHandledScreen();
                        }
                    }
                },
                Text.literal("\u00a77\uD83D\uDCCA Weekly Stats")
        ));
    }
}
