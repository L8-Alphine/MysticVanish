package org.hyzionstudios.mysticvanish.npc;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.StoreSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.Blackboard;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.AttitudeView;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;
import com.hypixel.hytale.server.npc.role.Role;
import java.util.logging.Level;
import org.hyzionstudios.mysticvanish.listener.EntityTargetListener;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

/**
 * Implements {@link VanishFeature#NO_ENTITY_TARGET} by hooking the NPC attitude system.
 *
 * <p>NPC hostile-target sensors gate candidates through {@code EntityFilterAttitude}, which asks
 * {@code WorldSupport.getAttitude(npc, target)} and matches against the sensor's allowed attitudes
 * (typically {@code HOSTILE}). Attitude is resolved by a prioritised chain of {@link IAttitudeProvider}s
 * on each NPC's {@code AttitudeView} (the built-in reputation system registers one the same way). We
 * register a provider at {@link IAttitudeProvider#OVERRIDE_PRIORITY} that forces {@link Attitude#IGNORE}
 * for vanished players holding the feature, so NPCs stop perceiving them as valid targets — globally,
 * with no per-NPC asset configuration.
 *
 * <p>Returning {@code null} abstains, deferring to lower-priority providers (so non-vanished players are
 * unaffected). This couples to internal {@code server.npc.*} APIs; if they shift in a future Hytale
 * build, this class is the single point to adjust. Note: already-engaged NPCs disengage when they next
 * re-evaluate attitude — newly attempted target acquisition is blocked immediately.
 */
public final class VanishAttitudeSystem extends StoreSystem<EntityStore> {
    private final EntityTargetListener entityTarget;
    private final HytaleLogger logger;
    private final ResourceType<EntityStore, Blackboard> blackboardResourceType = Blackboard.getResourceType();
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    public VanishAttitudeSystem(EntityTargetListener entityTarget, HytaleLogger logger) {
        this.entityTarget = entityTarget;
        this.logger = logger;
    }

    @Override
    public void onSystemAddedToStore(Store<EntityStore> store) {
        Blackboard blackboard = store.getResource(blackboardResourceType);
        if (blackboard == null) {
            logger.at(Level.WARNING).log("NPC blackboard unavailable; no_entity_target will not affect mob targeting in this store.");
            return;
        }
        AttitudeView view = blackboard.getView(AttitudeView.class, 0L);
        if (view == null) {
            logger.at(Level.WARNING).log("NPC attitude view unavailable; no_entity_target will not affect mob targeting in this store.");
            return;
        }
        view.registerProvider(IAttitudeProvider.OVERRIDE_PRIORITY, this::resolveAttitude);
    }

    @Override
    public void onSystemRemovedFromStore(Store<EntityStore> store) {
        // The provider's lifetime is tied to the store's blackboard view; nothing to unwind here.
    }

    private Attitude resolveAttitude(Ref<EntityStore> self, Role role, Ref<EntityStore> target, ComponentAccessor<EntityStore> accessor) {
        PlayerRef targetPlayer = accessor.getComponent(target, playerRefComponentType);
        if (targetPlayer == null) {
            return null;
        }
        return entityTarget.shouldSuppressTarget(targetPlayer.getUuid()) ? Attitude.IGNORE : null;
    }
}
