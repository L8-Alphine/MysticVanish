package org.hyzionstudios.mysticvanish.manager;

import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.protocol.packets.interface_.AddToServerPlayerList;
import com.hypixel.hytale.protocol.packets.interface_.RemoveFromServerPlayerList;
import com.hypixel.hytale.protocol.packets.interface_.ServerPlayerListPlayer;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.event.VanishVisibilityUpdateEvent;
import org.hyzionstudios.mysticvanish.model.VanishFeature;
import org.hyzionstudios.mysticvanish.model.VanishState;
import org.hyzionstudios.mysticvanish.permission.PermissionResolver;
import org.hyzionstudios.mysticvanish.util.WorldThread;

/**
 * Centralises the one visibility rule ({@code viewer.seeLevel >= target.vanishLevel}) and applies it
 * to the client.
 *
 * <p>A player's {@code HiddenPlayersManager} must only be mutated on the thread that owns that player,
 * so every mutating path here is funnelled through {@link WorldThread#run}: if the caller is already on
 * the viewer's world thread (the common event-driven case) the work runs inline, otherwise it is queued
 * onto that world. This makes off-thread callers — the periodic safety-sweep and command-triggered
 * toggles — safe without blocking them. Read-only queries ({@link #canSee}, counts) do not mutate
 * client state and run on the calling thread.
 */
public final class VisibilityManager implements AutoCloseable {
    private final ConfigManager configManager;
    private final PermissionResolver permissionResolver;
    private final VanishManager vanishManager;
    private final Set<PlayerListKey> hiddenFromPlayerList = ConcurrentHashMap.newKeySet();

    public VisibilityManager(ConfigManager configManager, PermissionResolver permissionResolver, VanishManager vanishManager) {
        this.configManager = configManager;
        this.permissionResolver = permissionResolver;
        this.vanishManager = vanishManager;
    }

    public boolean canSee(UUID viewerId, UUID targetId) {
        if (viewerId == null || targetId == null || viewerId.equals(targetId)) {
            return true;
        }
        VanishState targetState = vanishManager.getState(targetId);
        if (!targetState.vanished()) {
            return true;
        }
        return getSeeLevel(viewerId) >= targetState.vanishLevel();
    }

    public int getSeeLevel(UUID viewerId) {
        PlayerRef viewer = Universe.get().getPlayer(viewerId);
        return viewer == null ? configManager.getDefaultSeeLevel() : permissionResolver.resolveSeeLevel(viewer);
    }

    public void updateVisibility(UUID targetId) {
        for (PlayerRef viewer : onlinePlayers()) {
            WorldThread.run(viewer, () -> applyVisibility(viewer, targetId));
        }
    }

    public void updateVisibilityFor(UUID viewerId) {
        PlayerRef viewer = Universe.get().getPlayer(viewerId);
        if (viewer == null) {
            return;
        }
        WorldThread.run(viewer, () -> applyAllTargets(viewer));
    }

    public void updateAllVisibility() {
        for (PlayerRef viewer : onlinePlayers()) {
            WorldThread.run(viewer, () -> applyAllTargets(viewer));
        }
    }

    public int getHiddenFromCount(UUID targetId) {
        int count = 0;
        for (PlayerRef viewer : onlinePlayers()) {
            if (!canSee(viewer.getUuid(), targetId)) {
                count++;
            }
        }
        return count;
    }

    public int getVisibleToCount(UUID targetId) {
        int count = 0;
        for (PlayerRef viewer : onlinePlayers()) {
            if (canSee(viewer.getUuid(), targetId)) {
                count++;
            }
        }
        return count;
    }

    private void applyAllTargets(PlayerRef viewer) {
        if (!viewer.isValid()) {
            return;
        }
        for (VanishState state : vanishManager.getKnownStates()) {
            applyVisibility(viewer, state.playerId());
        }
        for (PlayerRef target : onlinePlayers()) {
            applyVisibility(viewer, target.getUuid());
        }
    }

    private void applyVisibility(PlayerRef viewer, UUID targetId) {
        if (!viewer.isValid()) {
            return;
        }
        VanishState targetState = vanishManager.getState(targetId);
        HiddenPlayersManager hidden = viewer.getHiddenPlayersManager();
        boolean shouldSee = viewer.getUuid().equals(targetId) || canSee(viewer.getUuid(), targetId);
        applyPlayerListVisibility(viewer, targetId, targetState, shouldSee);
        // Skip when already in the desired state: avoids redundant client packets and event spam.
        if (shouldSee != hidden.isPlayerHidden(targetId)) {
            return;
        }
        if (shouldSee) {
            hidden.showPlayer(targetId);
        } else {
            hidden.hidePlayer(targetId);
        }
        dispatchVisibilityUpdate(targetId, viewer.getUuid(), shouldSee);
    }

    private void applyPlayerListVisibility(PlayerRef viewer, UUID targetId, VanishState targetState, boolean shouldSee) {
        if (viewer.getUuid().equals(targetId) || !configManager.getBoolean("settings.hide_from_tab_list", true)) {
            return;
        }
        if (!isClientReady(viewer)) {
            return;
        }
        boolean hideFromList = !shouldSee && targetState.vanished() && targetState.hasFeature(VanishFeature.HIDE_TAB);
        PlayerListKey key = new PlayerListKey(viewer.getUuid(), targetId);
        if (hideFromList) {
            if (hiddenFromPlayerList.add(key)) {
                viewer.getPacketHandler().write(new RemoveFromServerPlayerList(new UUID[] { targetId }));
            }
            return;
        }
        if (!hiddenFromPlayerList.remove(key)) {
            return;
        }
        PlayerRef target = Universe.get().getPlayer(targetId);
        if (target != null && target.isValid()) {
            viewer.getPacketHandler().write(new AddToServerPlayerList(new ServerPlayerListPlayer[] {
                    new ServerPlayerListPlayer(target.getUuid(), target.getUsername(), target.getWorldUuid(), 0)
            }));
        }
    }

    private boolean isClientReady(PlayerRef player) {
        CompletableFuture<Void> readyForChunks = player.getPacketHandler().getClientReadyForChunksFuture();
        return readyForChunks != null && readyForChunks.isDone();
    }

    private void dispatchVisibilityUpdate(UUID targetId, UUID viewerId, boolean visible) {
        EventBus bus = HytaleServer.get().getEventBus();
        if (bus != null) {
            bus.dispatchFor(VanishVisibilityUpdateEvent.class, targetId)
                    .dispatch(new VanishVisibilityUpdateEvent(targetId, viewerId, visible));
        }
    }

    /** Snapshot copy so off-thread iteration cannot hit concurrent join/leave modification. */
    private Collection<PlayerRef> onlinePlayers() {
        return new ArrayList<>(Universe.get().getPlayers());
    }

    @Override
    public void close() {
        hiddenFromPlayerList.clear();
    }

    private record PlayerListKey(UUID viewerId, UUID targetId) {
    }
}
