package com.cobblegyms.util

import com.cobblegyms.data.models.PokemonType

object PokemonTypeUtil {
    
    /**
     * Gets all 18 Pokémon types.
     */
    fun getAllTypes(): List<PokemonType> = PokemonType.entries.toList()
    
    /**
     * Gets the color code for a Pokémon type.
     */
    fun getTypeColor(type: PokemonType): String = type.color
    
    /**
     * Gets a formatted type list for display.
     */
    fun getTypeListDisplay(): String {
        return PokemonType.entries.joinToString(", ") { "${it.color}${it.displayName}§r" }
    }
    
    /**
     * Checks if a list of types includes at least one matching type.
     */
    fun isTypeCompatible(pokemonTypes: List<String>, allowedTypes: List<PokemonType>): Boolean {
        return pokemonTypes.any { pokemonType ->
            allowedTypes.any { allowed -> allowed.name.equals(pokemonType, ignoreCase = true) }
        }
    }
    
    /**
     * Gets the badge display name for a type.
     */
    fun getBadgeName(type: PokemonType): String = "${type.displayName} Badge"
    
    /**
     * Gets the corresponding medal color for a type.
     */
    fun getBadgeDescription(type: PokemonType): String = when (type) {
        PokemonType.FIRE -> "The burning ${type.displayName} Badge"
        PokemonType.WATER -> "The flowing ${type.displayName} Badge"
        PokemonType.GRASS -> "The verdant ${type.displayName} Badge"
        PokemonType.ELECTRIC -> "The sparkling ${type.displayName} Badge"
        PokemonType.ICE -> "The frozen ${type.displayName} Badge"
        PokemonType.FIGHTING -> "The mighty ${type.displayName} Badge"
        PokemonType.POISON -> "The toxic ${type.displayName} Badge"
        PokemonType.GROUND -> "The earthen ${type.displayName} Badge"
        PokemonType.FLYING -> "The soaring ${type.displayName} Badge"
        PokemonType.PSYCHIC -> "The mystical ${type.displayName} Badge"
        PokemonType.BUG -> "The buzzing ${type.displayName} Badge"
        PokemonType.ROCK -> "The ancient ${type.displayName} Badge"
        PokemonType.GHOST -> "The phantom ${type.displayName} Badge"
        PokemonType.DRAGON -> "The legendary ${type.displayName} Badge"
        PokemonType.DARK -> "The shadowy ${type.displayName} Badge"
        PokemonType.STEEL -> "The metallic ${type.displayName} Badge"
        PokemonType.FAIRY -> "The enchanted ${type.displayName} Badge"
        PokemonType.NORMAL -> "The ordinary ${type.displayName} Badge"
    }
}
