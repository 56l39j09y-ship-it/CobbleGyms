package com.cobblegyms.util;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class CobblemonUtil {

    public static PlayerPartyStore getPlayerParty(ServerPlayerEntity player) {
        return Cobblemon.INSTANCE.getStorage().getParty(player);
    }

    public static String getPokemonSpecies(Pokemon pokemon) {
        if (pokemon == null) return "";
        return pokemon.getSpecies().getName().toLowerCase();
    }

    public static List<String> getPokemonMoves(Pokemon pokemon) {
        List<String> moves = new ArrayList<>();
        if (pokemon == null) return moves;
        pokemon.getMoveSet().forEach(move -> {
            if (move != null) moves.add(move.getName().toLowerCase());
        });
        return moves;
    }

    public static String getPokemonAbility(Pokemon pokemon) {
        if (pokemon == null) return "";
        return pokemon.getAbility().getName().toLowerCase();
    }

    public static String getPokemonItem(Pokemon pokemon) {
        if (pokemon == null) return "";
        var item = pokemon.heldItem();
        if (item == null || item.isEmpty()) return "";
        return item.getItem().toString().toLowerCase().replace("minecraft:", "").replace("cobblemon:", "");
    }

    public static String getPokemonForm(Pokemon pokemon) {
        if (pokemon == null) return "";
        return pokemon.getForm().getName().toLowerCase();
    }

    public static List<Pokemon> getPartyPokemon(ServerPlayerEntity player) {
        List<Pokemon> party = new ArrayList<>();
        PlayerPartyStore store = getPlayerParty(player);
        if (store == null) return party;
        for (Pokemon pokemon : store) {
            if (pokemon != null) party.add(pokemon);
        }
        return party;
    }

    public static void startBattle(ServerPlayerEntity leader, ServerPlayerEntity challenger,
                                    com.cobblegyms.model.BattleFormat format) {
        try {
            BattleFormat cobblemonFormat = format == com.cobblegyms.model.BattleFormat.DOUBLES
                    ? BattleFormat.GEN_9_DOUBLES
                    : BattleFormat.GEN_9_SINGLES;

            PlayerBattleActor leaderActor = new PlayerBattleActor(
                    leader.getUuid(),
                    getPlayerParty(leader).toBattleTeam(true, true)
            );
            PlayerBattleActor challengerActor = new PlayerBattleActor(
                    challenger.getUuid(),
                    getPlayerParty(challenger).toBattleTeam(true, true)
            );

            BattleRegistry.INSTANCE.startBattle(
                    cobblemonFormat,
                    new BattleSide(leaderActor),
                    new BattleSide(challengerActor)
            );
        } catch (Exception e) {
            System.err.println("[CobbleGyms] Failed to start battle: " + e.getMessage());
        }
    }

    public static boolean isInBattle(ServerPlayerEntity player) {
        return BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player) != null;
    }

    public static PokemonBattle getBattle(ServerPlayerEntity player) {
        return BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
    }
}
