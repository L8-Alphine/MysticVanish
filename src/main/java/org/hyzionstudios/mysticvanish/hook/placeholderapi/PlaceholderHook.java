package org.hyzionstudios.mysticvanish.hook.placeholderapi;

import java.util.logging.Level;
import org.hyzionstudios.mysticvanish.MysticVanishPlugin;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.manager.VisibilityManager;
import org.hyzionstudios.mysticvanish.permission.PermissionResolver;

/**
 * Optional PlaceholderAPI integration. PlaceholderAPI is a {@code compileOnly} dependency that may be
 * absent at runtime, so {@link MysticVanishExpansion} (which extends a PAPI base class) is <em>only</em>
 * instantiated inside the guarded {@link #registerIfAvailable()} block — never in the constructor.
 * If PlaceholderAPI is not installed, touching the expansion throws {@link NoClassDefFoundError} /
 * {@link LinkageError}, which is caught here so the plugin loads normally without placeholders.
 */
public final class PlaceholderHook {
    private final MysticVanishPlugin plugin;
    private final ConfigManager configManager;
    private final VanishManager vanishManager;
    private final VisibilityManager visibilityManager;
    private final PermissionResolver permissionResolver;
    private MysticVanishExpansion expansion;
    private boolean registered;

    public PlaceholderHook(MysticVanishPlugin plugin, ConfigManager configManager, VanishManager vanishManager, VisibilityManager visibilityManager, PermissionResolver permissionResolver) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.vanishManager = vanishManager;
        this.visibilityManager = visibilityManager;
        this.permissionResolver = permissionResolver;
    }

    public void registerIfAvailable() {
        if (!configManager.getBoolean("placeholderapi.enabled", true) || !configManager.getBoolean("placeholderapi.register_expansion", true)) {
            return;
        }
        try {
            MysticVanishExpansion candidate = new MysticVanishExpansion(vanishManager, visibilityManager, permissionResolver);
            if (candidate.register()) {
                expansion = candidate;
                registered = true;
                plugin.getLogger().at(Level.INFO).log("Registered PlaceholderAPI expansion.");
            }
        } catch (LinkageError error) {
            plugin.getLogger().at(Level.INFO).log("PlaceholderAPI not installed; placeholders disabled.");
        }
    }

    public void unregister() {
        if (!registered || expansion == null) {
            return;
        }
        try {
            expansion.unregister();
        } catch (LinkageError ignored) {
            // PlaceholderAPI was unloaded before us; nothing to clean up.
        }
        registered = false;
        expansion = null;
    }
}
