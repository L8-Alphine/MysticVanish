package org.hyzionstudios.mysticvanish;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.hyzionstudios.mysticvanish.api.MysticVanishProvider;
import org.hyzionstudios.mysticvanish.command.VChatCommand;
import org.hyzionstudios.mysticvanish.command.VanishCommand;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.config.MessageManager;
import org.hyzionstudios.mysticvanish.hook.luckperms.LuckPermsHook;
import org.hyzionstudios.mysticvanish.hook.TriggerVolumeHook;
import org.hyzionstudios.mysticvanish.glow.VanishGlowController;
import org.hyzionstudios.mysticvanish.hook.placeholderapi.PlaceholderHook;
import org.hyzionstudios.mysticvanish.hud.VanishHudController;
import org.hyzionstudios.mysticvanish.listener.ChatListener;
import org.hyzionstudios.mysticvanish.listener.CommandListener;
import org.hyzionstudios.mysticvanish.listener.EntityTargetListener;
import org.hyzionstudios.mysticvanish.listener.InteractionListener;
import org.hyzionstudios.mysticvanish.listener.PlayerListener;
import org.hyzionstudios.mysticvanish.manager.VanishAuraManager;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.manager.VisibilityManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;
import org.hyzionstudios.mysticvanish.npc.VanishAttitudeSystem;
import org.hyzionstudios.mysticvanish.permission.PermissionResolver;
import org.hyzionstudios.mysticvanish.storage.StorageManager;
import org.hyzionstudios.mysticvanish.task.VisibilityRefreshTask;

public class MysticVanishPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PermissionResolver permissionResolver;
    private StorageManager storageManager;
    private VanishManager vanishManager;
    private VisibilityManager visibilityManager;
    private VanishAuraManager vanishAuraManager;
    private PlaceholderHook placeholderHook;
    private AutoCloseable luckPermsHook;
    private ScheduledFuture<?> visibilityRefreshHandle;

    public MysticVanishPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        configManager = new ConfigManager(this);
        configManager.load();
        messageManager = new MessageManager(this, configManager);
        messageManager.load();
        permissionResolver = new PermissionResolver(configManager);
        storageManager = new StorageManager(configManager);
        vanishManager = new VanishManager(storageManager.storage(), configManager, permissionResolver);

        // ECS systems must be registered in the setup phase, before world stores are built. The NPC
        // attitude provider powers the no_entity_target feature; only register it when enabled.
        if (configManager.isFeatureEnabled(VanishFeature.NO_ENTITY_TARGET)) {
            getEntityStoreRegistry().registerSystem(new VanishAttitudeSystem(new EntityTargetListener(vanishManager), getLogger()));
        }
    }

    @Override
    protected void start() {
        visibilityManager = new VisibilityManager(configManager, permissionResolver, vanishManager);
        vanishManager.setVisibilityManager(visibilityManager);
        vanishManager.setHudController(new VanishHudController(configManager, visibilityManager));
        vanishManager.setGlowController(new VanishGlowController(configManager, getLogger()));

        MysticVanishProvider.register(vanishManager);
        registerCommands();
        registerListeners();
        scheduleVisibilityRefresh();
        vanishAuraManager = new VanishAuraManager(configManager, vanishManager, visibilityManager, getLogger());
        vanishAuraManager.start();
        registerLuckPermsHook();

        placeholderHook = new PlaceholderHook(this, configManager, vanishManager, visibilityManager, permissionResolver);
        placeholderHook.registerIfAvailable();

        getLogger().at(Level.INFO).log("MysticVanish started.");
    }

    @Override
    protected void shutdown() {
        if (visibilityRefreshHandle != null) {
            visibilityRefreshHandle.cancel(false);
            visibilityRefreshHandle = null;
        }
        if (vanishAuraManager != null) {
            vanishAuraManager.close();
            vanishAuraManager = null;
        }
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
        if (luckPermsHook != null) {
            try {
                luckPermsHook.close();
            } catch (Exception exception) {
                getLogger().at(Level.WARNING).log("Failed to unregister LuckPerms hook.", exception);
            }
            luckPermsHook = null;
        }
        if (visibilityManager != null) {
            visibilityManager.close();
            visibilityManager = null;
        }
        MysticVanishProvider.unregister(vanishManager);
        getLogger().at(Level.INFO).log("MysticVanish shut down.");
    }

    public void reloadMysticVanish() {
        configManager.load();
        messageManager.reload();
        visibilityManager.updateAllVisibility();
        if (vanishAuraManager != null) {
            vanishAuraManager.reload();
        }
    }

    /**
     * Visibility is updated reactively (join, permission change, vanish toggle). This optional periodic
     * sweep is a safety net for desyncs; it is disabled by default and only runs when
     * {@code settings.visibility_refresh_interval_seconds} is greater than zero.
     */
    private void scheduleVisibilityRefresh() {
        int interval = configManager.getInt("settings.visibility_refresh_interval_seconds", 0);
        if (interval <= 0) {
            return;
        }
        visibilityRefreshHandle = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                new VisibilityRefreshTask(visibilityManager), interval, interval, TimeUnit.SECONDS);
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new VanishCommand(this, vanishManager, visibilityManager, permissionResolver, messageManager));
        getCommandRegistry().registerCommand(new VChatCommand(vanishManager, messageManager));
    }

    private void registerListeners() {
        PlayerListener playerListener = new PlayerListener(configManager, vanishManager, visibilityManager);
        playerListener.register(getEventRegistry());

        CommandListener commandListener = new CommandListener(vanishManager);
        ChatListener chatListener = new ChatListener(configManager, messageManager, vanishManager, commandListener);
        chatListener.register(getEventRegistry(), HytaleServer.get().getEventBus());

        InteractionListener interactionListener = new InteractionListener(vanishManager);
        interactionListener.register(getEventRegistry());

        TriggerVolumeHook triggerVolumeHook = new TriggerVolumeHook(vanishManager);
        triggerVolumeHook.register(getEventRegistry(), HytaleServer.get().getEventBus());
    }

    private void registerLuckPermsHook() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            luckPermsHook = new LuckPermsHook(this, configManager, vanishManager);
            getLogger().at(Level.INFO).log("LuckPerms integration enabled.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            luckPermsHook = null;
        }
    }
}
