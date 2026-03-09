package com.cobblegyms.util;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.cobblegyms.model.PokemonType;

public class MessageUtil {

    private static final String PREFIX = "\u00a77[\u00a76CobbleGyms\u00a77] \u00a7r";

    public static void sendSuccess(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX + "\u00a7a" + message), false);
    }

    public static void sendError(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX + "\u00a7c" + message), false);
    }

    public static void sendInfo(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX + "\u00a77" + message), false);
    }

    public static void sendWarning(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(PREFIX + "\u00a7e" + message), false);
    }

    public static void sendTitle(ServerPlayerEntity player, String title, String subtitle) {
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title)));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle)));
        }
    }

    public static void broadcast(MinecraftServer server, String message) {
        server.getPlayerManager().broadcast(Text.literal(PREFIX + message), false);
    }

    public static void broadcastColored(MinecraftServer server, String message) {
        server.getPlayerManager().broadcast(Text.literal(PREFIX + message), false);
    }

    public static String formatType(PokemonType type) {
        if (type == null) return "\u00a77Unknown";
        return type.getColorCode() + type.getDisplayName();
    }

    public static String colorize(String text) {
        return text.replace("&", "\u00a7");
    }

    public static String stripColor(String text) {
        return text.replaceAll("\u00a7[0-9a-fk-or]", "");
    }
}
