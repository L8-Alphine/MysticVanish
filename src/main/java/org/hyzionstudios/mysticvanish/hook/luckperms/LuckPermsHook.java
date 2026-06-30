package org.hyzionstudios.mysticvanish.hook.luckperms;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.user.User;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.manager.VanishManager;

public final class LuckPermsHook implements AutoCloseable {
    private final ConfigManager configManager;
    private final VanishManager vanishManager;
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();

    public LuckPermsHook(JavaPlugin plugin, ConfigManager configManager, VanishManager vanishManager) {
        this.configManager = configManager;
        this.vanishManager = vanishManager;

        LuckPerms luckPerms = LuckPermsProvider.get();
        subscriptions.add(luckPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate));
        subscriptions.add(luckPerms.getEventBus().subscribe(plugin, NodeMutateEvent.class, this::onNodeMutate));
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        refresh(event.getUser().getUniqueId());
    }

    private void onNodeMutate(NodeMutateEvent event) {
        PermissionHolder target = event.getTarget();
        if (target instanceof User user) {
            refresh(user.getUniqueId());
            return;
        }
        refreshOnlinePlayers();
    }

    private void refreshOnlinePlayers() {
        if (!shouldUpdate()) {
            return;
        }
        for (PlayerRef player : Universe.get().getPlayers()) {
            vanishManager.refreshPermissions(player.getUuid());
        }
    }

    private void refresh(java.util.UUID playerId) {
        if (shouldUpdate()) {
            vanishManager.refreshPermissions(playerId);
        }
    }

    private boolean shouldUpdate() {
        return configManager.getBoolean("settings.update_visibility_on_permission_change", true);
    }

    @Override
    public void close() {
        for (EventSubscription<?> subscription : subscriptions) {
            subscription.close();
        }
        subscriptions.clear();
    }
}
