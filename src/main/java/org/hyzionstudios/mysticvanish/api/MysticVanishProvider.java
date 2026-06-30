package org.hyzionstudios.mysticvanish.api;

import java.util.Objects;

public final class MysticVanishProvider {
    private static MysticVanishAPI api;

    private MysticVanishProvider() {
    }

    public static MysticVanishAPI get() {
        MysticVanishAPI current = api;
        if (current == null) {
            throw new IllegalStateException("MysticVanish API is not registered yet.");
        }
        return current;
    }

    public static boolean isRegistered() {
        return api != null;
    }

    public static void register(MysticVanishAPI api) {
        MysticVanishProvider.api = Objects.requireNonNull(api, "api");
    }

    public static void unregister(MysticVanishAPI api) {
        if (MysticVanishProvider.api == api) {
            MysticVanishProvider.api = null;
        }
    }
}
