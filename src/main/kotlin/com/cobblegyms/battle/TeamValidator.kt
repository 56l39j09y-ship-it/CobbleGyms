package com.cobblegyms.battle

import com.cobblegyms.config.GymConfig
import com.cobblegyms.data.models.BattleFormat
import com.cobblegyms.data.models.PokemonType

data class ValidationResult(
    val valid: Boolean,
    val violations: List<String>
)

object TeamValidator {
    
    // Full Smogon National Dex banned Pokémon list (abbreviated - key ubers)
    private val NATDEX_UBERS = setOf(
        "Mewtwo", "Mewtwo-Mega-X", "Mewtwo-Mega-Y",
        "Lugia", "Ho-Oh",
        "Kyogre", "Kyogre-Primal",
        "Groudon", "Groudon-Primal",
        "Rayquaza", "Rayquaza-Mega",
        "Dialga", "Palkia", "Giratina", "Giratina-Origin",
        "Arceus",
        "Reshiram", "Zekrom", "Kyurem-Black", "Kyurem-White",
        "Xerneas", "Yveltal", "Zygarde-Complete",
        "Solgaleo", "Lunala", "Necrozma-Dawn-Wings", "Necrozma-Dusk-Mane", "Necrozma-Ultra",
        "Zacian", "Zacian-Crowned", "Zamazenta", "Zamazenta-Crowned",
        "Calyrex-Ice", "Calyrex-Shadow",
        "Eternatus", "Eternatus-Eternamax",
        "Koraidon", "Miraidon",
        "Terapagos", "Terapagos-Stellar",
        "Flutter Mane", "Iron Bundle", "Chien-Pao", "Ursaluna-Bloodmoon"
    )
    
    private val BANNED_MOVES_SINGLES = setOf(
        "Double Team", "Minimize", // Evasion Clause
        "Horn Drill", "Guillotine", "Sheer Cold", "Fissure", // OHKO Clause
        "Swagger" // Swagger Clause (sometimes)
    )
    
    private val BANNED_MOVES_DOUBLES = setOf(
        "Horn Drill", "Guillotine", "Sheer Cold", "Fissure" // OHKO Clause
    )
    
    private val BANNED_ABILITIES = setOf(
        "Arena Trap", "Shadow Tag", "Moody", "Power Construct"
    )
    
    private val CLAUSES = listOf(
        "Sleep Clause", "Evasion Clause (Singles)", "OHKO Clause",
        "Species Clause", "Endless Battle Clause", "Item Clause"
    )
    
    /**
     * Validates a team in pokepaste format against Smogon rules.
     * Returns a ValidationResult with any violations found.
     */
    fun validateTeam(
        pokepaste: String,
        format: BattleFormat,
        gymType: PokemonType?,
        extraBannedPokemon: String?,
        leadersTypes: List<PokemonType>? = null // for gym leader, only these types allowed
    ): ValidationResult {
        val violations = mutableListOf<String>()
        val configBans = GymConfig.config.smogon
        
        val pokemon = parsePokepaste(pokepaste)
        
        // Species Clause: no duplicates
        val species = pokemon.map { it.species.lowercase() }
        if (species.size != species.distinct().size) {
            violations.add("§cSpecies Clause violated: Duplicate Pokémon detected.")
        }
        
        // Team size
        if (pokemon.isEmpty()) {
            violations.add("§cNo valid Pokémon found in team.")
        } else if (pokemon.size > 6) {
            violations.add("§cTeam has more than 6 Pokémon.")
        }
        
        for (mon in pokemon) {
            val speciesName = mon.species
            
            // Check globally banned Pokémon (config + Ubers)
            val allBanned = NATDEX_UBERS + configBans.bannedPokemon.toSet()
            if (allBanned.any { it.equals(speciesName, ignoreCase = true) }) {
                violations.add("§c$speciesName is banned (Uber tier).")
            }
            
            // Check leader's extra ban
            if (extraBannedPokemon != null && extraBannedPokemon.equals(speciesName, ignoreCase = true)) {
                violations.add("§c$speciesName is banned by this Gym Leader.")
            }
            
            // Gym type check for leaders
            if (leadersTypes != null) {
                val monTypes = mon.types
                val validForGym = monTypes.any { t -> leadersTypes.any { lt -> lt.name.equals(t, ignoreCase = true) } }
                if (!validForGym) {
                    violations.add("§c$speciesName is not a valid type for this Gym (must be: ${leadersTypes.joinToString(", ") { it.displayName }}).")
                }
            }
            
            // Check banned moves
            val bannedMoves = if (format == BattleFormat.SINGLES) BANNED_MOVES_SINGLES else BANNED_MOVES_DOUBLES
            val allBannedMoves = bannedMoves + configBans.bannedMoves.toSet()
            for (move in mon.moves) {
                if (allBannedMoves.any { it.equals(move, ignoreCase = true) }) {
                    violations.add("§c$speciesName has banned move: $move")
                }
            }
            
            // Check banned abilities
            val allBannedAbilities = BANNED_ABILITIES + configBans.bannedAbilities.toSet()
            if (mon.ability != null && allBannedAbilities.any { it.equals(mon.ability, ignoreCase = true) }) {
                violations.add("§c$speciesName has banned ability: ${mon.ability}")
            }
            
            // Check banned items
            if (mon.item != null && configBans.bannedItems.any { it.equals(mon.item, ignoreCase = true) }) {
                violations.add("§c$speciesName has banned item: ${mon.item}")
            }
        }
        
        return ValidationResult(violations.isEmpty(), violations)
    }
    
    /**
     * Validates that a gym leader's team only contains Pokémon of their allowed types.
     */
    fun validateLeaderTeam(pokepaste: String, allowedTypes: List<PokemonType>, extraBan: String?): ValidationResult {
        return validateTeam(pokepaste, BattleFormat.SINGLES, null, extraBan, allowedTypes)
    }
    
    /**
     * Parses a pokepaste format team into structured data.
     * Supports the standard Pokémon Showdown paste format.
     */
    fun parsePokepaste(pokepaste: String): List<ParsedPokemon> {
        val result = mutableListOf<ParsedPokemon>()
        val blocks = pokepaste.trim().split(Regex("\n\\s*\n")).filter { it.isNotBlank() }
        
        for (block in blocks) {
            val lines = block.trim().lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            
            val parsed = parseBlock(lines)
            if (parsed != null) result.add(parsed)
        }
        
        return result
    }
    
    private fun parseBlock(lines: List<String>): ParsedPokemon? {
        if (lines.isEmpty()) return null
        
        // First line: "Name (Nickname) @ Item" or "Name @ Item" or just "Name"
        val firstLine = lines[0]
        var species = ""
        var item: String? = null
        var nickname: String? = null
        
        // Parse item
        val atIndex = firstLine.lastIndexOf(" @ ")
        if (atIndex != -1) {
            item = firstLine.substring(atIndex + 3).trim()
            val namePart = firstLine.substring(0, atIndex).trim()
            // Parse nickname
            val nickMatch = Regex("^(.+) \\((.+)\\)$").matchEntire(namePart)
            if (nickMatch != null) {
                nickname = nickMatch.groupValues[1].trim()
                species = nickMatch.groupValues[2].trim()
            } else {
                species = namePart.trim()
            }
        } else {
            val nickMatch = Regex("^(.+) \\((.+)\\)$").matchEntire(firstLine.trim())
            if (nickMatch != null) {
                nickname = nickMatch.groupValues[1].trim()
                species = nickMatch.groupValues[2].trim()
            } else {
                species = firstLine.trim()
            }
        }
        
        // Remove gender marker
        species = species.replace(Regex("\\s*\\([MF]\\)\\s*$"), "").trim()
        
        var ability: String? = null
        var level: Int = 100
        val evs = mutableMapOf<String, Int>()
        val ivs = mutableMapOf<String, Int>()
        var nature: String? = null
        var teraType: String? = null
        val moves = mutableListOf<String>()
        
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            when {
                line.startsWith("Ability:") -> ability = line.removePrefix("Ability:").trim()
                line.startsWith("Level:") -> level = line.removePrefix("Level:").trim().toIntOrNull() ?: 100
                line.startsWith("EVs:") -> parseStats(line.removePrefix("EVs:").trim(), evs)
                line.startsWith("IVs:") -> parseStats(line.removePrefix("IVs:").trim(), ivs)
                line.endsWith("Nature") -> nature = line.removeSuffix("Nature").trim()
                line.startsWith("Tera Type:") -> teraType = line.removePrefix("Tera Type:").trim()
                line.startsWith("-") -> moves.add(line.removePrefix("-").trim().removePrefix(" "))
            }
        }
        
        return ParsedPokemon(
            species = species,
            nickname = nickname,
            item = item,
            ability = ability,
            level = level,
            evs = evs,
            ivs = ivs,
            nature = nature,
            teraType = teraType,
            moves = moves,
            types = inferTypesFromSpecies(species)
        )
    }
    
    private fun parseStats(statStr: String, map: MutableMap<String, Int>) {
        statStr.split("/").forEach { part ->
            val trimmed = part.trim()
            val spaceIdx = trimmed.lastIndexOf(" ")
            if (spaceIdx != -1) {
                val value = trimmed.substring(0, spaceIdx).trim().toIntOrNull() ?: return@forEach
                val stat = trimmed.substring(spaceIdx + 1).trim()
                map[stat] = value
            }
        }
    }
    
    /**
     * Infers Pokémon types from species name.
     * This is a simplified lookup - in production this would use Cobblemon's API.
     */
    fun inferTypesFromSpecies(species: String): List<String> {
        // Simplified type map for common Pokémon - in full implementation would query Cobblemon API
        return typeMap[species.lowercase().replace("-", " ")] ?: listOf("Normal")
    }
    
    private val typeMap: Map<String, List<String>> = mapOf(
        "charizard" to listOf("Fire", "Flying"),
        "blastoise" to listOf("Water"),
        "venusaur" to listOf("Grass", "Poison"),
        "pikachu" to listOf("Electric"),
        "mewtwo" to listOf("Psychic"),
        "lucario" to listOf("Fighting", "Steel"),
        "garchomp" to listOf("Dragon", "Ground"),
        "gengar" to listOf("Ghost", "Poison"),
        "gyarados" to listOf("Water", "Flying"),
        "dragonite" to listOf("Dragon", "Flying"),
        "tyranitar" to listOf("Rock", "Dark"),
        "blaziken" to listOf("Fire", "Fighting"),
        "salamence" to listOf("Dragon", "Flying"),
        "metagross" to listOf("Steel", "Psychic"),
        "garchomp" to listOf("Dragon", "Ground"),
        "togekiss" to listOf("Fairy", "Flying"),
        "rotom-wash" to listOf("Electric", "Water"),
        "ferrothorn" to listOf("Grass", "Steel"),
        "landorus-therian" to listOf("Ground", "Flying"),
        "toxapex" to listOf("Poison", "Water"),
        "hawlucha" to listOf("Fighting", "Flying"),
        "clefable" to listOf("Fairy"),
        "heatran" to listOf("Fire", "Steel"),
        "scizor" to listOf("Bug", "Steel"),
        "weavile" to listOf("Dark", "Ice"),
        "kingambit" to listOf("Dark", "Steel"),
        "flutter mane" to listOf("Ghost", "Fairy"),
        "iron bundle" to listOf("Ice", "Water"),
        "chien-pao" to listOf("Dark", "Ice")
    )
    
    fun getClauseSummary(format: BattleFormat): String {
        val base = CLAUSES.joinToString(", ")
        return "§7Active clauses: §f$base"
    }
}

data class ParsedPokemon(
    val species: String,
    val nickname: String?,
    val item: String?,
    val ability: String?,
    val level: Int,
    val evs: Map<String, Int>,
    val ivs: Map<String, Int>,
    val nature: String?,
    val teraType: String?,
    val moves: List<String>,
    val types: List<String>
)
