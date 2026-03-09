package com.cobblegyms.validation;

import com.cobblegyms.config.SmogonBanConfig;
import com.cobblegyms.util.CobblemonUtil;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SmogonValidator {

    public record ValidationViolation(String description) {}

    public List<ValidationViolation> getViolations(ServerPlayerEntity player, UUID targetLeaderId) {
        List<ValidationViolation> violations = new ArrayList<>();
        SmogonBanConfig banConfig = SmogonBanConfig.getInstance();
        List<Pokemon> party = CobblemonUtil.getPartyPokemon(player);

        if (party.isEmpty()) {
            violations.add(new ValidationViolation("Your party is empty!"));
            return violations;
        }

        for (Pokemon pokemon : party) {
            String species = CobblemonUtil.getPokemonSpecies(pokemon);
            String form = CobblemonUtil.getPokemonForm(pokemon);
            String fullName = species + (form.isEmpty() || form.equals("normal") ? "" : form);

            if (banConfig.isPokemonBanned(species) || banConfig.isPokemonBanned(fullName)) {
                violations.add(new ValidationViolation(
                        "Banned Pokémon: " + capitalize(species) + " is banned (Uber tier or clause)"));
            }

            if (targetLeaderId != null && banConfig.isGymExtraBanned(targetLeaderId.toString(), species)) {
                violations.add(new ValidationViolation(
                        "Banned by Gym Leader: " + capitalize(species) + " is banned in this gym this season"));
            }

            String ability = CobblemonUtil.getPokemonAbility(pokemon);
            if (!ability.isEmpty() && banConfig.isAbilityBanned(ability)) {
                violations.add(new ValidationViolation(
                        "Banned ability on " + capitalize(species) + ": " + capitalize(ability)));
            }

            String item = CobblemonUtil.getPokemonItem(pokemon);
            if (!item.isEmpty() && banConfig.isItemBanned(item)) {
                violations.add(new ValidationViolation(
                        "Banned item on " + capitalize(species) + ": " + capitalize(item)));
            }

            List<String> moves = CobblemonUtil.getPokemonMoves(pokemon);
            for (String move : moves) {
                if (banConfig.isMoveBanned(move)) {
                    violations.add(new ValidationViolation(
                            "Banned move on " + capitalize(species) + ": " + capitalize(move)));
                }
            }

            boolean hasMoody = ability.equalsIgnoreCase("moody");
            boolean hasEvasionMove = moves.contains("doubleteam") || moves.contains("minimize");
            if (hasMoody && hasEvasionMove) {
                violations.add(new ValidationViolation(
                        capitalize(species) + " has both Moody and an evasion move (banned combination)"));
            }
        }

        long batonPassCount = party.stream()
                .filter(p -> CobblemonUtil.getPokemonMoves(p).contains("batonpass"))
                .count();
        if (batonPassCount > 1) {
            violations.add(new ValidationViolation("Baton Pass chain strategy is banned (multiple Baton Pass users)"));
        }

        return violations;
    }

    public boolean isTeamValid(ServerPlayerEntity player, UUID targetLeaderId) {
        return getViolations(player, targetLeaderId).isEmpty();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
