package org.hyzionstudios.mysticvanish.listener;

import java.util.UUID;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

/**
 * Query utility for the {@link VanishFeature#NO_ENTITY_TARGET} feature.
 *
 * <p>There is no entity-targeting / aggro event to subscribe to in Server 0.5.6, so the feature is
 * actually <em>enforced</em> by {@code org.hyzionstudios.mysticvanish.npc.VanishAttitudeSystem}, which
 * forces NPC attitude to {@code IGNORE} for vanished players. This class remains as a lightweight query
 * ("should mobs ignore this player?") for NPC/AI scripts and other mods that want to consult the state
 * directly through a stable entry point.
 */
public final class EntityTargetListener {
    private final VanishManager vanishManager;

    public EntityTargetListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    public boolean shouldSuppressTarget(UUID targetPlayerId) {
        return vanishManager.hasFeature(targetPlayerId, VanishFeature.NO_ENTITY_TARGET);
    }
}
