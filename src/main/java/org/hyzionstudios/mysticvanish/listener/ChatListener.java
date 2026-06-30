package org.hyzionstudios.mysticvanish.listener;

import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.List;
import org.hyzionstudios.mysticvanish.config.ConfigManager;
import org.hyzionstudios.mysticvanish.config.MessageManager;
import org.hyzionstudios.mysticvanish.event.VanishChatBlockedEvent;
import org.hyzionstudios.mysticvanish.event.VanishCommandBlockedEvent;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.model.VanishFeature;
import org.hyzionstudios.mysticvanish.util.Notifications;
import org.hyzionstudios.mysticvanish.util.TextUtil;

/**
 * Intercepts {@link PlayerChatEvent}. This is the only text-input event exposed in Server 0.5.6, so
 * it serves double duty: chat-mistype protection for normal messages, and best-effort command-mistype
 * protection for anything the client routes through chat as a {@code /command}. If a future build adds
 * a dedicated command-preprocess event, {@link #handleCommand} should move there.
 */
public final class ChatListener {
    private final ConfigManager configManager;
    private final MessageManager messages;
    private final VanishManager vanishManager;
    private final CommandListener commandListener;
    private EventBus eventBus;

    public ChatListener(ConfigManager configManager, MessageManager messages, VanishManager vanishManager, CommandListener commandListener) {
        this.configManager = configManager;
        this.messages = messages;
        this.vanishManager = vanishManager;
        this.commandListener = commandListener;
    }

    public void register(EventRegistry registry, EventBus eventBus) {
        this.eventBus = eventBus;
        registry.registerAsyncGlobal(PlayerChatEvent.class, future -> future.thenApply(this::onChat));
    }

    private PlayerChatEvent onChat(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();
        if (sender == null) {
            return event;
        }
        String content = event.getContent() == null ? "" : event.getContent();
        if (TextUtil.normalizeCommand(content).isEmpty()) {
            return event;
        }
        if (content.stripLeading().startsWith("/")) {
            handleCommand(event, sender, content);
            return event;
        }
        handleChat(event, sender, content);
        return event;
    }

    private void handleChat(PlayerChatEvent event, PlayerRef sender, String content) {
        if (!vanishManager.hasFeature(sender.getUuid(), VanishFeature.CHAT_PROTECTION)) {
            return;
        }
        String mode = configManager.getFeatureMode(VanishFeature.CHAT_PROTECTION, "block");
        event.setCancelled(true);
        if ("redirect_to_staff_chat".equalsIgnoreCase(mode)) {
            Universe.get().sendMessage(TextUtil.colorize("&8[Staff] &7" + sender.getUsername() + ": &f" + content));
        } else {
            Notifications.send(sender, messages.message("chat_blocked"));
            Notifications.toast(sender, messages.message("chat_blocked"), NotificationStyle.Warning);
        }
        dispatchChatBlocked(sender, content, mode);
    }

    private void handleCommand(PlayerChatEvent event, PlayerRef sender, String content) {
        List<String> blocked = configManager.getStringList("features.command_protection.blocked_commands");
        if (!commandListener.shouldBlockCommand(sender.getUuid(), content, blocked)) {
            return;
        }
        event.setCancelled(true);
        Notifications.send(sender, messages.message("command_blocked"));
        Notifications.toast(sender, messages.message("command_blocked"), NotificationStyle.Warning);
        dispatchCommandBlocked(sender, content);
    }

    private void dispatchChatBlocked(PlayerRef sender, String content, String mode) {
        if (eventBus != null) {
            eventBus.dispatchFor(VanishChatBlockedEvent.class, sender.getUuid()).dispatch(new VanishChatBlockedEvent(sender.getUuid(), content, mode));
        }
    }

    private void dispatchCommandBlocked(PlayerRef sender, String content) {
        if (eventBus != null) {
            eventBus.dispatchFor(VanishCommandBlockedEvent.class, sender.getUuid()).dispatch(new VanishCommandBlockedEvent(sender.getUuid(), TextUtil.normalizeCommand(content)));
        }
    }
}
