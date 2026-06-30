package org.hyzionstudios.mysticvanish.config;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import java.util.Map;
import java.util.logging.Level;
import org.hyzionstudios.mysticvanish.util.TextUtil;

public final class MessageManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public MessageManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void load() {
        configManager.loadMessages();
    }

    public Message message(String key) {
        return message(key, Map.of());
    }

    public Message message(String key, Map<String, String> placeholders) {
        String text = configManager.getMessage(key, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return TextUtil.colorize(text);
    }

    public void reload() {
        load();
        plugin.getLogger().at(Level.INFO).log("MysticVanish messages reloaded.");
    }
}
