package com.cobblegyms.gui;

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
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.BiConsumer;

public class GuiManager {
    private static GuiManager instance;

    private GuiManager() {}

    public static GuiManager getInstance() {
        if (instance == null) instance = new GuiManager();
        return instance;
    }

    public void openGymsMenu(ServerPlayerEntity player) {
        GymsGui.open(player);
    }

    public void openChallengeMenu(ServerPlayerEntity player) {
        ChallengeGui.open(player);
    }

    public void openAdminMenu(ServerPlayerEntity player) {
        AdminGui.open(player);
    }

    public static ItemStack createItem(net.minecraft.item.Item item, String name, List<String> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(net.minecraft.component.DataComponentTypes.ITEM_NAME, Text.literal(name));
        if (lore != null && !lore.isEmpty()) {
            var loreComp = new net.minecraft.component.type.LoreComponent(
                    lore.stream().map(l -> Text.literal(l).formatted(Formatting.GRAY)).toList()
            );
            stack.set(net.minecraft.component.DataComponentTypes.LORE, loreComp);
        }
        return stack;
    }

    public static ItemStack createItem(net.minecraft.item.Item item, String name) {
        return createItem(item, name, null);
    }

    public static ItemStack fillerItem() {
        ItemStack gray = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        gray.set(net.minecraft.component.DataComponentTypes.ITEM_NAME, Text.literal(" "));
        return gray;
    }

    public static void openChestGui(ServerPlayerEntity player, String title, int rows,
                                     ItemStack[] contents, BiConsumer<Integer, PlayerEntity> clickHandler) {
        SimpleInventory inventory = new SimpleInventory(rows * 9) {
            @Override
            public boolean canPlayerUse(PlayerEntity player) { return true; }
        };

        for (int i = 0; i < contents.length && i < rows * 9; i++) {
            if (contents[i] != null) inventory.setStack(i, contents[i]);
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> {
                    ScreenHandlerType<?> type = switch (rows) {
                        case 1 -> ScreenHandlerType.GENERIC_9X1;
                        case 2 -> ScreenHandlerType.GENERIC_9X2;
                        case 3 -> ScreenHandlerType.GENERIC_9X3;
                        case 4 -> ScreenHandlerType.GENERIC_9X4;
                        case 5 -> ScreenHandlerType.GENERIC_9X5;
                        default -> ScreenHandlerType.GENERIC_9X6;
                    };
                    return new GenericContainerScreenHandler(type, syncId, playerInv, inventory, rows) {
                        @Override
                        public ItemStack quickMove(PlayerEntity player, int slot) {
                            return ItemStack.EMPTY;
                        }

                        @Override
                        public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
                            if (slotIndex >= 0 && slotIndex < rows * 9) {
                                clickHandler.accept(slotIndex, player);
                            }
                        }
                    };
                },
                Text.literal(title)
        ));
    }
}
