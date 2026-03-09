package com.cobblegyms.system.validation

import com.cobblegyms.config.CobbleGymsConfig

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
)

object SmogonValidator {
    fun validateTeam(speciesList: List<String>): ValidationResult {
        val errors = mutableListOf<String>()

        if (speciesList.size < CobbleGymsConfig.minTeamSize) {
            errors.add("Team must have at least ${CobbleGymsConfig.minTeamSize} Pokémon.")
        }
        if (speciesList.size > CobbleGymsConfig.maxTeamSize) {
            errors.add("Team cannot have more than ${CobbleGymsConfig.maxTeamSize} Pokémon.")
        }

        val bannedOnTeam = speciesList.filter { species ->
            CobbleGymsConfig.defaultBannedSpecies.any { banned ->
                species.lowercase() == banned.lowercase()
            }
        }
        if (bannedOnTeam.isNotEmpty()) {
            errors.add("Banned Pokémon on team: ${bannedOnTeam.joinToString(", ")}")
        }

        val duplicates = speciesList.groupBy { it.lowercase() }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate Pokémon found: ${duplicates.joinToString(", ")}")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun isBanned(species: String): Boolean {
        return CobbleGymsConfig.defaultBannedSpecies.any { banned ->
            species.lowercase() == banned.lowercase()
        }
    }

    fun getBannedList(): List<String> = CobbleGymsConfig.defaultBannedSpecies.toList()

    fun validateFormat(formatName: String): Boolean {
        val validFormats = listOf("singles", "doubles", "vgc", "ou", "uu", "ru", "nu", "pu", "ubers", "lc")
        return validFormats.contains(formatName.lowercase())
    }
}
