package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;

public final class VanishEnableEvent extends VanishEvent {
    private final int level;

    public VanishEnableEvent(UUID playerId, int level) {
        super(playerId);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
