package com.cobblegyms.gui

import com.cobblegyms.managers.BattleManager
import com.cobblegyms.managers.ChampionManager
import com.cobblegyms.managers.E4Manager
import com.cobblegyms.managers.GymManager
import com.cobblegyms.managers.SeasonManager
import com.cobblegyms.utils.MessageUtils
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

/**
 * Server-side inventory GUIs for CobbleGyms.
 *
 * Each menu is a simple chest-based GUI opened via
 * [ServerPlayerEntity.openHandledScreen].
 *
 * Menus provided:
 *  - [openMainMenu]    : overview (leaders, E4, champion, leaderboard)
 *  - [openGymMenu]     : list of gym leaders with their stats
 *  - [openE4Menu]      : list of Elite Four members
 *  - [openAdminMenu]   : admin overview (op-only)
 */
object GymsMenuGUI {

    // -------------------------------------------------------------------------
    // Menu openers
    // -------------------------------------------------------------------------

    fun openMainMenu(
        player: ServerPlayerEntity,
        gymManager: GymManager,
        e4Manager: E4Manager,
        championManager: ChampionManager,
        seasonManager: SeasonManager
    ) {
        val inventory = SimpleInventory(54)
        val title = Text.literal("§6✦ CobbleGyms ✦")

        // Fill borders with glass panes
        fillBorders(inventory, 54)

        // Slot 10: Gym Leaders
        inventory.setStack(
            10,
            namedItem(Items.GOLDEN_SWORD, "§e⚔ Gym Leaders", listOf(
                "§7View all active Gym Leaders",
                "§7Active: §a${gymManager.getActiveLeaders().size}"
            ))
        )

        // Slot 12: Elite Four
        inventory.setStack(
            12,
            namedItem(Items.AMETHYST_SHARD, "§5★ Elite Four", listOf(
                "§7View all Elite Four members",
                "§7Active: §a${e4Manager.getActiveMembers().size}/4"
            ))
        )

        // Slot 14: Champion
        val champion = championManager.getCurrentChampion()
        inventory.setStack(
            14,
            namedItem(Items.GOLD_INGOT, "§6👑 Champion", listOf(
                "§7Current champion:",
                if (champion != null) "§e${champion.playerName} §7(${champion.defenses} defenses)"
                else "§7Title is vacant!"
            ))
        )

        // Slot 16: Leaderboard
        val season = seasonManager.getActiveSeason()
        inventory.setStack(
            16,
            namedItem(Items.BOOK, "§b📊 Leaderboard", listOf(
                "§7Season: §e${season?.number ?: "None"}",
                "§7View top trainers this season"
            ))
        )

        // Slot 31: Rules
        inventory.setStack(
            31,
            namedItem(Items.PAPER, "§f📋 Battle Rules", listOf(
                "§7See /gyms rules for details"
            ))
        )

        openInventory(player, inventory, title, 6)
    }

    fun openGymMenu(player: ServerPlayerEntity, gymManager: GymManager) {
        val leaders = gymManager.getActiveLeaders()
        val size = ((leaders.size / 9) + 1).coerceAtLeast(1) * 9
        val inventory = SimpleInventory(size.coerceAtMost(54))
        val title = Text.literal("§e⚔ Gym Leaders")

        leaders.forEachIndexed { idx, leader ->
            if (idx >= inventory.size()) return@forEachIndexed
            val ratio = MessageUtils.ratio(leader.wins, leader.losses)
            inventory.setStack(
                idx,
                namedItem(Items.GOLDEN_SWORD, "§e${leader.gymType} Gym Leader", listOf(
                    "§7Trainer: §f${leader.playerName}",
                    "§7Badge: §b${leader.badgeName}",
                    "§7Record: §f$ratio",
                    "",
                    "§aClick to view challenge queue"
                ))
            )
        }

        openInventory(player, inventory, title, inventory.size() / 9)
    }

    fun openE4Menu(player: ServerPlayerEntity, e4Manager: E4Manager) {
        val members = e4Manager.getActiveMembers()
        val inventory = SimpleInventory(36)
        val title = Text.literal("§5★ Elite Four")

        fillBorders(inventory, 36)

        members.forEach { member ->
            val slot = 10 + (member.position - 1) * 2
            if (slot < inventory.size()) {
                val ratio = MessageUtils.ratio(member.wins, member.losses)
                inventory.setStack(
                    slot,
                    namedItem(Items.AMETHYST_SHARD, "§5Elite Four #${member.position}", listOf(
                        "§7Trainer: §f${member.playerName}",
                        "§7Record: §f$ratio",
                        "",
                        "§aUse /challenge e4 ${member.position} to fight"
                    ))
                )
            }
        }

        openInventory(player, inventory, title, 4)
    }

    fun openAdminMenu(
        player: ServerPlayerEntity,
        gymManager: GymManager,
        e4Manager: E4Manager,
        seasonManager: SeasonManager
    ) {
        if (!player.hasPermissionLevel(2)) {
            MessageUtils.sendError(player, "You don't have permission to open the admin menu.")
            return
        }

        val inventory = SimpleInventory(54)
        val title = Text.literal("§c⚙ CobbleGyms Admin")
        fillBorders(inventory, 54)

        // Gym leaders overview
        inventory.setStack(
            10,
            namedItem(Items.GOLDEN_SWORD, "§eGym Leaders", listOf(
                "§7Active: §a${gymManager.getActiveLeaders().size}",
                "§7Use /gymsadmin setleader"
            ))
        )

        // Elite Four overview
        inventory.setStack(
            12,
            namedItem(Items.AMETHYST_SHARD, "§5Elite Four", listOf(
                "§7Active: §a${e4Manager.getActiveMembers().size}/4",
                "§7Use /gymsadmin sete4"
            ))
        )

        // Season overview
        val season = seasonManager.getActiveSeason()
        inventory.setStack(
            14,
            namedItem(Items.CLOCK, "§bSeason Info", listOf(
                "§7Current: §e${season?.number ?: "None"}",
                "§7Status: ${if (season != null) "§aActive" else "§cInactive"}",
                "§7Use /gymsadmin startseason"
            ))
        )

        // Reload config
        inventory.setStack(
            31,
            namedItem(Items.REPEATER, "§aReload Config", listOf(
                "§7Reloads cobblegyms config",
                "§7Use /gymsadmin reload"
            ))
        )

        openInventory(player, inventory, title, 6)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun namedItem(material: net.minecraft.item.Item, name: String, lore: List<String> = emptyList()): ItemStack {
        val stack = ItemStack(material)
        stack.setCustomName(Text.literal(name))
        // Lore is set via NBT in 1.21 – simplified here as the API may differ per MC version
        return stack
    }

    private fun fillBorders(inventory: SimpleInventory, size: Int) {
        val border = ItemStack(Items.GRAY_STAINED_GLASS_PANE)
        border.setCustomName(Text.literal("§r"))
        val rows = size / 9
        for (col in 0 until 9) {
            inventory.setStack(col, border.copy())                    // top row
            inventory.setStack((rows - 1) * 9 + col, border.copy())  // bottom row
        }
        for (row in 1 until rows - 1) {
            inventory.setStack(row * 9, border.copy())               // left col
            inventory.setStack(row * 9 + 8, border.copy())           // right col
        }
    }

    private fun openInventory(
        player: ServerPlayerEntity,
        inventory: SimpleInventory,
        title: Text,
        rows: Int
    ) {
        val type = when (rows) {
            1 -> ScreenHandlerType.GENERIC_9X1
            2 -> ScreenHandlerType.GENERIC_9X2
            3 -> ScreenHandlerType.GENERIC_9X3
            4 -> ScreenHandlerType.GENERIC_9X4
            5 -> ScreenHandlerType.GENERIC_9X5
            else -> ScreenHandlerType.GENERIC_9X6
        }
        val factory: NamedScreenHandlerFactory = SimpleNamedScreenHandlerFactory(
            { syncId, inv, _ -> GenericContainerScreenHandler(type, syncId, inv, inventory, rows) },
            title
        )
        player.openHandledScreen(factory)
    }
}
