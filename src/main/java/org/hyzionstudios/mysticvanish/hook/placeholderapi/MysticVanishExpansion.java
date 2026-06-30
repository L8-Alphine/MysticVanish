package org.hyzionstudios.mysticvanish.hook.placeholderapi;

import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.List;
import java.util.Locale;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.manager.VisibilityManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;
import org.hyzionstudios.mysticvanish.model.VanishState;
import org.hyzionstudios.mysticvanish.permission.PermissionResolver;

public final class MysticVanishExpansion extends PlaceholderExpansion {
    private final VanishManager vanishManager;
    private final VisibilityManager visibilityManager;
    private final PermissionResolver permissionResolver;

    public MysticVanishExpansion(VanishManager vanishManager, VisibilityManager visibilityManager, PermissionResolver permissionResolver) {
        this.vanishManager = vanishManager;
        this.visibilityManager = visibilityManager;
        this.permissionResolver = permissionResolver;
    }

    @Override
    public String getIdentifier() {
        return "mysticvanish";
    }

    @Override
    public String getAuthor() {
        return "Hyzion Studios";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> getPlaceholders() {
        return List.of(
                "enabled",
                "status",
                "status_formatted",
                "icon",
                "level",
                "see_level",
                "profile",
                "hidden_from_count",
                "visible_to_count",
                "feature_no_entity_target",
                "feature_no_trigger_volume",
                "feature_silent_chests",
                "feature_silent_interactions",
                "feature_chat_protection",
                "feature_command_protection"
        );
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(PlayerRef player, String params) {
        if (player == null) {
            return "";
        }
        VanishState state = vanishManager.getState(player.getUuid());
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "enabled" -> String.valueOf(state.vanished());
            case "status" -> state.vanished() ? "vanished" : "visible";
            case "status_formatted" -> state.vanished() ? "&aVanished" : "&7Visible";
            case "icon" -> state.vanished() ? "V" : "-";
            case "level" -> String.valueOf(state.vanishLevel());
            case "see_level" -> String.valueOf(permissionResolver.resolveSeeLevel(player));
            case "profile" -> state.profile();
            case "hidden_from_count" -> String.valueOf(visibilityManager.getHiddenFromCount(player.getUuid()));
            case "visible_to_count" -> String.valueOf(visibilityManager.getVisibleToCount(player.getUuid()));
            case "feature_no_entity_target" -> bool(state.hasFeature(VanishFeature.NO_ENTITY_TARGET));
            case "feature_no_trigger_volume" -> bool(state.hasFeature(VanishFeature.NO_TRIGGER_VOLUME));
            case "feature_silent_chests" -> bool(state.hasFeature(VanishFeature.SILENT_CHESTS));
            case "feature_silent_interactions" -> bool(state.hasFeature(VanishFeature.SILENT_INTERACTIONS));
            case "feature_chat_protection" -> bool(state.hasFeature(VanishFeature.CHAT_PROTECTION));
            case "feature_command_protection" -> bool(state.hasFeature(VanishFeature.COMMAND_PROTECTION));
            default -> "";
        };
    }

    private static String bool(boolean value) {
        return value ? "true" : "false";
    }
}
