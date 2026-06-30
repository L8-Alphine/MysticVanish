package org.hyzionstudios.mysticvanish.permission;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class HytalePermissionSource implements ExactPermissionSource {
    @Override
    public Boolean hasExact(UUID playerId, String permission) {
        PermissionsModule permissions = PermissionsModule.get();
        if (permissions == null || playerId == null || permission == null) {
            return null;
        }
        for (PermissionProvider provider : permissions.getProviders()) {
            Boolean userResult = exactResult(provider.getUserPermissions(playerId), permission);
            if (userResult != null) {
                return userResult;
            }
            for (String group : provider.getGroupsForUser(playerId)) {
                Boolean groupResult = exactGroupResult(permissions, provider, group, permission, new HashSet<>());
                if (groupResult != null) {
                    return groupResult;
                }
            }
        }
        return null;
    }

    @Override
    public int highestLevel(UUID playerId, String prefix, int currentHighest) {
        PermissionsModule permissions = PermissionsModule.get();
        if (permissions == null || playerId == null || prefix == null) {
            return currentHighest;
        }
        int highest = currentHighest;
        for (PermissionProvider provider : permissions.getProviders()) {
            highest = highestLevel(provider.getUserPermissions(playerId), prefix, highest);
            for (String group : provider.getGroupsForUser(playerId)) {
                highest = highestGroupLevel(permissions, provider, group, prefix, highest, new HashSet<>());
            }
        }
        return highest;
    }

    private Boolean exactGroupResult(PermissionsModule permissions, PermissionProvider provider, String group, String permission, Set<String> seen) {
        if (group == null || !seen.add(group)) {
            return null;
        }
        Boolean groupResult = exactResult(provider.getGroupPermissions(group), permission);
        if (groupResult != null) {
            return groupResult;
        }
        Map<String, Set<String>> virtualGroups = permissions.getVirtualGroups();
        if (virtualGroups != null) {
            Boolean virtualResult = exactResult(virtualGroups.get(group), permission);
            if (virtualResult != null) {
                return virtualResult;
            }
        }
        return exactGroupResult(permissions, provider, provider.getGroupParent(group), permission, seen);
    }

    private Boolean exactResult(Set<String> permissions, String permission) {
        if (permissions == null || permissions.isEmpty()) {
            return null;
        }
        try {
            if (permissions.contains("-" + permission)) {
                return Boolean.FALSE;
            }
            if (permissions.contains(permission)) {
                return Boolean.TRUE;
            }
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        return null;
    }

    private int highestGroupLevel(PermissionsModule permissions, PermissionProvider provider, String group, String prefix, int highest, Set<String> seen) {
        if (group == null || !seen.add(group)) {
            return highest;
        }
        int result = highestLevel(provider.getGroupPermissions(group), prefix, highest);
        Map<String, Set<String>> virtualGroups = permissions.getVirtualGroups();
        if (virtualGroups != null) {
            result = highestLevel(virtualGroups.get(group), prefix, result);
        }
        return highestGroupLevel(permissions, provider, provider.getGroupParent(group), prefix, result, seen);
    }

    private int highestLevel(Set<String> permissions, String prefix, int highest) {
        if (permissions == null || permissions.isEmpty()) {
            return highest;
        }
        Set<Integer> denied = new HashSet<>();
        try {
            for (String permission : permissions) {
                if (permission == null || !permission.startsWith("-")) {
                    continue;
                }
                Integer level = ExactPermissionSource.parseLevel(permission.substring(1), prefix);
                if (level != null) {
                    denied.add(level);
                }
            }
            int result = highest;
            for (String permission : permissions) {
                Integer level = ExactPermissionSource.parseLevel(permission, prefix);
                if (level != null && level > result && !denied.contains(level)) {
                    result = level;
                }
            }
            return result;
        } catch (UnsupportedOperationException exception) {
            return highest;
        }
    }
}
