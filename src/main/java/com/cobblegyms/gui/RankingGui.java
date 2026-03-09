package com.cobblegyms.gui;

import com.cobblegyms.system.RankingManager;
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

public class RankingGui {

    public static void open(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        List<RankingManager.RankEntry> gymRanks = RankingManager.getInstance().getTopGymLeaders(9);
        List<RankingManager.RankEntry> e4Ranks = RankingManager.getInstance().getTopE4AndChampion(4);

        // Gym leaders section
        inv.setStack(4, GuiManager.createItem(Items.IRON_SWORD,
                "\u00a7c\u2694 Top Gym Leaders", null));

        int[] gymSlots = {10,11,12,13,14,15,16,19,20};
        for (int i = 0; i < gymRanks.size() && i < gymSlots.length; i++) {
            RankingManager.RankEntry entry = gymRanks.get(i);
            String medal = i == 0 ? "\u00a7e\uD83E\uDD47" : i == 1 ? "\u00a77\uD83E\uDD48" : i == 2 ? "\u00a76\uD83E\uDD49" : "\u00a77#" + (i + 1);
            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Battles: \u00a7e" + entry.battles());
            lore.add("\u00a77Wins: \u00a7a" + entry.wins());
            lore.add("\u00a77Losses: \u00a7c" + entry.losses());
            lore.add("\u00a77Win Rate: \u00a7e" + entry.getWinratePercent());
            inv.setStack(gymSlots[i], GuiManager.createItem(Items.PLAYER_HEAD,
                    medal + " " + entry.username(), lore));
        }

        // E4/Champion section
        inv.setStack(26, GuiManager.createItem(Items.DIAMOND_SWORD,
                "\u00a7b\uD83D\uDC8E Top E4 & Champion", null));

        int[] e4Slots = {34, 35, 43, 44};
        for (int i = 0; i < e4Ranks.size() && i < e4Slots.length; i++) {
            RankingManager.RankEntry entry = e4Ranks.get(i);
            String medal = i == 0 ? "\u00a7e\uD83E\uDD47" : "\u00a77#" + (i + 1);
            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Battles: \u00a7e" + entry.battles());
            lore.add("\u00a77Wins: \u00a7a" + entry.wins());
            lore.add("\u00a77Win Rate: \u00a7e" + entry.getWinratePercent());
            inv.setStack(e4Slots[i], GuiManager.createItem(Items.PLAYER_HEAD,
                    medal + " " + entry.username(), lore));
        }

        // Player's own rank
        RankingManager.RankEntry myRank = RankingManager.getInstance().getPlayerRanking(player.getUuid());
        List<String> myLore = new ArrayList<>();
        myLore.add("\u00a77Your stats this week:");
        myLore.add("\u00a77Battles: \u00a7e" + myRank.battles());
        myLore.add("\u00a77Win Rate: \u00a7e" + myRank.getWinratePercent());
        inv.setStack(49, GuiManager.createItem(Items.GOLD_INGOT, "\u00a76\uD83D\uDCCA My Stats", myLore));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (slotIndex == 49 && clicker instanceof ServerPlayerEntity sp) {
                            sp.closeHandledScreen();
                        }
                    }
                },
                Text.literal("\u00a76\uD83C\uDFC5 Rankings")
        ));
    }
}
