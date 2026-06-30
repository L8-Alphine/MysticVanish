package org.hyzionstudios.mysticvanish.model;

public enum VanishFeature {
    NO_ENTITY_TARGET("no_entity_target", "mysticvanish.feature.no_entity_target"),
    NO_TRIGGER_VOLUME("no_trigger_volume", "mysticvanish.feature.no_trigger_volume"),
    SILENT_CHESTS("silent_chests", "mysticvanish.feature.silent_chests"),
    SILENT_INTERACTIONS("silent_interactions", "mysticvanish.feature.silent_interactions"),
    CHAT_PROTECTION("chat_protection", "mysticvanish.feature.chat_protection"),
    COMMAND_PROTECTION("command_protection", "mysticvanish.feature.command_protection"),
    HIDE_TAB("hide_tab", "mysticvanish.feature.hide_tab"),
    HIDE_MAP("hide_map", "mysticvanish.feature.hide_map"),
    HIDE_NAMEPLATE("hide_nameplate", "mysticvanish.feature.hide_nameplate"),
    HIDE_COMMANDS("hide_commands", "mysticvanish.feature.hide_commands");

    private final String configKey;
    private final String defaultPermission;

    VanishFeature(String configKey, String defaultPermission) {
        this.configKey = configKey;
        this.defaultPermission = defaultPermission;
    }

    public String configKey() {
        return configKey;
    }

    public String defaultPermission() {
        return defaultPermission;
    }
}
