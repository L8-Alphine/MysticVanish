package org.hyzionstudios.mysticvanish.hook;

import com.hypixel.hytale.builtin.triggervolumes.event.TriggerVolumeEvent;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
import java.util.UUID;
import org.hyzionstudios.mysticvanish.event.VanishFeatureCheckEvent;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

/**
 * Observes {@link TriggerVolumeEvent} for vanished players holding {@link VanishFeature#NO_TRIGGER_VOLUME}.
 *
 * <p>In Server 0.5.6 {@code TriggerVolumeEvent} is <em>not</em> cancellable, so the volume cannot be
 * prevented from firing from here. What we can do is surface a {@link VanishFeatureCheckEvent} keyed to
 * the actor so server scripts / other mods can choose to ignore the vanished entity (skip the cutscene,
 * trap, quest step, etc.). If a cancellable trigger event appears later, cancel it inside {@link #onTrigger}.
 */
public final class TriggerVolumeHook {
    private final VanishManager vanishManager;
    private EventBus eventBus;

    public TriggerVolumeHook(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    public void register(EventRegistry registry, EventBus eventBus) {
        this.eventBus = eventBus;
        registry.registerGlobal(TriggerVolumeEvent.class, this::onTrigger);
    }

    private void onTrigger(TriggerVolumeEvent event) {
        UUID entityId = event.getEntityUuid();
        if (entityId == null || !shouldSuppressTrigger(entityId)) {
            return;
        }
        if (eventBus != null) {
            eventBus.dispatchFor(VanishFeatureCheckEvent.class, entityId)
                    .dispatch(new VanishFeatureCheckEvent(entityId, VanishFeature.NO_TRIGGER_VOLUME, false));
        }
    }

    public boolean shouldSuppressTrigger(UUID playerId) {
        return vanishManager.hasFeature(playerId, VanishFeature.NO_TRIGGER_VOLUME);
    }
}
