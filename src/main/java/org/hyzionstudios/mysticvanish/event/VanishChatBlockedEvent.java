package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;

public final class VanishChatBlockedEvent extends VanishEvent {
    private final String message;
    private final String mode;

    public VanishChatBlockedEvent(UUID playerId, String message, String mode) {
        super(playerId);
        this.message = message;
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public String getMode() {
        return mode;
    }
}
