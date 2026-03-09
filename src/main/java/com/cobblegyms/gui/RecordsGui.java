package com.cobblegyms.gui;

import com.cobblegyms.model.BattleRecord;
import com.cobblegyms.system.BattleManager;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RecordsGui {

    public static void open(ServerPlayerEntity player, UUID leaderId) {
        List<BattleRecord> records = BattleManager.getInstance().getBattleHistory(leaderId, 10);
        openWithRecords(player, records, "\u00a7f\uD83D\uDCCB Battle Records");
    }

    public static void openAll(ServerPlayerEntity player) {
        List<BattleRecord> records = com.cobblegyms.database.DatabaseManager.getInstance().getAllBattleRecords(10);
        openWithRecords(player, records, "\u00a7f\uD83D\uDCCB All Battle Records");
    }

    private static void openWithRecords(ServerPlayerEntity player, List<BattleRecord> records, String title) {
        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < records.size() && i < slots.length; i++) {
            BattleRecord record = records.get(i);
            boolean win = record.isWin();
            String emoji = win ? "\u00a7a\u2714" : "\u00a7c\u2718";
            String resultText = win ? "\u00a7aWIN" : "\u00a7cLOSS";

            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Challenger: \u00a7f" + record.getChallengerName());
            lore.add("\u00a77Result: " + resultText);
            lore.add("\u00a77Turns: \u00a7e" + record.getTurns());
            lore.add("\u00a77Date: \u00a77" + new Date(record.getTimestamp()).toString());
            if (record.isCanReplay()) {
                lore.add("\u00a7eReplay available (battle < 2 turns)");
            }

            inv.setStack(slots[i], GuiManager.createItem(
                    win ? Items.LIME_DYE : Items.RED_DYE,
                    emoji + " " + record.getChallengerName() + " - " + resultText,
                    lore));
        }

        if (records.isEmpty()) {
            inv.setStack(22, GuiManager.createItem(Items.BARRIER,
                    "\u00a77No records found",
                    List.of("\u00a77No battles have been recorded yet.")));
        }

        // Back button
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
                        }
                    }
                },
                Text.literal(title)
        ));
    }
}
