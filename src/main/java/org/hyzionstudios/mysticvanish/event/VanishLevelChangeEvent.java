package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;

public final class VanishLevelChangeEvent extends VanishEvent {
    private final int oldLevel;
    private final int newLevel;

    public VanishLevelChangeEvent(UUID playerId, int oldLevel, int newLevel) {
        super(playerId);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
