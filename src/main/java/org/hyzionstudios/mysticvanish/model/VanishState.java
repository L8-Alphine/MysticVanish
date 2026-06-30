package org.hyzionstudios.mysticvanish.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class VanishState {
    private final UUID playerId;
    private final boolean vanished;
    private final int vanishLevel;
    private final String profile;
    private final EnumSet<VanishFeature> features;
    private final Instant updatedAt;

    public VanishState(UUID playerId, boolean vanished, int vanishLevel, String profile, Set<VanishFeature> features) {
        this(playerId, vanished, vanishLevel, profile, features, Instant.now());
    }

    public VanishState(UUID playerId, boolean vanished, int vanishLevel, String profile, Set<VanishFeature> features, Instant updatedAt) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.vanished = vanished;
        this.vanishLevel = Math.max(0, vanishLevel);
        this.profile = profile == null || profile.isBlank() ? "default" : profile;
        this.features = features.isEmpty() ? EnumSet.noneOf(VanishFeature.class) : EnumSet.copyOf(features);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static VanishState visible(UUID playerId) {
        return new VanishState(playerId, false, 0, "default", EnumSet.noneOf(VanishFeature.class));
    }

    public UUID playerId() {
        return playerId;
    }

    public boolean vanished() {
        return vanished;
    }

    public int vanishLevel() {
        return vanishLevel;
    }

    public String profile() {
        return profile;
    }

    public EnumSet<VanishFeature> features() {
        return EnumSet.copyOf(features);
    }

    public boolean hasFeature(VanishFeature feature) {
        return features.contains(feature);
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public VanishState withVanish(boolean vanished, int level, Set<VanishFeature> features) {
        return new VanishState(playerId, vanished, level, profile, features);
    }
}
