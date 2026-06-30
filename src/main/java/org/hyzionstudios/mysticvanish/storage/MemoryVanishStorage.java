package org.hyzionstudios.mysticvanish.storage;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hyzionstudios.mysticvanish.model.VanishState;

public final class MemoryVanishStorage implements VanishStorage {
    private final Map<UUID, VanishState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<VanishState> load(UUID playerId) {
        return Optional.ofNullable(states.get(playerId));
    }

    @Override
    public void save(VanishState state) {
        states.put(state.playerId(), state);
    }

    @Override
    public void delete(UUID playerId) {
        states.remove(playerId);
    }

    @Override
    public void saveAll(Collection<VanishState> states) {
        for (VanishState state : states) {
            save(state);
        }
    }

    public Collection<VanishState> allStates() {
        return states.values();
    }
}
