package com.cobblegyms.gui;

import com.cobblegyms.validation.BanListManager;
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
import java.util.Set;
import java.util.stream.Collectors;

public class RulesGui {

    public static void open(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        BanListManager manager = BanListManager.getInstance();

        // Banned Pokémon
        Set<String> bannedPokemon = manager.getBannedPokemon();
        List<String> pokemonLore = formatSet(bannedPokemon, 30);
        inv.setStack(10, GuiManager.createItem(Items.BARRIER,
                "\u00a7c\uD83D\uDEAB Banned Pokémon (" + bannedPokemon.size() + ")",
                pokemonLore));

        // Banned Moves
        Set<String> bannedMoves = manager.getBannedMoves();
        List<String> movesLore = formatSet(bannedMoves, 20);
        inv.setStack(12, GuiManager.createItem(Items.BLAZE_POWDER,
                "\u00a7c\uD83D\uDEAB Banned Moves (" + bannedMoves.size() + ")",
                movesLore));

        // Banned Abilities
        Set<String> bannedAbilities = manager.getBannedAbilities();
        List<String> abilitiesLore = formatSet(bannedAbilities, 10);
        inv.setStack(14, GuiManager.createItem(Items.ENCHANTED_BOOK,
                "\u00a7c\uD83D\uDEAB Banned Abilities (" + bannedAbilities.size() + ")",
                abilitiesLore));

        // Banned Items
        Set<String> bannedItems = manager.getBannedItems();
        List<String> itemsLore = formatSet(bannedItems, 10);
        inv.setStack(16, GuiManager.createItem(Items.GOLDEN_APPLE,
                "\u00a7c\uD83D\uDEAB Banned Items (" + bannedItems.size() + ")",
                itemsLore));

        // General rules
        inv.setStack(22, GuiManager.createItem(Items.WRITTEN_BOOK,
                "\u00a7eGeneral Rules",
                List.of(
                        "\u00a77\u2022 No OHKO moves (Fissure, etc.)",
                        "\u00a77\u2022 No Evasion moves (Double Team, etc.)",
                        "\u00a77\u2022 No Moody ability",
                        "\u00a77\u2022 No Baton Pass chains",
                        "\u00a77\u2022 Standard Smogon clauses apply",
                        "\u00a77\u2022 Species Clause: No duplicate species",
                        "\u00a77\u2022 Sleep Clause: One sleep at a time"
                )));

        // Back
        inv.setStack(49, GuiManager.createItem(Items.ARROW, "\u00a77\u2190 Back", null));

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
                            GymsGui.open(sp);
                        }
                    }
                },
                Text.literal("\u00a7c\uD83D\uDCD6 Rules & Ban List")
        ));
    }

    private static List<String> formatSet(Set<String> set, int maxDisplay) {
        List<String> lore = new ArrayList<>();
        List<String> sorted = set.stream().sorted().collect(Collectors.toList());
        int count = Math.min(sorted.size(), maxDisplay);
        for (int i = 0; i < count; i++) {
            lore.add("\u00a7c\u2022 \u00a7f" + capitalize(sorted.get(i)));
        }
        if (sorted.size() > maxDisplay) {
            lore.add("\u00a77... and " + (sorted.size() - maxDisplay) + " more");
        }
        return lore;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
