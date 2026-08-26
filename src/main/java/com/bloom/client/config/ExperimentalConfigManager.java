package com.bloom.client.config;

import com.bloom.BloomMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/**
 * Loads the original Shine experimental schema without binding it to removed 1.21 APIs.
 * The state is intentionally independent from BloomConfig, so experimental failures cannot
 * disable or corrupt selective bloom.
 */
public final class ExperimentalConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/assets/shine/defaults/experimental.json";
    private static volatile JsonObject state = new JsonObject();
    private static boolean loaded;

    private ExperimentalConfigManager() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        state = readBundledDefaults();
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("config/shine/experimental.json");
        try {
            if (Files.isRegularFile(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject override = JsonParser.parseReader(reader).getAsJsonObject();
                    merge(state, override);
                }
            } else {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(state, writer);
                }
            }
        } catch (Exception exception) {
            BloomMod.LOGGER.warn("Unable to load Shine experimental config; using bundled defaults", exception);
        }
    }

    public static synchronized void reload() {
        loaded = false;
        load();
    }

    public static synchronized boolean enabled() {
        load();
        return !state.has("enabled") || !state.get("enabled").isJsonPrimitive()
            || state.get("enabled").getAsBoolean();
    }

    public static synchronized JsonObject snapshot() {
        load();
        return state.deepCopy();
    }

    public static synchronized void save(JsonObject snapshot) {
        load();
        if (snapshot == null) return;
        state = snapshot.deepCopy();
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("config/shine/experimental.json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (Exception exception) {
            BloomMod.LOGGER.warn("Unable to save Shine experimental config", exception);
        }
    }

    private static JsonObject readBundledDefaults() {
        try (Reader reader = new java.io.InputStreamReader(
                ExperimentalConfigManager.class.getResourceAsStream(DEFAULT_RESOURCE), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            BloomMod.LOGGER.warn("Unable to read bundled Shine experimental defaults", exception);
            return new JsonObject();
        }
    }

    private static void merge(JsonObject target, JsonObject override) {
        for (String key : override.keySet()) {
            if (override.get(key).isJsonObject() && target.has(key) && target.get(key).isJsonObject()) {
                merge(target.getAsJsonObject(key), override.getAsJsonObject(key));
            } else {
                target.add(key, override.get(key).deepCopy());
            }
        }
    }
}
