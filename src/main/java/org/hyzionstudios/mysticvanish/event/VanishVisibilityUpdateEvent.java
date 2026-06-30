package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;

public final class VanishVisibilityUpdateEvent extends VanishEvent {
    private final UUID viewerId;
    private final boolean visible;

    public VanishVisibilityUpdateEvent(UUID playerId, UUID viewerId, boolean visible) {
        super(playerId);
        this.viewerId = viewerId;
        this.visible = visible;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public boolean isVisible() {
        return visible;
    }
}
