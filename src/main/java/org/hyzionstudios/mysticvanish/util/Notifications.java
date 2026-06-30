package org.hyzionstudios.mysticvanish.util;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

/**
 * Player-directed feedback (toasts + chat) that always lands on the target's world thread via
 * {@link WorldThread}, so it is safe to call from commands, async chat events, or cross-world actions
 * such as {@code /vanish <player>}.
 */
public final class Notifications {
    private Notifications() {
    }

    public static void toast(PlayerRef player, Message message, NotificationStyle style) {
        if (player == null || message == null) {
            return;
        }
        WorldThread.run(player, () -> {
            if (player.isValid()) {
                NotificationUtil.sendNotification(player.getPacketHandler(), message, style);
            }
        });
    }

    public static void send(PlayerRef player, Message message) {
        if (player == null || message == null) {
            return;
        }
        WorldThread.run(player, () -> {
            if (player.isValid()) {
                player.sendMessage(message);
            }
        });
    }
}
