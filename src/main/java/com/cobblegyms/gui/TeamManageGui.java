package com.cobblegyms.gui;

import com.cobblegyms.model.GymLeaderData;
import com.cobblegyms.system.GymManager;
import com.cobblegyms.system.TeamManager;
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

import java.util.*;

public class TeamManageGui {

    private static final Map<UUID, UUID> adminViewingLeader = new HashMap<>();

    public static void open(ServerPlayerEntity admin) {
        List<GymLeaderData> allLeaders = new ArrayList<>(GymManager.getInstance().getAllLeaders().values());
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31};
        for (int i = 0; i < allLeaders.size() && i < slots.length; i++) {
            GymLeaderData leader = allLeaders.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Role: " + leader.getRole().getDisplayName());
            lore.add("\u00a77Type: " + leader.getTypeDisplay());
            lore.add("\u00a77Teams configured: " + leader.getTeamData().stream().filter(Objects::nonNull).count());
            lore.add("");
            lore.add("\u00a7eClick to manage teams");
            inv.setStack(slots[i], GuiManager.createItem(Items.PLAYER_HEAD, "\u00a7f" + leader.getUsername(), lore));
        }

        inv.setStack(49, GuiManager.createItem(Items.ARROW, "\u00a77\u2190 Back", null));

        admin.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (!(clicker instanceof ServerPlayerEntity sp)) return;
                        if (slotIndex == 49) {
                            sp.closeHandledScreen();
                            AdminGui.open(sp);
                            return;
                        }
                        for (int i = 0; i < slots.length && i < allLeaders.size(); i++) {
                            if (slots[i] == slotIndex) {
                                openTeamSlotGui(sp, allLeaders.get(i));
                                return;
                            }
                        }
                    }
                },
                Text.literal("\u00a76\uD83C\uDFAE Team Management")
        ));
    }

    private static void openTeamSlotGui(ServerPlayerEntity admin, GymLeaderData leader) {
        SimpleInventory inv = new SimpleInventory(27);
        for (int i = 0; i < 27; i++) inv.setStack(i, GuiManager.fillerItem());

        for (int slot = 1; slot <= 3; slot++) {
            String teamJson = leader.getTeamForSlot(slot - 1);
            boolean hasTeam = teamJson != null && !teamJson.isEmpty();
            int guiSlot = 10 + (slot - 1) * 2;
            List<String> lore = new ArrayList<>();
            if (hasTeam) {
                List<String> species = com.cobblegyms.team.PokePasteImporter.extractSpeciesNames(teamJson);
                lore.add("\u00a7aTeam configured:");
                species.forEach(s -> lore.add("\u00a77\u2022 " + s));
            } else {
                lore.add("\u00a7cNo team configured");
            }
            lore.add("");
            lore.add("\u00a7eClick to import from PokePaste URL");
            inv.setStack(guiSlot, GuiManager.createItem(
                    hasTeam ? Items.EMERALD : Items.RED_DYE,
                    "\u00a7fTeam Slot " + slot, lore));
        }

        inv.setStack(22, GuiManager.createItem(Items.ARROW, "\u00a77\u2190 Back", null));

        admin.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (!(clicker instanceof ServerPlayerEntity sp)) return;
                        if (slotIndex == 22) { sp.closeHandledScreen(); open(sp); return; }
                        for (int slot2 = 1; slot2 <= 3; slot2++) {
                            if (slotIndex == 10 + (slot2 - 1) * 2) {
                                final int teamSlot = slot2;
                                sp.closeHandledScreen();
                                MessageUtil.sendInfo(sp, "To import team for " + leader.getUsername() + " slot " + teamSlot
                                        + ", use: /gymsadmin importteam " + leader.getUsername() + " " + teamSlot + " <pokepaste_url>");
                                return;
                            }
                        }
                    }
                },
                Text.literal("\u00a76Teams for " + leader.getUsername())
        ));
    }

    private static final int[] teamMgmtSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31};
}
