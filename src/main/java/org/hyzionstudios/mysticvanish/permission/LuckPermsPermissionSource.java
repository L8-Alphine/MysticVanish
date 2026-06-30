package org.hyzionstudios.mysticvanish.permission;

import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

final class LuckPermsPermissionSource implements ExactPermissionSource {
    @Override
    public Boolean hasExact(UUID playerId, String permission) {
        if (playerId == null || permission == null) {
            return null;
        }
        User user = user(playerId);
        if (user == null) {
            return null;
        }
        boolean granted = false;
        for (Node node : user.resolveInheritedNodes(user.getQueryOptions())) {
            if (!NodeType.PERMISSION.matches(node)) {
                continue;
            }
            PermissionNode permissionNode = NodeType.PERMISSION.cast(node);
            if (permissionNode.hasExpired() || permissionNode.isWildcard()) {
                continue;
            }
            if (!permission.equalsIgnoreCase(permissionNode.getPermission())) {
                continue;
            }
            if (!permissionNode.getValue()) {
                return Boolean.FALSE;
            }
            granted = true;
        }
        return granted ? Boolean.TRUE : null;
    }

    @Override
    public int highestLevel(UUID playerId, String prefix, int currentHighest) {
        if (playerId == null || prefix == null) {
            return currentHighest;
        }
        User user = user(playerId);
        if (user == null) {
            return currentHighest;
        }
        int highest = currentHighest;
        for (Node node : user.resolveDistinctInheritedNodes(user.getQueryOptions())) {
            if (!NodeType.PERMISSION.matches(node)) {
                continue;
            }
            PermissionNode permissionNode = NodeType.PERMISSION.cast(node);
            if (permissionNode.hasExpired() || permissionNode.isWildcard() || !permissionNode.getValue()) {
                continue;
            }
            Integer level = ExactPermissionSource.parseLevel(permissionNode.getPermission(), prefix);
            if (level != null && level > highest && hasExact(playerId, prefix + level) == Boolean.TRUE) {
                highest = level;
            }
        }
        return highest;
    }

    private User user(UUID playerId) {
        LuckPerms luckPerms = luckPerms();
        return luckPerms == null ? null : luckPerms.getUserManager().getUser(playerId);
    }

    private LuckPerms luckPerms() {
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException exception) {
            return null;
        }
    }
}
