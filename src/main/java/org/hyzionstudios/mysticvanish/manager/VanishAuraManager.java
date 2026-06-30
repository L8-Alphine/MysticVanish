package org.hyzionstudios.mysticvanish.manager;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.model.VanishState;
import org.hyzionstudios.mysticvanish.util.WorldThread;
import org.joml.Vector3d;

public final class VanishAuraManager implements AutoCloseable {
    private static final long MILLIS_PER_TICK = 50L;
    private static final Direction ZERO_ROTATION = new Direction(0.0F, 0.0F, 0.0F);

    private final ConfigManager configManager;
    private final VanishManager vanishManager;
    private final VisibilityManager visibilityManager;
    private final HytaleLogger logger;
    private final Set<String> warnedParticleIds = new HashSet<>();
    private final Set<String> warnedColors = new HashSet<>();
    private ScheduledFuture<?> handle;

    public VanishAuraManager(ConfigManager configManager, VanishManager vanishManager, VisibilityManager visibilityManager, HytaleLogger logger) {
        this.configManager = configManager;
        this.vanishManager = vanishManager;
        this.visibilityManager = visibilityManager;
        this.logger = logger;
    }

    public void start() {
        if (handle != null || !configManager.getBoolean("aura.enabled", true)) {
            return;
        }
        long intervalMillis = Math.max(1, configManager.getInt("aura.interval_ticks", 10)) * MILLIS_PER_TICK;
        handle = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::tickSafely, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void reload() {
        close();
        warnedParticleIds.clear();
        warnedColors.clear();
        start();
    }

    private void tickSafely() {
        try {
            tick();
        } catch (RuntimeException exception) {
            logger.at(Level.WARNING).log("Failed to render vanish aura particles.", exception);
        }
    }

    private void tick() {
        if (!configManager.getBoolean("aura.enabled", true)) {
            return;
        }
        for (PlayerRef viewer : onlinePlayers()) {
            WorldThread.run(viewer, () -> renderForViewer(viewer));
        }
    }

    private void renderForViewer(PlayerRef viewer) {
        if (viewer == null || !viewer.isValid() || !isClientReady(viewer)) {
            return;
        }
        UUID viewerId = viewer.getUuid();
        int viewerSeeLevel = visibilityManager.getSeeLevel(viewerId);
        for (PlayerRef target : onlinePlayers()) {
            if (!shouldRender(viewer, target)) {
                continue;
            }
            boolean mutual = visibilityManager.canSee(target.getUuid(), viewerId);
            AuraStyle style = style(mutual);
            if (style == null) {
                continue;
            }
            int gap = Math.max(0, viewerSeeLevel - vanishManager.getState(target.getUuid()).vanishLevel());
            spawnAura(viewer, target, scaleByGap(style, gap));
        }
    }

    /**
     * Scales aura size and colour brightness by the level gap (viewer see-level minus target vanish-level).
     * A larger gap renders a bigger, brighter aura; a near-zero gap renders a small, dim one.
     */
    private AuraStyle scaleByGap(AuraStyle style, int gap) {
        if (!configManager.getBoolean("aura.scaling.enabled", true)) {
            return style;
        }
        double maxGap = Math.max(1.0D, configManager.getDouble("aura.scaling.max_gap", 40.0D));
        double t = Math.max(0.0D, Math.min(1.0D, gap / maxGap));
        double scaleFactor = lerp(configManager.getDouble("aura.scaling.min_scale_factor", 0.6D),
                configManager.getDouble("aura.scaling.max_scale_factor", 1.6D), t);
        double brightness = lerp(clamp01(configManager.getDouble("aura.scaling.min_brightness", 0.5D)),
                clamp01(configManager.getDouble("aura.scaling.max_brightness", 1.0D)), t);
        float scale = (float) Math.max(0.01D, style.scale() * scaleFactor);
        return new AuraStyle(style.particleId(), scaleBrightness(style.color(), brightness), scale, style.durationSeconds(), style.yOffset());
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static Color scaleBrightness(Color color, double brightness) {
        return new Color(scaleChannel(color.red, brightness), scaleChannel(color.green, brightness), scaleChannel(color.blue, brightness));
    }

    private static byte scaleChannel(byte channel, double brightness) {
        int scaled = (int) Math.round((channel & 0xFF) * brightness);
        return (byte) Math.max(0, Math.min(255, scaled));
    }

    private boolean shouldRender(PlayerRef viewer, PlayerRef target) {
        if (target == null || !target.isValid() || viewer.getUuid().equals(target.getUuid())) {
            return false;
        }
        UUID viewerWorldId = viewer.getWorldUuid();
        UUID targetWorldId = target.getWorldUuid();
        if (viewerWorldId == null || !viewerWorldId.equals(targetWorldId)) {
            return false;
        }
        VanishState targetState = vanishManager.getState(target.getUuid());
        if (!targetState.vanished() || !visibilityManager.canSee(viewer.getUuid(), target.getUuid())) {
            return false;
        }
        return withinDistance(viewer, target);
    }

    private boolean withinDistance(PlayerRef viewer, PlayerRef target) {
        double maxDistance = Math.max(0.0D, configManager.getDouble("aura.max_distance", 64.0D));
        if (maxDistance <= 0.0D) {
            return true;
        }
        Vector3d viewerPosition = viewer.getTransform().getPosition();
        Vector3d targetPosition = target.getTransform().getPosition();
        return viewerPosition.distanceSquared(targetPosition) <= maxDistance * maxDistance;
    }

    private AuraStyle style(boolean mutual) {
        String prefix = mutual ? "aura.mutual" : "aura.one_way";
        String particleId = configManager.getString(prefix + "_particle_system_id", "Drop_Uncommon").trim();
        Color color = parseColor(configManager.getString(prefix + "_color", mutual ? "#55FFFF" : "#FF5555"));
        if (particleId.isBlank() || color == null) {
            return null;
        }
        warnIfUnknownParticleSystem(particleId);
        float scale = (float) Math.max(0.01D, configManager.getDouble("aura.scale", 1.0D));
        float duration = (float) Math.max(0.05D, configManager.getDouble("aura.duration_seconds", 0.75D));
        double yOffset = configManager.getDouble("aura.y_offset", 1.0D);
        return new AuraStyle(particleId, color, scale, duration, yOffset);
    }

    /**
     * Logs once if a particle id isn't in the server's asset map, but never blocks the send: the client
     * has its own particle assets, and a server-side map miss (or unkeyed lookup) shouldn't kill the aura.
     */
    private void warnIfUnknownParticleSystem(String particleId) {
        try {
            if (ParticleSystem.getAssetMap().getAsset(particleId) != null) {
                return;
            }
        } catch (RuntimeException exception) {
            return;
        }
        if (warnedParticleIds.add(particleId)) {
            logger.at(Level.WARNING).log("Vanish aura particle system '" + particleId
                    + "' was not found in the server asset map; sending anyway (the client may still render it).");
        }
    }

    private Color parseColor(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            warnColor(value);
            return null;
        }
        try {
            int rgb = Integer.parseInt(normalized, 16);
            return new Color((byte) ((rgb >> 16) & 0xFF), (byte) ((rgb >> 8) & 0xFF), (byte) (rgb & 0xFF));
        } catch (NumberFormatException exception) {
            warnColor(value);
            return null;
        }
    }

    private void warnColor(String value) {
        String key = String.valueOf(value);
        if (warnedColors.add(key)) {
            logger.at(Level.WARNING).log("Invalid vanish aura color '" + key + "'. Use #RRGGBB format.");
        }
    }

    private void spawnAura(PlayerRef viewer, PlayerRef target, AuraStyle style) {
        Vector3d position = target.getTransform().getPosition();
        // writeNoCache: matches the engine's ParticleUtil. The cached write() path is for identical
        // broadcast packets; per-viewer particles (own position/colour/scale) must not be cached.
        viewer.getPacketHandler().writeNoCache(new SpawnParticleSystem(
                style.particleId(),
                new Position(position.x, position.y + style.yOffset(), position.z),
                ZERO_ROTATION,
                style.scale(),
                style.color(),
                style.durationSeconds()));
    }

    private boolean isClientReady(PlayerRef player) {
        CompletableFuture<Void> readyForChunks = player.getPacketHandler().getClientReadyForChunksFuture();
        return readyForChunks != null && readyForChunks.isDone();
    }

    private ArrayList<PlayerRef> onlinePlayers() {
        return new ArrayList<>(Universe.get().getPlayers());
    }

    @Override
    public void close() {
        if (handle != null) {
            handle.cancel(false);
            handle = null;
        }
    }

    private record AuraStyle(String particleId, Color color, float scale, float durationSeconds, double yOffset) {
    }
}
