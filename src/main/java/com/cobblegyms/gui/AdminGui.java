package com.cobblegyms.gui;

import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.model.GymRole;
import com.cobblegyms.model.PokemonType;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.system.SeasonManager;
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

public class AdminGui {

    public static void open(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        inv.setStack(10, GuiManager.createItem(Items.IRON_SWORD,
                "\u00a7c\u2694 Manage Gym Leaders",
                List.of("\u00a77Assign/remove gym leaders", "\u00a77Set types and formats")));

        inv.setStack(12, GuiManager.createItem(Items.DIAMOND_SWORD,
                "\u00a7b\uD83D\uDC8E Manage Elite Four",
                List.of("\u00a77Assign/remove E4 members")));

        inv.setStack(14, GuiManager.createItem(Items.NETHER_STAR,
                "\u00a7e\u2B50 Manage Champion",
                List.of("\u00a77Assign/remove Champion")));

        inv.setStack(16, GuiManager.createItem(Items.BARRIER,
                "\u00a7c\uD83D\uDEAB Smogon Ban Lists",
                List.of("\u00a77Manage banned Pokémon,", "\u00a77moves, abilities, items")));

        inv.setStack(28, GuiManager.createItem(Items.CLOCK,
                "\u00a77\uD83D\uDDD3 Season Management",
                List.of("\u00a77View/end current season")));

        inv.setStack(30, GuiManager.createItem(Items.WRITTEN_BOOK,
                "\u00a7f\uD83D\uDCCB Battle Records",
                List.of("\u00a77View all battle records")));

        inv.setStack(32, GuiManager.createItem(Items.CHEST,
                "\u00a7a\uD83C\uDF81 Rewards Config",
                List.of("\u00a77Configure season rewards")));

        inv.setStack(34, GuiManager.createItem(Items.GOLD_BLOCK,
                "\u00a76\uD83C\uDFC5 View Rankings",
                List.of("\u00a77View the leaderboard")));

        inv.setStack(46, GuiManager.createItem(Items.PAPER,
                "\u00a77\uD83D\uDCCA Team Management",
                List.of("\u00a77Import/manage gym teams")));

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
                Text.literal("\u00a7c\u2699 Admin Panel")
        ));
    }

    private static void handleClick(ServerPlayerEntity player, int slot) {
        switch (slot) {
            case 10 -> {
                player.closeHandledScreen();
                MessageUtil.sendInfo(player, "\u00a7eGym Leader Commands:");
                MessageUtil.sendInfo(player, "\u00a77/gymsadmin setleader <player> <type> [singles|doubles]");
                MessageUtil.sendInfo(player, "\u00a77/gymsadmin removeleader <player>");
            }
            case 12 -> {
                player.closeHandledScreen();
                MessageUtil.sendInfo(player, "\u00a7eElite Four Commands:");
                MessageUtil.sendInfo(player, "\u00a77/gymsadmin sete4 <player> <type1> <type2> [singles|doubles]");
            }
            case 14 -> {
                player.closeHandledScreen();
                MessageUtil.sendInfo(player, "\u00a7eChampion Commands:");
                MessageUtil.sendInfo(player, "\u00a77/gymsadmin setchampion <player> [singles|doubles]");
            }
            case 16 -> {
                player.closeHandledScreen();
                RulesGui.open(player);
            }
            case 28 -> {
                player.closeHandledScreen();
                var season = SeasonManager.getInstance().getCurrentSeason();
                MessageUtil.sendInfo(player, "\u00a7eSeason: " + (season != null ? season.getId() : "None")
                        + " | Remaining: " + SeasonManager.getInstance().getRemainingTime());
                MessageUtil.sendInfo(player, "\u00a77Use /gymsadmin endseason to force end the current season.");
            }
            case 30 -> {
                player.closeHandledScreen();
                RecordsGui.openAll(player);
            }
            case 32 -> {
                player.closeHandledScreen();
                MessageUtil.sendInfo(player, "\u00a7eReward commands:");
                MessageUtil.sendInfo(player, "\u00a77Edit config/cobblegyms/config.json to change reward commands.");
            }
            case 34 -> {
                player.closeHandledScreen();
                RankingGui.open(player);
            }
            case 46 -> {
                player.closeHandledScreen();
                TeamManageGui.open(player);
            }
        }
    }
}
