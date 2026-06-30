package org.hyzionstudios.mysticvanish.util;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.UUID;

/**
 * Runs player-directed work on the thread that owns the player's world.
 *
 * <p>A {@link World} is a {@link java.util.concurrent.Executor} backed by its own tick thread, and
 * per-player state (visibility, packet sends) should be touched on that thread. Calls already on the
 * world thread run inline; off-thread calls (commands, async chat events, the periodic sweep) are
 * queued via {@code world.execute(...)}. Falls back to inline if the world can't be resolved.
 */
public final class WorldThread {
    private WorldThread() {
    }

    public static void run(PlayerRef player, Runnable action) {
        if (player == null) {
            return;
        }
        UUID worldId = player.getWorldUuid();
        World world = worldId == null ? null : Universe.get().getWorld(worldId);
        if (world == null || world.isInThread()) {
            action.run();
        } else {
            world.execute(action);
        }
    }
}
