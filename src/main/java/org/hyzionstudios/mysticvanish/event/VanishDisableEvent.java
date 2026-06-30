package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;

public final class VanishDisableEvent extends VanishEvent {
    public VanishDisableEvent(UUID playerId) {
        super(playerId);
    }
}
