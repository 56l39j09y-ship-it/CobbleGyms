package com.cobblegyms.utils

import com.cobblegyms.config.CobbleGymsConfig

/**
 * Validates player Pokémon teams against Smogon format rules.
 *
 * Checks:
 *  - Team size (exactly 6 Pokémon)
 *  - Maximum level cap
 *  - Banned Pokémon species
 *  - Banned moves
 *  - Banned abilities
 *  - No duplicate Pokémon (Clause)
 */
object SmogonValidator {

    /**
     * Result returned by [validateTeam].
     */
    data class ValidationResult(
        val valid: Boolean,
        val violations: List<String> = emptyList()
    ) {
        /** Human-readable summary of all violations. */
        fun summary(): String = if (valid) "Team is valid." else violations.joinToString("\n") { "§c✗ $it" }
    }

    /**
     * Simplified representation of a team member used for validation.
     *
     * In a real integration this would be populated from Cobblemon's
     * PlayerPartyStore / Pokemon data objects.
     */
    data class TeamMember(
        val species: String,
        val level: Int,
        val moves: List<String> = emptyList(),
        val ability: String = ""
    )

    /**
     * Validate a list of [TeamMember]s against the rules in [config].
     */
    fun validateTeam(
        team: List<TeamMember>,
        config: CobbleGymsConfig.SmogonConfig = CobbleGymsConfig.get().smogon
    ): ValidationResult {
        val violations = mutableListOf<String>()

        // 1. Team size
        if (team.size != config.teamSize) {
            violations += "Team must have exactly ${config.teamSize} Pokémon (found ${team.size})"
        }

        // 2. Level cap
        team.filter { it.level > config.maxLevel }.forEach { member ->
            violations += "${member.species} exceeds the level cap (${member.level} > ${config.maxLevel})"
        }

        // 3. Banned species
        if (config.banListEnabled) {
            val bannedLower = config.bannedPokemon.map { it.lowercase() }
            team.filter { it.species.lowercase() in bannedLower }.forEach { member ->
                violations += "${member.species} is banned in ${config.format}"
            }
        }

        // 4. Banned moves
        if (config.banListEnabled) {
            val bannedMovesLower = config.bannedMoves.map { it.lowercase() }
            team.forEach { member ->
                member.moves.filter { it.lowercase() in bannedMovesLower }.forEach { move ->
                    violations += "${member.species} knows banned move: $move"
                }
            }
        }

        // 5. Banned abilities
        if (config.banListEnabled) {
            val bannedAbilitiesLower = config.bannedAbilities.map { it.lowercase() }
            team.filter { it.ability.lowercase() in bannedAbilitiesLower }.forEach { member ->
                violations += "${member.species} has banned ability: ${member.ability}"
            }
        }

        // 6. Species Clause (no duplicate Pokémon)
        val seen = mutableSetOf<String>()
        team.forEach { member ->
            val speciesKey = member.species.lowercase()
            if (!seen.add(speciesKey)) {
                violations += "Duplicate Pokémon: ${member.species} (Species Clause)"
            }
        }

        return ValidationResult(valid = violations.isEmpty(), violations = violations)
    }

    /**
     * Quick check: returns true if the team passes all Smogon rules.
     */
    fun isTeamValid(team: List<TeamMember>, config: CobbleGymsConfig.SmogonConfig = CobbleGymsConfig.get().smogon): Boolean =
        validateTeam(team, config).valid
}
