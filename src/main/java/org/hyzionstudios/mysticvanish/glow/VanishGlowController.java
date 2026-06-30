package org.hyzionstudios.mysticvanish.glow;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.util.WorldThread;

/**
 * Applies/removes a persistent model-highlight effect ("glow") on a vanished player's entity via the
 * engine's {@link EffectControllerComponent}. The effect is part of the entity's replicated state, so it
 * is only seen by viewers who can already see the vanished entity (see-level >= vanish-level) — it scopes
 * itself the same way the rest of vanish does. All ECS access runs on the entity's world thread.
 *
 * <p>The effect "key" passed to add/remove MUST be the effect asset's index in
 * {@code EntityEffect.getAssetMap()} (that is how the engine's own auto-keyed {@code addEffect} derives
 * it). The client uses this int to index its effect table, so an out-of-range value (e.g. a hashCode)
 * crashes every viewing client with an IndexOutOfRangeException.
 */
public final class VanishGlowController {
    /** Preset colour name -> bundled effect asset suffix. Each effect tints the player that colour. */
    private static final Set<String> PRESET_COLORS = Set.of("cyan", "blue", "green", "red", "purple", "gold", "white", "pink");
    private static final String DEFAULT_COLOR = "cyan";

    /** Every glow effect this plugin has ever applied, so cleanup can remove a stale one whatever it was. */
    private static final List<String> GLOW_EFFECT_IDS = List.of(
            "MysticVanishGlow",
            "MysticVanishGlow_Cyan", "MysticVanishGlow_Blue", "MysticVanishGlow_Green", "MysticVanishGlow_Red",
            "MysticVanishGlow_Purple", "MysticVanishGlow_Gold", "MysticVanishGlow_White", "MysticVanishGlow_Pink",
            "Drop_Legendary", "Drop_Epic", "Drop_Rare", "Drop_Uncommon");

    private final ConfigManager configManager;
    private final HytaleLogger logger;
    private final Set<String> warnedEffectIds = ConcurrentHashMap.newKeySet();

    public VanishGlowController(ConfigManager configManager, HytaleLogger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void apply(UUID playerId, boolean vanished) {
        // Removal always runs (cleanup); only application is gated on the toggle.
        if (vanished && !configManager.getBoolean("glow.enabled", true)) {
            return;
        }
        PlayerRef player = Universe.get().getPlayer(playerId);
        if (player == null) {
            return;
        }
        WorldThread.run(player, () -> update(player.getWorldUuid(), playerId, vanished));
    }

    private void update(UUID worldId, UUID playerId, boolean vanished) {
        World world = worldId == null ? null : Universe.get().getWorld(worldId);
        if (world == null) {
            return;
        }
        try {
            Ref<EntityStore> ref = world.getEntityRef(playerId);
            if (ref == null) {
                return;
            }
            Store<EntityStore> store = world.getEntityStore().getStore();
            EffectControllerComponent controller = store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (controller == null) {
                return;
            }
            // Always clear any existing glow first. Infinite effects persist on the entity (and can
            // outlive the in-memory vanish state across relogs/restarts), so this clears stale glows and
            // prevents stacking. We remove every glow variant we might have applied, not just the current.
            clearAllGlows(controller, ref, store);
            if (!vanished) {
                return;
            }
            String effectId = resolveEffectId();
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
            if (effect == null) {
                if (warnedEffectIds.add(effectId)) {
                    logger.at(Level.WARNING).log("Vanish glow effect '" + effectId + "' was not found; no glow will be applied.");
                }
                return;
            }
            // The key is the effect's index in the asset table (the client indexes by it). Using anything
            // else (e.g. a hashCode) crashes viewing clients with IndexOutOfRangeException.
            controller.addInfiniteEffect(ref, EntityEffect.getAssetMap().getIndex(effectId), effect, store);
        } catch (RuntimeException exception) {
            logger.at(Level.WARNING).log("Failed to update vanish glow for " + playerId + ".", exception);
        }
    }

    /** Removes every glow effect this plugin can apply, keyed by valid asset index, so none can linger. */
    private void clearAllGlows(EffectControllerComponent controller, Ref<EntityStore> ref, Store<EntityStore> store) {
        for (String id : GLOW_EFFECT_IDS) {
            if (EntityEffect.getAssetMap().getAsset(id) != null) {
                controller.removeEffect(ref, EntityEffect.getAssetMap().getIndex(id), store);
            }
        }
    }

    /** Maps the {@code glow.color} preset (cyan/blue/.../pink) to its bundled effect asset id. */
    private String resolveEffectId() {
        String color = configManager.getString("glow.color", DEFAULT_COLOR).trim().toLowerCase(Locale.ROOT);
        if (!PRESET_COLORS.contains(color)) {
            if (warnedEffectIds.add("color:" + color)) {
                logger.at(Level.WARNING).log("Unknown glow.color '" + color + "'; using " + DEFAULT_COLOR
                        + ". Valid colours: " + PRESET_COLORS);
            }
            color = DEFAULT_COLOR;
        }
        return "MysticVanishGlow_" + Character.toUpperCase(color.charAt(0)) + color.substring(1);
    }
}
