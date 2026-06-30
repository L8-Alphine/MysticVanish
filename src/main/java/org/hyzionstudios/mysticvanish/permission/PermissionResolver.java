package org.hyzionstudios.mysticvanish.permission;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

public final class PermissionResolver {
    public static final String USE = "mysticvanish.use";
    public static final String ADMIN = "mysticvanish.admin";
    public static final String TOGGLE_OTHERS = "mysticvanish.toggle.others";
    public static final String STATUS_OTHERS = "mysticvanish.status.others";
    public static final String RELOAD = "mysticvanish.reload";

    private final ConfigManager configManager;
    private final List<ExactPermissionSource> permissionSources;

    public PermissionResolver(ConfigManager configManager) {
        this.configManager = configManager;
        this.permissionSources = createPermissionSources();
    }

    public boolean hasUse(PermissionHolder holder) {
        return has(holder, USE);
    }

    public boolean canReload(PermissionHolder holder) {
        return has(holder, RELOAD);
    }

    public boolean canToggleOthers(PermissionHolder holder) {
        return has(holder, TOGGLE_OTHERS);
    }

    public boolean canStatusOthers(PermissionHolder holder) {
        return has(holder, STATUS_OTHERS);
    }

    public int resolveVanishLevel(PermissionHolder holder) {
        return resolveHighest(holder, configManager.getLevelPrefix(), configManager.getDefaultVanishLevel());
    }

    public int resolveSeeLevel(PermissionHolder holder) {
        return resolveHighest(holder, configManager.getSeePrefix(), configManager.getDefaultSeeLevel());
    }

    public boolean canUseVanishLevel(PermissionHolder holder, int level) {
        return resolveVanishLevel(holder) >= level;
    }

    public boolean hasFeature(PermissionHolder holder, VanishFeature feature) {
        if (!configManager.isFeatureEnabled(feature)) {
            return false;
        }
        return !configManager.doesFeatureRequirePermission(feature) || has(holder, configManager.getFeaturePermission(feature));
    }

    public boolean has(PermissionHolder holder, String permission) {
        return hasExact(holder, permission) || hasExact(holder, ADMIN);
    }

    /**
     * Resolves the highest level the holder is explicitly granted via {@code prefix + tier} permissions.
     * Wildcard nodes are intentionally ignored so platform admin groups never inherit vanish or see power.
     */
    private int resolveHighest(PermissionHolder holder, String prefix, int fallback) {
        if (holder == null) {
            return fallback;
        }
        int highest = fallback;
        for (Integer candidate : configManager.getProfileLevels()) {
            if (candidate != null && candidate > highest && hasExact(holder, prefix + candidate)) {
                highest = candidate;
            }
        }
        if (!(holder instanceof CommandSender sender)) {
            return highest;
        }
        for (ExactPermissionSource source : permissionSources) {
            highest = source.highestLevel(sender.getUuid(), prefix, highest);
        }
        return highest;
    }

    private boolean hasExact(PermissionHolder holder, String permission) {
        if (!(holder instanceof CommandSender sender)) {
            return false;
        }
        return hasExact(sender.getUuid(), permission);
    }

    private boolean hasExact(UUID playerId, String permission) {
        for (ExactPermissionSource source : permissionSources) {
            Boolean result = source.hasExact(playerId, permission);
            if (result != null) {
                return result;
            }
        }
        return false;
    }

    private List<ExactPermissionSource> createPermissionSources() {
        List<ExactPermissionSource> sources = new ArrayList<>();
        ExactPermissionSource luckPerms = createLuckPermsSource();
        if (luckPerms != null) {
            sources.add(luckPerms);
        }
        sources.add(new HytalePermissionSource());
        return List.copyOf(sources);
    }

    private ExactPermissionSource createLuckPermsSource() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            Class<?> sourceClass = Class.forName("org.hyzionstudios.mysticvanish.permission.LuckPermsPermissionSource");
            return (ExactPermissionSource) sourceClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }
}
