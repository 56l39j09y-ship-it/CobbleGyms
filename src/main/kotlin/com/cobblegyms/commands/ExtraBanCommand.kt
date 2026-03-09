package com.cobblegyms.commands

import com.cobblegyms.CobbleGyms
import com.cobblegyms.data.GymRepository
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * Command for Gym Leaders to manage their extra Pokémon ban (1 per season).
 */
object ExtraBanCommand {
    
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("gymban")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("pokemon", StringArgumentType.word())
                        .executes { ctx -> setExtraBan(ctx, StringArgumentType.getString(ctx, "pokemon")) }))
                .then(CommandManager.literal("clear")
                    .executes { ctx -> clearExtraBan(ctx) })
                .then(CommandManager.literal("info")
                    .executes { ctx -> showBanInfo(ctx) })
        )
    }
    
    private fun setExtraBan(ctx: CommandContext<ServerCommandSource>, pokemon: String): Int {
        val player = ctx.source.player ?: run {
            ctx.source.sendError(Text.literal("Must be run by a player."))
            return 0
        }
        
        if (!CobbleGyms.gymManager.isGymLeader(player.uuid)) {
            player.sendMessage(Text.literal("§cYou must be a Gym Leader to use this command."))
            return 0
        }
        
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        if (season == null) {
            player.sendMessage(Text.literal("§cNo active season."))
            return 0
        }
        
        val leader = GymRepository.getGymLeader(player.uuid)!!
        
        // Check if they already set a ban this season
        if (leader.extraBannedPokemon != null && leader.extraBanSeason == season.id) {
            player.sendMessage(Text.literal("§c§lYou have already set your extra ban for this season!"))
            player.sendMessage(Text.literal("§7Banned: §c${leader.extraBannedPokemon}"))
            player.sendMessage(Text.literal("§7Wait until the next season to change your ban."))
            return 0
        }
        
        val success = CobbleGyms.gymManager.setExtraBan(player.uuid, pokemon, season.id)
        
        if (success) {
            player.sendMessage(Text.literal("§a§lExtra Ban Set! §7Pokémon: §c$pokemon"))
            player.sendMessage(Text.literal("§7Challengers will not be able to use §c$pokemon §7in your gym."))
            player.sendMessage(Text.literal("§7This ban is permanent for §eSeason ${season.number}§7. Choose wisely!"))
        } else {
            player.sendMessage(Text.literal("§cFailed to set extra ban. You may have already set one this season."))
        }
        
        return 1
    }
    
    private fun clearExtraBan(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        
        if (!CobbleGyms.gymManager.isGymLeader(player.uuid)) {
            player.sendMessage(Text.literal("§cYou must be a Gym Leader to use this command."))
            return 0
        }
        
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        val leader = GymRepository.getGymLeader(player.uuid)
        
        if (leader?.extraBannedPokemon == null) {
            player.sendMessage(Text.literal("§7You have no extra ban set."))
            return 1
        }
        
        if (leader.extraBanSeason == season?.id) {
            player.sendMessage(Text.literal("§c§lCannot remove ban! §7You have already used your ban this season."))
            player.sendMessage(Text.literal("§7You cannot change your ban until the next season."))
            return 0
        }
        
        // Can only clear if it's from a previous season
        GymRepository.upsertGymLeader(leader.copy(extraBannedPokemon = null, extraBanSeason = 0))
        player.sendMessage(Text.literal("§aExtra ban cleared."))
        return 1
    }
    
    private fun showBanInfo(ctx: CommandContext<ServerCommandSource>): Int {
        val player = ctx.source.player ?: return 0
        
        if (!CobbleGyms.gymManager.isGymLeader(player.uuid)) {
            player.sendMessage(Text.literal("§cYou must be a Gym Leader to use this command."))
            return 0
        }
        
        val season = CobbleGyms.seasonManager.getCurrentSeason()
        val leader = GymRepository.getGymLeader(player.uuid)!!
        
        player.sendMessage(Text.literal("§6§l=== Your Extra Ban ==="))
        player.sendMessage(Text.literal("§7Season: §e${season?.number ?: "N/A"}"))
        
        if (leader.extraBannedPokemon != null) {
            val isCurrentSeason = leader.extraBanSeason == season?.id
            player.sendMessage(Text.literal("§7Banned Pokémon: §c${leader.extraBannedPokemon}"))
            if (isCurrentSeason) {
                player.sendMessage(Text.literal("§7Status: §cFixed for this season"))
                player.sendMessage(Text.literal("§7You cannot change this ban until Season ${(season?.number ?: 0) + 1}."))
            } else {
                player.sendMessage(Text.literal("§7Status: §aFrom previous season (can be changed)"))
                player.sendMessage(Text.literal("§7Use §f/gymban set <pokemon> §7to set a new ban."))
            }
        } else {
            player.sendMessage(Text.literal("§7No extra ban set."))
            player.sendMessage(Text.literal("§7Use §f/gymban set <pokemon> §7to ban a Pokémon for challengers."))
            player.sendMessage(Text.literal("§c§lWarning: §7Once set, you cannot change it this season!"))
        }
        
        return 1
    }
}
