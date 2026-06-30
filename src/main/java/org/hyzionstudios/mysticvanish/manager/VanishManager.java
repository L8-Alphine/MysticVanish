package org.hyzionstudios.mysticvanish.manager;

import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.hyzionstudios.mysticvanish.api.MysticVanishAPI;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.event.VanishDisableEvent;
import org.hyzionstudios.mysticvanish.event.VanishEnableEvent;
import org.hyzionstudios.mysticvanish.event.VanishEvent;
import org.hyzionstudios.mysticvanish.event.VanishLevelChangeEvent;
import org.hyzionstudios.mysticvanish.glow.VanishGlowController;
import org.hyzionstudios.mysticvanish.hud.VanishHudController;
import org.hyzionstudios.mysticvanish.model.VanishFeature;
import org.hyzionstudios.mysticvanish.model.VanishState;
import org.hyzionstudios.mysticvanish.permission.PermissionResolver;
import org.hyzionstudios.mysticvanish.storage.MemoryVanishStorage;
import org.hyzionstudios.mysticvanish.storage.VanishStorage;

public final class VanishManager implements MysticVanishAPI {
    private final VanishStorage storage;
    private final ConfigManager configManager;
    private final PermissionResolver permissionResolver;
    private VisibilityManager visibilityManager;
    private VanishHudController hudController;
    private VanishGlowController glowController;

    public VanishManager(VanishStorage storage, ConfigManager configManager, PermissionResolver permissionResolver) {
        this.storage = storage;
        this.configManager = configManager;
        this.permissionResolver = permissionResolver;
    }

    public void setVisibilityManager(VisibilityManager visibilityManager) {
        this.visibilityManager = visibilityManager;
    }

    public void setHudController(VanishHudController hudController) {
        this.hudController = hudController;
    }

    public void setGlowController(VanishGlowController glowController) {
        this.glowController = glowController;
    }

    @Override
    public boolean isVanished(UUID playerId) {
        return getState(playerId).vanished();
    }

    @Override
    public int getVanishLevel(UUID playerId) {
        return getState(playerId).vanishLevel();
    }

    @Override
    public int getSeeLevel(UUID viewerId) {
        return visibilityManager == null ? configManager.getDefaultSeeLevel() : visibilityManager.getSeeLevel(viewerId);
    }

    @Override
    public boolean canSee(UUID viewerId, UUID targetId) {
        return visibilityManager == null || visibilityManager.canSee(viewerId, targetId);
    }

    @Override
    public void vanish(UUID playerId, int level) {
        applyVanish(playerId, level, resolveFeatures(playerId));
    }

    public void vanish(PlayerRef player, int level) {
        applyVanish(player.getUuid(), level, resolveFeatures(player));
    }

    /** Single path for enabling/relevelling vanish, so lifecycle events fire regardless of caller. */
    private void applyVanish(UUID playerId, int level, EnumSet<VanishFeature> features) {
        // Level 0 (or below) is the "not vanished" baseline: vanishing requires mysticvanish.level.1+.
        // Enforced here so command, API, and toggle callers all share the same rule.
        if (level < 1) {
            return;
        }
        VanishState previous = getState(playerId);
        storage.save(previous.withVanish(true, level, features));
        if (!previous.vanished()) {
            dispatch(VanishEnableEvent.class, playerId, new VanishEnableEvent(playerId, level));
        } else if (previous.vanishLevel() != level) {
            dispatch(VanishLevelChangeEvent.class, playerId, new VanishLevelChangeEvent(playerId, previous.vanishLevel(), level));
        }
        updateVisibility(playerId);
        if (hudController != null) {
            hudController.show(playerId, level);
        }
        if (glowController != null) {
            glowController.apply(playerId, true);
        }
    }

    @Override
    public void unvanish(UUID playerId) {
        VanishState previous = getState(playerId);
        storage.save(previous.withVanish(false, 0, EnumSet.noneOf(VanishFeature.class)));
        if (previous.vanished()) {
            dispatch(VanishDisableEvent.class, playerId, new VanishDisableEvent(playerId));
        }
        updateVisibility(playerId);
        if (hudController != null) {
            hudController.hide(playerId);
        }
        if (glowController != null) {
            glowController.apply(playerId, false);
        }
    }

    private <T extends VanishEvent> void dispatch(Class<T> type, UUID key, T event) {
        EventBus bus = HytaleServer.get().getEventBus();
        if (bus != null) {
            bus.dispatchFor(type, key).dispatch(event);
        }
    }

    @Override
    public void toggleVanish(UUID playerId) {
        if (isVanished(playerId)) {
            unvanish(playerId);
            return;
        }
        PlayerRef player = Universe.get().getPlayer(playerId);
        int level = player == null ? configManager.getDefaultVanishLevel() : permissionResolver.resolveVanishLevel(player);
        vanish(playerId, level);
    }

    public void toggleVanish(PlayerRef player) {
        if (isVanished(player.getUuid())) {
            unvanish(player.getUuid());
            return;
        }
        vanish(player, permissionResolver.resolveVanishLevel(player));
    }

    @Override
    public void updateVisibility(UUID playerId) {
        if (visibilityManager != null) {
            visibilityManager.updateVisibility(playerId);
        }
    }

    @Override
    public void updateVisibilityFor(UUID viewerId) {
        if (visibilityManager != null) {
            visibilityManager.updateVisibilityFor(viewerId);
        }
    }

    /** Removes any vanish glow from the player; used on quit so an infinite effect is never left saved on the entity. */
    public void clearGlow(UUID playerId) {
        if (glowController != null) {
            glowController.apply(playerId, false);
        }
    }

    /** Re-syncs the vanish HUD and glow to the player's current state; used on join/world change where client state resets. */
    public void refreshVisuals(UUID playerId) {
        VanishState state = getState(playerId);
        if (hudController != null) {
            if (state.vanished()) {
                hudController.show(playerId, state.vanishLevel());
            } else {
                hudController.hide(playerId);
            }
        }
        if (glowController != null) {
            glowController.apply(playerId, state.vanished());
        }
    }

    public void refreshPermissions(UUID playerId) {
        PlayerRef player = Universe.get().getPlayer(playerId);
        if (player == null) {
            return;
        }
        updateVisibilityFor(playerId);
        VanishState state = getState(playerId);
        if (state.vanished()) {
            applyVanish(playerId, permissionResolver.resolveVanishLevel(player), resolveFeatures(player));
        }
    }

    @Override
    public VanishState getState(UUID playerId) {
        return storage.load(playerId).orElseGet(() -> VanishState.visible(playerId));
    }

    public Collection<VanishState> getKnownStates() {
        if (storage instanceof MemoryVanishStorage memoryVanishStorage) {
            return memoryVanishStorage.allStates();
        }
        return java.util.List.of();
    }

    public Optional<VanishState> getStoredState(UUID playerId) {
        return storage.load(playerId);
    }

    public boolean hasFeature(UUID playerId, VanishFeature feature) {
        VanishState state = getState(playerId);
        return state.vanished() && state.hasFeature(feature);
    }

    private EnumSet<VanishFeature> resolveFeatures(UUID playerId) {
        PlayerRef player = Universe.get().getPlayer(playerId);
        return player == null ? EnumSet.noneOf(VanishFeature.class) : resolveFeatures(player);
    }

    private EnumSet<VanishFeature> resolveFeatures(PlayerRef player) {
        EnumSet<VanishFeature> features = EnumSet.noneOf(VanishFeature.class);
        for (VanishFeature feature : VanishFeature.values()) {
            if (permissionResolver.hasFeature(player, feature)) {
                features.add(feature);
            }
        }
        return features;
    }
}
