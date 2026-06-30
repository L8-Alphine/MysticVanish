package org.hyzionstudios.mysticvanish.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for the default config, messages, and placeholder docs. Files are generated
 * from these maps on first run (see {@link ConfigManager}); nothing is shipped as a bundled resource.
 * Ordered {@link LinkedHashMap}s so the generated JSON keeps a readable, stable key order.
 */
final class ConfigDefaults {
    private ConfigDefaults() {
    }

    static Map<String, Object> config() {
        return map(
                "settings", map(
                        // 0 = no baseline; vanish level comes solely from mysticvanish.level.<N> permissions.
                        // A player who resolves to level 0 cannot vanish (requires mysticvanish.level.1+).
                        "default_vanish_level", 0,
                        "default_see_level", 0,
                        "hide_from_tab_list", true,
                        "hide_from_map_markers", true,
                        "hide_nametag", true,
                        "hide_nameplate", true,
                        "hide_join_leave_messages", true,
                        "update_visibility_on_join", true,
                        "update_visibility_on_world_change", true,
                        "update_visibility_on_permission_change", true,
                        "visibility_refresh_interval_seconds", 0),
                "storage", map(
                        "type", "memory"),
                "aura", map(
                        "enabled", true,
                        "one_way_particle_system_id", "Drop_Uncommon",
                        "mutual_particle_system_id", "Drop_Uncommon",
                        "particle_system_examples", List.of("Dance_Light_Player", "Drop_Common", "Drop_Uncommon", "Drop_Epic", "Drop_Legendary"),
                        "one_way_color", "#FF5555",
                        "mutual_color", "#55FFFF",
                        "interval_ticks", 10,
                        "duration_seconds", 0.75,
                        "scale", 1.0,
                        "y_offset", 1.0,
                        "max_distance", 64.0,
                        // Scales aura size + colour brightness by the level gap (viewer see-level minus
                        // target vanish-level). Larger gap = bigger, brighter aura.
                        "scaling", map(
                                "enabled", true,
                                "max_gap", 40,
                                "min_scale_factor", 0.6,
                                "max_scale_factor", 1.6,
                                "min_brightness", 0.5,
                                "max_brightness", 1.0)),
                "glow", map(
                        // Persistent colour highlight on the vanished player, visible to anyone who can see
                        // them. Pick a preset colour: cyan, blue, green, red, purple, gold, white, pink.
                        // (Each maps to a bundled player-safe tint effect.)
                        "enabled", true,
                        "color", "cyan"),
                "hud", map(
                        "enabled", true,
                        // Internal key for this HUD slot (used to add/clear it). Arbitrary but unique.
                        "hud_id", "mysticvanish:vanish",
                        "z_order", 0,
                        // Path (relative to Common/UI/Custom/) of the .ui layout the server instantiates
                        // while vanished. Bundled in this mod's asset pack at
                        // Common/UI/Custom/Hud/MysticVanishHud.ui. Leave blank to disable the HUD.
                        "ui_file", "Hud/MysticVanishHud.ui",
                        // Element inside that .ui whose text the server sets. Plain text only ({level}/{see}
                        // substituted; &-codes are stripped) -- colour/size come from the .ui Style block.
                        "text_selector", "#Status.Text",
                        "text", "VANISHED  Level {level}"),
                "permissions", map(
                        "level_prefix", "mysticvanish.level.",
                        "see_prefix", "mysticvanish.see."),
                "features", map(
                        "no_entity_target", feature("mysticvanish.feature.no_entity_target"),
                        "no_trigger_volume", feature("mysticvanish.feature.no_trigger_volume"),
                        "silent_chests", feature("mysticvanish.feature.silent_chests"),
                        "silent_interactions", feature("mysticvanish.feature.silent_interactions"),
                        "chat_protection", featureWithMode("mysticvanish.feature.chat_protection", "block"),
                        "command_protection", commandProtection(),
                        "hide_tab", feature("mysticvanish.feature.hide_tab"),
                        "hide_map", feature("mysticvanish.feature.hide_map"),
                        "hide_nameplate", feature("mysticvanish.feature.hide_nameplate"),
                        "hide_commands", feature("mysticvanish.feature.hide_commands")),
                "placeholderapi", map(
                        "enabled", true,
                        "register_expansion", true,
                        "parse_placeholders_in_messages", true),
                "profiles", map(
                        "helper", profile(10, 10),
                        "moderator", profile(25, 25),
                        "admin", profile(75, 75),
                        "owner", profile(100, 100)));
    }

    static Map<String, Object> messages() {
        return map(
                "prefix", "&8[&bMysticVanish&8]&r",
                "vanish_enabled", "&8[&bMysticVanish&8] &aYou are now vanished at level {level}.",
                "vanish_disabled", "&8[&bMysticVanish&8] &7You are now visible.",
                "vanish_level_changed", "&8[&bMysticVanish&8] &aYour vanish level is now {level}.",
                "target_vanish_enabled", "&8[&bMysticVanish&8] &a{player} is now vanished at level {level}.",
                "target_vanish_disabled", "&8[&bMysticVanish&8] &7{player} is now visible.",
                "no_permission", "&8[&bMysticVanish&8] &cYou do not have permission to do that.",
                "no_vanish_permission", "&8[&bMysticVanish&8] &cYou do not have permission to vanish. Ask an admin for a mysticvanish.level.<n> rank.",
                "target_no_vanish_permission", "&8[&bMysticVanish&8] &c{player} has no vanish rank (mysticvanish.level.<n>).",
                "vanish_level_from_permission", "&8[&bMysticVanish&8] &7Your vanish level is set by your mysticvanish.level.<n> permission, not by the command.",
                "invalid_level", "&8[&bMysticVanish&8] &cThat vanish level is invalid.",
                "player_not_found", "&8[&bMysticVanish&8] &cThat player could not be found.",
                "player_only", "&8[&bMysticVanish&8] &cOnly players can use that command.",
                "reload_complete", "&8[&bMysticVanish&8] &aConfiguration and messages reloaded.",
                "chat_blocked", "&8[&bMysticVanish&8] &cYou are vanished. Public chat was blocked to prevent accidental exposure.",
                "command_blocked", "&8[&bMysticVanish&8] &cThat command was blocked while vanished to prevent accidental exposure.",
                "status", "&8[&bMysticVanish&8] &7Status: &f{status} &7Level: &f{level} &7See: &f{see_level} &7Profile: &f{profile}",
                "target_status", "&8[&bMysticVanish&8] &7{player}: &f{status} &7Level: &f{level} &7Profile: &f{profile}",
                "vchat_sent", "&8[&bMysticVanish&8] &7Sent public chat while vanished: &f{message}");
    }

    static Map<String, Object> placeholders() {
        return map(
                "placeholders", map(
                        "enabled", "%mysticvanish_enabled%",
                        "status", "%mysticvanish_status%",
                        "status_formatted", "%mysticvanish_status_formatted%",
                        "icon", "%mysticvanish_icon%",
                        "level", "%mysticvanish_level%",
                        "see_level", "%mysticvanish_see_level%",
                        "profile", "%mysticvanish_profile%",
                        "hidden_from_count", "%mysticvanish_hidden_from_count%",
                        "visible_to_count", "%mysticvanish_visible_to_count%"));
    }

    private static Map<String, Object> feature(String permission) {
        return map("enabled", true, "require_permission", true, "permission", permission);
    }

    private static Map<String, Object> featureWithMode(String permission, String mode) {
        Map<String, Object> feature = feature(permission);
        feature.put("mode", mode);
        return feature;
    }

    private static Map<String, Object> commandProtection() {
        Map<String, Object> feature = featureWithMode("mysticvanish.feature.command_protection", "block");
        feature.put("blocked_commands", List.of("msg", "tell", "r", "reply", "me", "tpa", "tpahere", "trade", "party invite"));
        return feature;
    }

    private static Map<String, Object> profile(int vanishLevel, int seeLevel) {
        return map("vanish_level", vanishLevel, "see_level", seeLevel);
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
