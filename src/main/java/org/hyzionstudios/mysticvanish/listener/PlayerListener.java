package org.hyzionstudios.mysticvanish.listener;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.permissions.PlayerPermissionChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.manager.VisibilityManager;

public final class PlayerListener {
    private final ConfigManager configManager;
    private final VanishManager vanishManager;
    private final VisibilityManager visibilityManager;

    public PlayerListener(ConfigManager configManager, VanishManager vanishManager, VisibilityManager visibilityManager) {
        this.configManager = configManager;
        this.vanishManager = vanishManager;
        this.visibilityManager = visibilityManager;
    }

    public void register(EventRegistry registry) {
        registry.register(PlayerConnectEvent.class, this::onConnect);
        registry.registerGlobal(PlayerDisconnectEvent.class, this::onDisconnect);
        registry.registerGlobal(PlayerPermissionChangeEvent.class, this::onPermissionChange);
        registry.registerGlobal(AddPlayerToWorldEvent.class, this::onAddedToWorld);
    }

    private void onConnect(PlayerConnectEvent event) {
        // AddPlayerToWorldEvent handles visibility once the client has a world.
    }

    private void onDisconnect(PlayerDisconnectEvent event) {
        UUID playerId = event.getPlayerRef().getUuid();
        vanishManager.getStoredState(playerId).ifPresent(state -> vanishManager.updateVisibility(state.playerId()));
        // Strip the glow before the entity is saved/unloaded, so it can't linger as an orphan on rejoin.
        vanishManager.clearGlow(playerId);
    }

    private void onPermissionChange(PlayerPermissionChangeEvent event) {
        if (configManager.getBoolean("settings.update_visibility_on_permission_change", true)) {
            vanishManager.refreshPermissions(event.getPlayerUuid());
        }
    }

    /**
     * Fires when a player enters a world, including world switches. Recompute visibility both ways: the
     * mover gets the correct hidden set for the new world, and existing players there get the right view
     * of the mover.
     */
    private void onAddedToWorld(AddPlayerToWorldEvent event) {
        if (!configManager.getBoolean("settings.update_visibility_on_world_change", true)) {
            return;
        }
        PlayerRef player = event.getHolder().getComponent(PlayerRef.getComponentType());
        if (player == null) {
            return;
        }
        UUID playerId = player.getUuid();
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            visibilityManager.updateVisibilityFor(playerId);
            visibilityManager.updateVisibility(playerId);
            vanishManager.refreshVisuals(playerId);
        }, 1, TimeUnit.SECONDS);
    }
}
