package com.cobblegyms.pokepaste

import com.cobblegyms.CobbleGyms
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object PokepasteImporter {
    
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    /**
     * Imports a team from a pokepaste URL.
     * Supports:
     * - https://pokepast.es/XXXXXXXXXXXX
     * - https://play.pokemonshowdown.com/...
     * - Direct pokepaste text
     */
    fun importFromUrl(url: String): String? {
        return try {
            val normalizedUrl = normalizeUrl(url)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(normalizedUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() != 200) {
                CobbleGyms.LOGGER.warn("Failed to fetch pokepaste: HTTP ${response.statusCode()}")
                return null
            }
            
            extractPokepaste(response.body(), url)
        } catch (e: Exception) {
            CobbleGyms.LOGGER.error("Failed to import pokepaste from $url: ${e.message}")
            null
        }
    }
    
    private fun normalizeUrl(url: String): String {
        return when {
            // pokepast.es URL - fetch the raw text
            url.contains("pokepast.es") && !url.endsWith("/raw") -> {
                val base = url.trimEnd('/')
                "$base/raw"
            }
            else -> url
        }
    }
    
    private fun extractPokepaste(content: String, originalUrl: String): String? {
        return when {
            // HTML page - try to extract pokepaste content
            content.contains("<html") -> {
                val pasteRegex = Regex("<pre[^>]*>([\\s\\S]*?)</pre>")
                val match = pasteRegex.find(content)
                match?.groupValues?.get(1)?.let { 
                    it.replace("&amp;", "&")
                       .replace("&lt;", "<")
                       .replace("&gt;", ">")
                       .replace("&quot;", "\"")
                       .replace("&apos;", "'")
                       .replace("&#39;", "'")
                       .trim()
                }
            }
            // Raw text - validate it looks like a pokepaste
            content.contains(" @ ") || content.contains("Ability:") -> content.trim()
            else -> null
        }
    }
    
    /**
     * Validates that the imported team data looks like valid pokepaste format.
     */
    fun isValidPokepaste(text: String): Boolean {
        return text.isNotBlank() && (text.contains("Ability:") || text.contains(" @ ") || text.contains("- "))
    }
}
