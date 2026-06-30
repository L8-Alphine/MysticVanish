package org.hyzionstudios.mysticvanish.listener;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

public final class CommandListener {
    private final VanishManager vanishManager;

    public CommandListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    public boolean shouldBlockCommand(UUID playerId, String rawCommand, List<String> blockedCommands) {
        if (!vanishManager.hasFeature(playerId, VanishFeature.COMMAND_PROTECTION)) {
            return false;
        }
        String normalized = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        normalized = normalized.toLowerCase(Locale.ROOT).trim();
        for (String blockedCommand : blockedCommands) {
            String blocked = blockedCommand.toLowerCase(Locale.ROOT);
            if (normalized.equals(blocked) || normalized.startsWith(blocked + " ")) {
                return true;
            }
        }
        return false;
    }
}
