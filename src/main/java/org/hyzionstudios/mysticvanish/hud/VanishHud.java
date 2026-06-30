package org.hyzionstudios.mysticvanish.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.util.TextUtil;

/**
 * Server-driven custom HUD shown to a player while they are vanished.
 *
 * <p>The visual layout lives in a client-side UI asset registered under {@code hud.hud_id}; this class
 * only pushes content into it. {@link #build(UICommandBuilder)} sets the text of the element named by
 * {@code hud.text_selector}, so the bundled config and the shipped client asset must agree on those ids.
 */
final class VanishHud extends CustomUIHud {
    private final ConfigManager configManager;
    private final int level;
    private final int seeLevel;

    VanishHud(PlayerRef player, String hudId, int zOrder, ConfigManager configManager, int level, int seeLevel) {
        super(player, hudId, zOrder);
        this.configManager = configManager;
        this.level = level;
        this.seeLevel = seeLevel;
    }

    @Override
    protected void build(UICommandBuilder builder) {
        // Instantiate the client-side .ui layout, then populate its named text element.
        String uiFile = configManager.getString("hud.ui_file", "").trim();
        if (uiFile.isEmpty()) {
            return;
        }
        builder.append(uiFile);
        String selector = configManager.getString("hud.text_selector", "#Status.Text");
        // The HUD label takes a raw String (colour comes from the .ui Style); a component Message is
        // rejected by the client's Set handler and disconnects it. Strip any &-codes to plain text.
        String text = TextUtil.stripCodes(configManager.getString("hud.text", "VANISHED  Level {level}")
                .replace("{level}", String.valueOf(level))
                .replace("{see}", String.valueOf(seeLevel)));
        builder.set(selector, text);
    }
}
