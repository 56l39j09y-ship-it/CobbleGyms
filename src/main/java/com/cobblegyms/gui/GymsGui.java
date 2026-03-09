package com.cobblegyms.gui;

import com.cobblegyms.system.SeasonManager;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.system.ValidationManager;
import com.cobblegyms.util.MessageUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class GymsGui {

    public static void open(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);
        fillFiller(inv);

        // Slot 10: My Badges
        inv.setStack(10, GuiManager.createItem(Items.GOLD_INGOT, "\u00a76\u00a7l\uD83C\uDF96 My Badges",
                List.of("\u00a77View your earned gym badges")));

        // Slot 12: Challenge Gym Leader
        inv.setStack(12, GuiManager.createItem(Items.IRON_SWORD, "\u00a7c\u00a7l\u2694 Challenge Gym Leader",
                List.of("\u00a77Challenge a gym leader", "\u00a77" + GymManager.getInstance().getAllGymLeaders().size() + " leaders available")));

        // Slot 14: Challenge Elite Four
        inv.setStack(14, GuiManager.createItem(Items.DIAMOND_SWORD, "\u00a7b\u00a7l\uD83D\uDC8E Challenge Elite Four",
                List.of("\u00a77Challenge the Elite Four", "\u00a77Requires all 18 badges")));

        // Slot 16: Challenge Champion
        inv.setStack(16, GuiManager.createItem(Items.NETHER_STAR, "\u00a7e\u00a7l\u2B50 Challenge Champion",
                List.of("\u00a77Challenge the Champion", "\u00a77Requires 4 E4 victories")));

        // Slot 28: Season Info
        String timeLeft = SeasonManager.getInstance().getRemainingTime();
        inv.setStack(28, GuiManager.createItem(Items.CLOCK, "\u00a77\uD83D\uDDD3 Season Info",
                List.of("\u00a77Time remaining: \u00a7e" + timeLeft)));

        // Slot 30: Team Validation
        inv.setStack(30, GuiManager.createItem(Items.SHIELD, "\u00a7a\u00a7l\uD83D\uDEE1 Validate Team",
                List.of("\u00a77Check your team for rule violations")));

        // Slot 32: Rules
        inv.setStack(32, GuiManager.createItem(Items.WRITTEN_BOOK, "\u00a7f\uD83D\uDCD6 Rules & Ban List",
                List.of("\u00a77View banned Pokémon, moves, and items")));

        // Slot 34: Queue Status
        inv.setStack(34, GuiManager.createItem(Items.HOPPER, "\u00a77\uD83D\uDD50 Queue Status",
                List.of("\u00a77Check your current queue position")));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (!(clicker instanceof ServerPlayerEntity serverPlayer)) return;
                        handleClick(serverPlayer, slotIndex);
                    }
                },
                Text.literal("\u00a76\u00a7l\uD83C\uDFC6 CobbleGyms")
        ));
    }

    private static void handleClick(ServerPlayerEntity player, int slot) {
        switch (slot) {
            case 10 -> {
                player.closeHandledScreen();
                MedalGui.open(player);
            }
            case 12 -> {
                player.closeHandledScreen();
                GymLeaderListGui.open(player);
            }
            case 14 -> {
                player.closeHandledScreen();
                GymLeaderListGui.openE4List(player);
            }
            case 16 -> {
                player.closeHandledScreen();
                GymLeaderListGui.openChampionChallenge(player);
            }
            case 28 -> {
                // Show season info in chat
                MessageUtil.sendInfo(player, "Season " + (SeasonManager.getInstance().getCurrentSeason() != null
                        ? SeasonManager.getInstance().getCurrentSeason().getId() : "N/A")
                        + " | Time remaining: " + SeasonManager.getInstance().getRemainingTime());
            }
            case 30 -> {
                player.closeHandledScreen();
                ValidationManager.getInstance().validateTeam(player, null);
            }
            case 32 -> {
                player.closeHandledScreen();
                RulesGui.open(player);
            }
            case 34 -> {
                MessageUtil.sendInfo(player, "Use /gyms to check your queue position in specific gyms.");
            }
        }
    }

    private static void fillFiller(SimpleInventory inv) {
        ItemStack filler = GuiManager.fillerItem();
        for (int i = 0; i < 54; i++) {
            if (shouldFill(i)) inv.setStack(i, filler);
        }
    }

    private static boolean shouldFill(int slot) {
        return slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8;
    }
}
