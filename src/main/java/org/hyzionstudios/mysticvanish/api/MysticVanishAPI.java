package org.hyzionstudios.mysticvanish.api;

import java.util.UUID;
import org.hyzionstudios.mysticvanish.model.VanishState;

public interface MysticVanishAPI {
    boolean isVanished(UUID playerId);

    int getVanishLevel(UUID playerId);

    int getSeeLevel(UUID viewerId);

    boolean canSee(UUID viewerId, UUID targetId);

    void vanish(UUID playerId, int level);

    void unvanish(UUID playerId);

    void toggleVanish(UUID playerId);

    void updateVisibility(UUID playerId);

    void updateVisibilityFor(UUID viewerId);

    VanishState getState(UUID playerId);
}
