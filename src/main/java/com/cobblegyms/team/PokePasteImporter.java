package com.cobblegyms.team;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PokePasteImporter {

    public static String fetchFromUrl(String pokePasteUrl) throws Exception {
        // Convert pokepaste.me URLs to raw format
        String rawUrl = pokePasteUrl;
        if (rawUrl.contains("pokepaste.me") && !rawUrl.endsWith("/raw")) {
            if (!rawUrl.endsWith("/")) rawUrl += "/";
            rawUrl += "raw";
        }

        URI uri = URI.create(rawUrl);
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "CobbleGyms/1.0");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP error " + responseCode + " fetching " + rawUrl);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public static String parsePokePasteData(String rawData) {
        JsonArray teamArray = new JsonArray();
        String[] pokemonBlocks = rawData.split("\n\n+");

        for (String block : pokemonBlocks) {
            if (block.trim().isEmpty()) continue;
            JsonObject pokemon = parseShowdownBlock(block.trim());
            if (pokemon != null) teamArray.add(pokemon);
        }

        return teamArray.toString();
    }

    private static JsonObject parseShowdownBlock(String block) {
        String[] lines = block.split("\n");
        if (lines.length == 0) return null;

        JsonObject obj = new JsonObject();
        String firstLine = lines[0].trim();

        // Parse "Pokémon @ Item" or just "Pokémon"
        if (firstLine.contains(" @ ")) {
            String[] parts = firstLine.split(" @ ", 2);
            parseSpeciesNickname(obj, parts[0].trim());
            obj.addProperty("item", parts[1].trim());
        } else {
            parseSpeciesNickname(obj, firstLine);
        }

        JsonArray moves = new JsonArray();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Ability:")) {
                obj.addProperty("ability", line.substring("Ability:".length()).trim());
            } else if (line.startsWith("Level:")) {
                try {
                    obj.addProperty("level", Integer.parseInt(line.substring("Level:".length()).trim()));
                } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("EVs:")) {
                obj.addProperty("evs", line.substring("EVs:".length()).trim());
            } else if (line.startsWith("IVs:")) {
                obj.addProperty("ivs", line.substring("IVs:".length()).trim());
            } else if (line.endsWith("Nature")) {
                obj.addProperty("nature", line.replace(" Nature", "").trim());
            } else if (line.startsWith("- ")) {
                moves.add(line.substring(2).trim());
            } else if (line.startsWith("Shiny:")) {
                obj.addProperty("shiny", line.substring("Shiny:".length()).trim().equalsIgnoreCase("Yes"));
            }
        }
        obj.add("moves", moves);
        return obj;
    }

    private static void parseSpeciesNickname(JsonObject obj, String nameStr) {
        // Format: "Nickname (Species)" or just "Species"
        if (nameStr.contains("(") && nameStr.contains(")")) {
            int start = nameStr.lastIndexOf("(");
            int end = nameStr.lastIndexOf(")");
            String nickname = nameStr.substring(0, start).trim();
            String species = nameStr.substring(start + 1, end).trim();
            // Remove gender indicator if present
            if (species.endsWith(" (M)") || species.endsWith(" (F)")) {
                species = species.substring(0, species.length() - 4).trim();
            }
            obj.addProperty("nickname", nickname);
            obj.addProperty("species", species);
        } else {
            String species = nameStr;
            if (species.endsWith(" (M)") || species.endsWith(" (F)")) {
                species = species.substring(0, species.length() - 4).trim();
            }
            obj.addProperty("species", species);
        }
    }

    public static List<String> extractSpeciesNames(String teamJson) {
        List<String> names = new ArrayList<>();
        try {
            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(teamJson).getAsJsonArray();
            for (com.google.gson.JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("species")) names.add(obj.get("species").getAsString());
            }
        } catch (Exception e) {
            // Return empty list if parsing fails
        }
        return names;
    }
}
