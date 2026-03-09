package com.cobblegyms.gui;

import com.cobblegyms.database.DatabaseManager;
import com.cobblegyms.model.*;
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

import java.util.ArrayList;
import java.util.List;

public class GymLeaderListGui {

    public static void open(ServerPlayerEntity player) {
        List<GymLeaderData> leaders = GymManager.getInstance().getAllGymLeaders();
        openList(player, leaders, "\u00a7c\u2694 Gym Leaders");
    }

    public static void openE4List(ServerPlayerEntity player) {
        Season season = SeasonManager.getInstance().getCurrentSeason();
        int seasonId = season != null ? season.getId() : 0;
        int badges = DatabaseManager.getInstance().countBadgesForSeason(player.getUuid(), seasonId);
        int totalGyms = GymManager.getInstance().getAllGymLeaders().size();

        if (badges < totalGyms) {
            MessageUtil.sendError(player, "You need all " + totalGyms + " gym badges to challenge the Elite Four! You have " + badges + ".");
            return;
        }
        List<GymLeaderData> e4 = GymManager.getInstance().getAllE4();
        openList(player, e4, "\u00a7b\uD83D\uDC8E Elite Four");
    }

    public static void openChampionChallenge(ServerPlayerEntity player) {
        Season season = SeasonManager.getInstance().getCurrentSeason();
        int seasonId = season != null ? season.getId() : 0;
        int e4Wins = DatabaseManager.getInstance().countE4WinsForSeason(player.getUuid(), seasonId);
        int totalE4 = GymManager.getInstance().getAllE4().size();

        if (e4Wins < totalE4) {
            MessageUtil.sendError(player, "You need to defeat all " + totalE4 + " Elite Four members first! You have " + e4Wins + "/" + totalE4 + ".");
            return;
        }
        var champion = GymManager.getInstance().getChampion();
        if (champion.isEmpty()) {
            MessageUtil.sendInfo(player, "There is no Champion yet.");
            return;
        }
        List<GymLeaderData> champ = List.of(champion.get());
        openList(player, champ, "\u00a7e\u2B50 Champion");
    }

    private static void openList(ServerPlayerEntity player, List<GymLeaderData> leaders, String title) {
        int rows = Math.max(3, Math.min(6, (leaders.size() / 7) + 2));
        SimpleInventory inv = new SimpleInventory(rows * 9);
        for (int i = 0; i < rows * 9; i++) inv.setStack(i, GuiManager.fillerItem());

        List<GymLeaderData> leaderList = new ArrayList<>(leaders);
        for (int i = 0; i < leaderList.size() && i < 45; i++) {
            GymLeaderData leader = leaderList.get(i);
            int slot = 10 + (i % 7) + (i / 7) * 9;
            if (slot >= rows * 9) break;

            boolean online = player.getServer() != null
                    && player.getServer().getPlayerManager().getPlayer(leader.getLeaderId()) != null;
            boolean open = leader.isActive();

            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Type: " + leader.getTypeDisplay());
            lore.add("\u00a77Format: " + leader.getFormat().getDisplayName());
            lore.add("\u00a77Status: " + (open ? "\u00a7aOPEN" : "\u00a7cCLOSED"));
            lore.add("\u00a77Online: " + (online ? "\u00a7aYes" : "\u00a7cNo"));
            int queueSize = QueueManager.getInstance().getQueue(leader.getLeaderId()).size();
            lore.add("\u00a77Queue: \u00a7e" + queueSize + " waiting");
            lore.add("");
            if (open && online) {
                lore.add("\u00a7aClick to join queue!");
            } else {
                lore.add("\u00a7cThis gym is unavailable.");
            }

            String statusColor = open && online ? "\u00a7a" : "\u00a7c";
            ItemStack item = GuiManager.createItem(Items.PLAYER_HEAD,
                    statusColor + leader.getUsername(), lore);
            inv.setStack(slot, item);
        }

        // Back button
        inv.setStack(rows * 9 - 5, GuiManager.createItem(Items.ARROW, "\u00a77\u2190 Back", List.of("\u00a77Return to main menu")));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        getScreenType(rows), syncId, playerInv, inv, rows) {
                    @Override
                    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity clicker) {
                        if (!(clicker instanceof ServerPlayerEntity sp)) return;
                        if (slotIndex == rows * 9 - 5) {
                            sp.closeHandledScreen();
                            GymsGui.open(sp);
                            return;
                        }
                        handleLeaderClick(sp, slotIndex, leaderList, rows);
                    }
                },
                Text.literal(title)
        ));
    }

    private static void handleLeaderClick(ServerPlayerEntity player, int slot, List<GymLeaderData> leaders, int rows) {
        int idx = -1;
        for (int i = 0; i < leaders.size(); i++) {
            int expectedSlot = 10 + (i % 7) + (i / 7) * 9;
            if (expectedSlot == slot) { idx = i; break; }
        }
        if (idx < 0 || idx >= leaders.size()) return;

        GymLeaderData leader = leaders.get(idx);
        if (!leader.isActive()) {
            MessageUtil.sendError(player, "That gym is currently closed!");
            return;
        }
        if (player.getServer().getPlayerManager().getPlayer(leader.getLeaderId()) == null) {
            MessageUtil.sendError(player, "That gym leader is offline!");
            return;
        }
        if (BanManager.getInstance().isBanned(player.getUuid(), leader.getLeaderId())) {
            MessageUtil.sendError(player, "You are temporarily banned from this gym!");
            return;
        }

        // Validate team
        boolean valid = ValidationManager.getInstance().isTeamValid(player, leader.getLeaderId());
        if (!valid) {
            player.closeHandledScreen();
            ValidationManager.getInstance().validateTeam(player, leader.getLeaderId());
            return;
        }

        player.closeHandledScreen();
        int queueId = QueueManager.getInstance().addToQueue(leader.getLeaderId(),
                player.getUuid(), player.getName().getString());
        if (queueId < 0) {
            MessageUtil.sendError(player, "You are already in a queue!");
        }
    }

    private static ScreenHandlerType<?> getScreenType(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }
}
