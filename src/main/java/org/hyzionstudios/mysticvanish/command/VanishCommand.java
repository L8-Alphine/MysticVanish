package org.hyzionstudios.mysticvanish.command;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Map;
import org.hyzionstudios.mysticvanish.MysticVanishPlugin;
import org.hyzionstudios.mysticvanish.config.MessageManager;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.manager.VisibilityManager;
import org.hyzionstudios.mysticvanish.model.VanishState;
import org.hyzionstudios.mysticvanish.permission.PermissionResolver;
import org.hyzionstudios.mysticvanish.util.Notifications;

public final class VanishCommand extends CommandBase {
    private final MysticVanishPlugin plugin;
    private final VanishManager vanishManager;
    private final VisibilityManager visibilityManager;
    private final PermissionResolver permissionResolver;
    private final MessageManager messages;
    private final OptionalArg<String> args;

    public VanishCommand(MysticVanishPlugin plugin, VanishManager vanishManager, VisibilityManager visibilityManager, PermissionResolver permissionResolver, MessageManager messages) {
        super("vanish", "Layered staff vanish controls.");
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.visibilityManager = visibilityManager;
        this.permissionResolver = permissionResolver;
        this.messages = messages;
        args = withOptionalArg("args", "subcommand or target", ArgTypes.GREEDY_STRING);
        addAliases("v", "mysticvanish", "mvanish");
        addSubCommand(new StatusSubCommand());
        // Gate the command on our own node. Without this the framework auto-generates a permission
        // from the command name that players won't hold, so granting mysticvanish.use wouldn't work.
        // Finer actions (toggle others, reload, levels) are still checked individually inside.
        requirePermission(PermissionResolver.USE);
    }

    @Override
    protected void executeSync(CommandContext context) {
        CommandSender sender = context.sender();
        String input = context.provided(args) ? context.get(args).trim() : "";
        if (input.isBlank()) {
            toggleSelf(context, sender);
            return;
        }

        String[] parts = input.split("\\s+");
        if ("reload".equalsIgnoreCase(parts[0])) {
            reload(context, sender);
            return;
        }
        if ("status".equalsIgnoreCase(parts[0])) {
            status(context, sender, parts);
            return;
        }
        if ("off".equalsIgnoreCase(parts[0])) {
            unvanishSelf(context, sender);
            return;
        }
        // Vanish level is permission-defined; a bare number is no longer a valid argument.
        if (parseLevel(parts[0]) != null) {
            context.sendMessage(messages.message("vanish_level_from_permission"));
            return;
        }

        PlayerRef target = Universe.get().getPlayer(parts[0], NameMatching.DEFAULT);
        if (target == null) {
            context.sendMessage(messages.message("player_not_found"));
            return;
        }
        // Reject the old "/vanish <player> <level>" form so level can only come from the target's permission.
        if (parts.length > 1 && parseLevel(parts[1]) != null) {
            context.sendMessage(messages.message("vanish_level_from_permission"));
            return;
        }
        toggleOther(context, sender, target);
    }

    private void toggleSelf(CommandContext context, CommandSender sender) {
        if (!permissionResolver.hasUse(sender)) {
            context.sendMessage(messages.message("no_permission"));
            return;
        }
        PlayerRef player = requirePlayer(context);
        if (player == null) {
            return;
        }
        boolean wasVanished = vanishManager.isVanished(player.getUuid());
        if (!wasVanished && permissionResolver.resolveVanishLevel(player) < 1) {
            context.sendMessage(messages.message("no_vanish_permission"));
            return;
        }
        vanishManager.toggleVanish(player);
        VanishState state = vanishManager.getState(player.getUuid());
        sendStateMessage(context, state, !wasVanished);
        if (!wasVanished) {
            Notifications.toast(player, messages.message("vanish_enabled", Map.of("level", String.valueOf(state.vanishLevel()))), NotificationStyle.Success);
        } else {
            Notifications.toast(player, messages.message("vanish_disabled"), NotificationStyle.Default);
        }
    }

    private void unvanishSelf(CommandContext context, CommandSender sender) {
        if (!permissionResolver.hasUse(sender)) {
            context.sendMessage(messages.message("no_permission"));
            return;
        }
        PlayerRef player = requirePlayer(context);
        if (player == null) {
            return;
        }
        vanishManager.unvanish(player.getUuid());
        context.sendMessage(messages.message("vanish_disabled"));
        Notifications.toast(player, messages.message("vanish_disabled"), NotificationStyle.Default);
    }

    private void status(CommandContext context, CommandSender sender, String[] parts) {
        if (parts.length == 1) {
            PlayerRef player = requirePlayer(context);
            if (player != null) {
                sendStatus(context, player);
            }
            return;
        }
        if (!permissionResolver.canStatusOthers(sender)) {
            context.sendMessage(messages.message("no_permission"));
            return;
        }
        PlayerRef target = Universe.get().getPlayer(parts[1], NameMatching.DEFAULT);
        if (target == null) {
            context.sendMessage(messages.message("player_not_found"));
            return;
        }
        vanishManager.refreshPermissions(target.getUuid());
        VanishState state = vanishManager.getState(target.getUuid());
        int level = state.vanished() ? state.vanishLevel() : permissionResolver.resolveVanishLevel(target);
        context.sendMessage(messages.message("target_status", Map.of(
                "player", target.getUsername(),
                "status", state.vanished() ? "vanished" : "visible",
                "level", String.valueOf(level),
                "profile", state.profile()
        )));
    }

    private void reload(CommandContext context, CommandSender sender) {
        if (!permissionResolver.canReload(sender)) {
            context.sendMessage(messages.message("no_permission"));
            return;
        }
        plugin.reloadMysticVanish();
        context.sendMessage(messages.message("reload_complete"));
    }

    private void toggleOther(CommandContext context, CommandSender sender, PlayerRef target) {
        if (!permissionResolver.canToggleOthers(sender)) {
            context.sendMessage(messages.message("no_permission"));
            return;
        }
        boolean wasVanished = vanishManager.isVanished(target.getUuid());
        if (!wasVanished && permissionResolver.resolveVanishLevel(target) < 1) {
            context.sendMessage(messages.message("target_no_vanish_permission", Map.of("player", target.getUsername())));
            return;
        }
        vanishManager.toggleVanish(target);
        int level = vanishManager.getVanishLevel(target.getUuid());
        context.sendMessage(messages.message(wasVanished ? "target_vanish_disabled" : "target_vanish_enabled",
                Map.of("player", target.getUsername(), "level", String.valueOf(level))));
        notifyTarget(target, !wasVanished, level);
    }

    /** Tells the affected player about a staff-triggered vanish change, on their own world thread. */
    private void notifyTarget(PlayerRef target, boolean enabled, int level) {
        if (enabled) {
            Message message = messages.message("vanish_enabled", Map.of("level", String.valueOf(level)));
            Notifications.send(target, message);
            Notifications.toast(target, message, NotificationStyle.Success);
        } else {
            Message message = messages.message("vanish_disabled");
            Notifications.send(target, message);
            Notifications.toast(target, message, NotificationStyle.Default);
        }
    }

    private void sendStatus(CommandContext context, PlayerRef player) {
        vanishManager.refreshPermissions(player.getUuid());
        VanishState state = vanishManager.getState(player.getUuid());
        int level = state.vanished() ? state.vanishLevel() : permissionResolver.resolveVanishLevel(player);
        context.sendMessage(messages.message("status", Map.of(
                "status", state.vanished() ? "vanished" : "visible",
                "level", String.valueOf(level),
                "see_level", String.valueOf(visibilityManager.getSeeLevel(player.getUuid())),
                "profile", state.profile()
        )));
    }

    private void sendStateMessage(CommandContext context, VanishState state, boolean enabled) {
        if (enabled) {
            context.sendMessage(messages.message("vanish_enabled", Map.of("level", String.valueOf(state.vanishLevel()))));
        } else {
            context.sendMessage(messages.message("vanish_disabled"));
        }
    }

    private PlayerRef requirePlayer(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(messages.message("player_only"));
            return null;
        }
        return Universe.get().getPlayer(context.sender().getUuid());
    }

    private static Integer parseLevel(String value) {
        try {
            int level = Integer.parseInt(value);
            return level < 0 ? null : level;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private final class StatusSubCommand extends CommandBase {
        private StatusSubCommand() {
            super("status", "Shows your MysticVanish status.");
            requirePermission(PermissionResolver.USE);
        }

        @Override
        protected void executeSync(CommandContext context) {
            status(context, context.sender(), new String[] { "status" });
        }
    }
}
