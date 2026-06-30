package org.hyzionstudios.mysticvanish.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.UUID;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.manager.VisibilityManager;
import org.hyzionstudios.mysticvanish.util.WorldThread;

/**
 * Adds and removes the vanished-state HUD via {@link HudManager}. HUD mutation writes packets to the
 * player, so every path runs on the player's world thread through {@link WorldThread#run}.
 */
public final class VanishHudController {
    private final ConfigManager configManager;
    private final VisibilityManager visibilityManager;

    public VanishHudController(ConfigManager configManager, VisibilityManager visibilityManager) {
        this.configManager = configManager;
        this.visibilityManager = visibilityManager;
    }

    public void show(UUID playerId, int level) {
        if (!configManager.getBoolean("hud.enabled", true)) {
            return;
        }
        PlayerRef player = Universe.get().getPlayer(playerId);
        if (player == null) {
            return;
        }
        WorldThread.run(player, () -> {
            HudManager hud = hudManager(player);
            if (hud == null) {
                return;
            }
            String hudId = configManager.getString("hud.hud_id", "mysticvanish:vanish");
            int zOrder = configManager.getInt("hud.z_order", 0);
            int seeLevel = visibilityManager.getSeeLevel(playerId);
            hud.addCustomHud(player, new VanishHud(player, hudId, zOrder, configManager, level, seeLevel));
        });
    }

    public void hide(UUID playerId) {
        PlayerRef player = Universe.get().getPlayer(playerId);
        if (player == null) {
            return;
        }
        WorldThread.run(player, () -> {
            HudManager hud = hudManager(player);
            if (hud != null) {
                hud.removeCustomHud(player, configManager.getString("hud.hud_id", "mysticvanish:vanish"));
            }
        });
    }

    private HudManager hudManager(PlayerRef player) {
        if (!player.isValid()) {
            return null;
        }
        Player entity = player.getComponent(Player.getComponentType());
        return entity == null ? null : entity.getHudManager();
    }
}
