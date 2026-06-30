package org.hyzionstudios.mysticvanish.event;

import java.util.UUID;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

public final class VanishFeatureCheckEvent extends VanishEvent {
    private final VanishFeature feature;
    private final boolean allowed;

    public VanishFeatureCheckEvent(UUID playerId, VanishFeature feature, boolean allowed) {
        super(playerId);
        this.feature = feature;
        this.allowed = allowed;
    }

    public VanishFeature getFeature() {
        return feature;
    }

    public boolean isAllowed() {
        return allowed;
    }
}
