package org.hyzionstudios.mysticvanish.storage;

import java.util.Locale;
import org.hyzionstudios.mysticvanish.config.ConfigManager;

/**
 * Owns the active {@link VanishStorage} implementation and is the single place that decides which
 * backend to use. V1 ships in-memory storage; YAML, SQLite, MySQL, or Redis backends slot in here
 * behind the same interface without touching the rest of the plugin.
 */
public final class StorageManager {
    private final VanishStorage storage;

    public StorageManager(ConfigManager configManager) {
        this.storage = create(configManager.getString("storage.type", "memory"));
    }

    public VanishStorage storage() {
        return storage;
    }

    private static VanishStorage create(String type) {
        String normalized = type == null ? "memory" : type.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            // case "yaml" -> new YamlVanishStorage(...);
            // case "sqlite" -> new SqliteVanishStorage(...);
            // case "redis" -> new RedisVanishStorage(...);
            default -> new MemoryVanishStorage();
        };
    }
}
