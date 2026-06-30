package org.hyzionstudios.mysticvanish.listener;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;

/**
 * Suppresses interaction-driven triggers for vanished players.
 *
 * <p>Server 0.5.6 exposes {@link PlayerInteractEvent} (cancellable) but no proximity trigger-volume
 * event, and no API to silence sound/particles without cancelling the whole action. So this listener
 * does what the platform allows: when a vanished player with {@link VanishFeature#NO_TRIGGER_VOLUME}
 * activates an <em>entity</em> (NPC / mob / scripted entity), the interaction is cancelled to stop
 * quest/dialogue/aggro triggers that would expose them. Block interactions (chests, doors) are left
 * functional. The noise-suppression side of {@code silent_interactions} is not achievable on this
 * platform version (no partial-suppression API), so it is intentionally not implemented here.
 */
public final class InteractionListener {
    private final VanishManager vanishManager;

    public InteractionListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    public void register(EventRegistry registry) {
        registry.registerGlobal(PlayerInteractEvent.class, this::onInteract);
    }

    // Entity#getUuid is the only id accessor on the interaction event in 0.5.6; both it and
    // Player#getPlayerRef are marked for removal. When the ECS UUID component (Entity.UUID) gains a
    // stable read path off the event's Ref<EntityStore>, switch to that.
    @SuppressWarnings("removal")
    private void onInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getTargetEntity() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !vanishManager.hasFeature(player.getUuid(), VanishFeature.NO_TRIGGER_VOLUME)) {
            return;
        }
        if (isActivation(event.getActionType())) {
            event.setCancelled(true);
        }
    }

    private static boolean isActivation(InteractionType type) {
        return type == InteractionType.Use || type == InteractionType.Primary || type == InteractionType.Secondary;
    }
}
