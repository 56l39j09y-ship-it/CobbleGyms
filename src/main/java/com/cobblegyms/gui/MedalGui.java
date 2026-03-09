package com.cobblegyms.gui;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.PokemonType;
import com.cobblegyms.system.SeasonManager;
import com.cobblegyms.model.Season;
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

public class MedalGui {

    private static final PokemonType[] BADGE_ORDER = PokemonType.values();

    // Maps PokemonType to a representative wool/item
    private static net.minecraft.item.Item getTypeItem(PokemonType type) {
        return switch (type) {
            case FIRE -> Items.RED_WOOL;
            case WATER -> Items.BLUE_WOOL;
            case GRASS -> Items.GREEN_WOOL;
            case ELECTRIC -> Items.YELLOW_WOOL;
            case ICE -> Items.LIGHT_BLUE_WOOL;
            case FIGHTING -> Items.ORANGE_WOOL;
            case POISON -> Items.PURPLE_WOOL;
            case GROUND -> Items.BROWN_WOOL;
            case FLYING -> Items.CYAN_WOOL;
            case PSYCHIC -> Items.PINK_WOOL;
            case BUG -> Items.LIME_WOOL;
            case ROCK -> Items.GRAY_WOOL;
            case GHOST -> Items.PURPLE_WOOL;
            case DRAGON -> Items.BLUE_WOOL;
            case DARK -> Items.BLACK_WOOL;
            case STEEL -> Items.LIGHT_GRAY_WOOL;
            case FAIRY -> Items.MAGENTA_WOOL;
            default -> Items.WHITE_WOOL;
        };
    }

    public static void open(ServerPlayerEntity player) {
        Season season = SeasonManager.getInstance().getCurrentSeason();
        int seasonId = season != null ? season.getId() : 0;

        SimpleInventory inv = new SimpleInventory(54);
        for (int i = 0; i < 54; i++) inv.setStack(i, GuiManager.fillerItem());

        List<String> earnedBadges = DatabaseManager.getInstance().getBadgesForSeason(player.getUuid(), seasonId);

        // Place badge items for each type (slots 10-45)
        int[] slots = {10,11,12,13,14,15,16,
                       19,20,21,22,23,24,25,
                       28,29,30,31};
        for (int i = 0; i < BADGE_ORDER.length && i < slots.length; i++) {
            PokemonType type = BADGE_ORDER[i];
            boolean hasBadge = earnedBadges.contains(type.name());
            String name = hasBadge
                    ? "\u00a7a\u2714 " + type.getColoredName() + "\u00a7a Badge"
                    : "\u00a77\u2718 " + type.getColoredName() + "\u00a77 Badge";
            List<String> lore = hasBadge
                    ? List.of("\u00a7aEarned this season!")
                    : List.of("\u00a77Not yet earned");
            inv.setStack(slots[i], GuiManager.createItem(getTypeItem(type), name, lore));
        }

        // E4 wins indicator
        int e4Wins = DatabaseManager.getInstance().countE4WinsForSeason(player.getUuid(), seasonId);
        inv.setStack(37, GuiManager.createItem(Items.DIAMOND_SWORD,
                "\u00a7b\u00a7lElite Four: \u00a7e" + e4Wins + "/4",
                List.of("\u00a77Defeat all Elite Four members")));

        // Champion win indicator
        // (simplified: check if has champion win via badge-like record)
        inv.setStack(39, GuiManager.createItem(Items.NETHER_STAR,
                "\u00a7e\u00a7lChampion",
                List.of("\u00a77Defeat the Champion to complete your journey")));

        // Season info
        inv.setStack(49, GuiManager.createItem(Items.CLOCK,
                "\u00a77Season " + seasonId,
                List.of("\u00a77Badges: \u00a7e" + earnedBadges.size() + "/18")));

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
                Text.literal("\u00a76\uD83C\uDF96 Your Badges - Season " + seasonId)
        ));
    }
}
