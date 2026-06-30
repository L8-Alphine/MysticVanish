package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;

public final class VanishCommandBlockedEvent extends VanishEvent {
    private final String command;

    public VanishCommandBlockedEvent(UUID playerId, String command) {
        super(playerId);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
