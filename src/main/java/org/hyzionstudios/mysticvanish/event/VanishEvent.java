package org.hyzionstudios.mysticvanish.event;

import com.hypixel.hytale.event.IEvent;
import java.util.UUID;

public abstract class VanishEvent implements IEvent<UUID> {
    private final UUID playerId;

    protected VanishEvent(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
