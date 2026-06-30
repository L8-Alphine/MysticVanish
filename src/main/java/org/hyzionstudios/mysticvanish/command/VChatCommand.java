package org.hyzionstudios.mysticvanish.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Map;
import org.hyzionstudios.mysticvanish.config.MessageManager;
import org.hyzionstudios.mysticvanish.manager.VanishManager;
import org.hyzionstudios.mysticvanish.util.TextUtil;

public final class VChatCommand extends CommandBase {
    private final VanishManager vanishManager;
    private final MessageManager messages;
    private final RequiredArg<String> message;

    public VChatCommand(VanishManager vanishManager, MessageManager messages) {
        super("vchat", "Send intentional public chat while vanished.");
        this.vanishManager = vanishManager;
        this.messages = messages;
        message = withRequiredArg("message", "message", ArgTypes.GREEDY_STRING);
        requirePermission("mysticvanish.use");
    }

    @Override
    protected void executeSync(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(messages.message("player_only"));
            return;
        }
        PlayerRef player = Universe.get().getPlayer(context.sender().getUuid());
        if (player == null) {
            context.sendMessage(messages.message("player_only"));
            return;
        }
        String content = context.get(message);
        Universe.get().sendMessage(TextUtil.colorize("&7[" + player.getUsername() + "] &f" + content));
        context.sendMessage(messages.message("vchat_sent", Map.of("message", content)));
        vanishManager.updateVisibility(player.getUuid());
    }
}
