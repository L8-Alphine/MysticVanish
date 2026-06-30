package org.hyzionstudios.mysticvanish.storage;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.hyzionstudios.mysticvanish.model.VanishState;

public interface VanishStorage {
    Optional<VanishState> load(UUID playerId);

    void save(VanishState state);

    void delete(UUID playerId);

    void saveAll(Collection<VanishState> states);
}
