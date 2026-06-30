package org.hyzionstudios.mysticvanish.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.hyzionstudios.mysticvanish.model.VanishFeature;
import org.hyzionstudios.mysticvanish.model.VanishProfile;

public final class ConfigManager {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private final JavaPlugin plugin;
    private Map<String, Object> config = new LinkedHashMap<>();
    private Map<String, Object> messages = new LinkedHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = loadOrGenerate("config.json", ConfigDefaults.config());
        writeIfMissing("placeholders.json", ConfigDefaults.placeholders());
    }

    public void loadMessages() {
        messages = loadOrGenerate("messages.json", ConfigDefaults.messages());
    }

    public String getString(String path, String fallback) {
        Object value = value(config, path);
        return value == null ? fallback : String.valueOf(value);
    }

    public boolean getBoolean(String path, boolean fallback) {
        Object value = value(config, path);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    public int getInt(String path, int fallback) {
        Object value = value(config, path);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public double getDouble(String path, double fallback) {
        Object value = value(config, path);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public List<String> getStringList(String path) {
        Object value = value(config, path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    public List<Integer> getIntList(String path) {
        Object value = value(config, path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                result.add(number.intValue());
            }
        }
        return result;
    }

    public String getMessage(String path, String fallback) {
        Object value = value(messages, path);
        return value == null ? fallback : String.valueOf(value);
    }

    public Set<String> getSectionKeys(String path) {
        Object value = value(config, path);
        if (value instanceof Map<?, ?> map) {
            return map.keySet().stream().map(String::valueOf).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }
        return Collections.emptySet();
    }

    public VanishProfile getProfile(String name) {
        String base = "profiles." + name + ".";
        return new VanishProfile(name, getInt(base + "vanish_level", getDefaultVanishLevel()), getInt(base + "see_level", getDefaultSeeLevel()));
    }

    public List<Integer> getProfileLevels() {
        List<Integer> levels = new ArrayList<>();
        levels.add(getDefaultVanishLevel());
        levels.add(getDefaultSeeLevel());
        for (String key : getSectionKeys("profiles")) {
            VanishProfile profile = getProfile(key);
            levels.add(profile.vanishLevel());
            levels.add(profile.seeLevel());
        }
        return levels;
    }

    public boolean isFeatureEnabled(VanishFeature feature) {
        return getBoolean("features." + feature.configKey() + ".enabled", true);
    }

    public boolean doesFeatureRequirePermission(VanishFeature feature) {
        return getBoolean("features." + feature.configKey() + ".require_permission", true);
    }

    public String getFeaturePermission(VanishFeature feature) {
        return getString("features." + feature.configKey() + ".permission", feature.defaultPermission());
    }

    public String getFeatureMode(VanishFeature feature, String fallback) {
        return getString("features." + feature.configKey() + ".mode", fallback);
    }

    public int getDefaultVanishLevel() {
        return getInt("settings.default_vanish_level", 10);
    }

    public int getDefaultSeeLevel() {
        return getInt("settings.default_see_level", 0);
    }

    public String getLevelPrefix() {
        return getString("permissions.level_prefix", "mysticvanish.level.");
    }

    public String getSeePrefix() {
        return getString("permissions.see_prefix", "mysticvanish.see.");
    }

    /**
     * Resolves the plugin's data folder to {@code mods/MysticVanish/} (next to the deployed jar)
     * rather than the platform default. Falls back to {@link JavaPlugin#getDataDirectory()} if the
     * jar location is unavailable.
     */
    public Path dataDirectory() {
        Path jar = plugin.getFile();
        if (jar != null && jar.getParent() != null) {
            return jar.getParent().resolve(folderName());
        }
        return plugin.getDataDirectory();
    }

    /**
     * The mod's folder name. {@code getName()} returns the qualified {@code group:name} identifier whose
     * {@code :} is an illegal path char on Windows, so use the manifest's simple name ("MysticVanish").
     */
    private String folderName() {
        String name = plugin.getManifest() == null ? null : plugin.getManifest().getName();
        return name == null || name.isBlank() ? "MysticVanish" : name;
    }

    /** Generates the file from in-code defaults if absent, then reads it back (defaults on failure). */
    private Map<String, Object> loadOrGenerate(String fileName, Map<String, Object> defaults) {
        writeIfMissing(fileName, defaults);
        Path path = dataDirectory().resolve(fileName);
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            Map<String, Object> parsed = GSON.fromJson(reader, MAP_TYPE);
            return parsed == null ? new LinkedHashMap<>(defaults) : parsed;
        } catch (IOException | JsonParseException exception) {
            plugin.getLogger().at(Level.WARNING).log("Failed to read " + fileName + "; using in-code defaults.", exception);
            return new LinkedHashMap<>(defaults);
        }
    }

    /** Writes the given data as pretty-printed, HTML-escaped JSON, but only if the file isn't there yet. */
    private void writeIfMissing(String fileName, Map<String, Object> data) {
        Path path = dataDirectory().resolve(fileName);
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(dataDirectory());
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)) {
                PRETTY_GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            plugin.getLogger().at(Level.WARNING).log("Failed to generate " + fileName + ".", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object value(Map<String, Object> source, String path) {
        Object current = source;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(part);
        }
        return current;
    }
}
