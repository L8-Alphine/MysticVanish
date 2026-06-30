package org.hyzionstudios.mysticvanish.permission;

import java.util.UUID;

interface ExactPermissionSource {
    Boolean hasExact(UUID playerId, String permission);

    default int highestLevel(UUID playerId, String prefix, int currentHighest) {
        return currentHighest;
    }

    static Integer parseLevel(String permission, String prefix) {
        if (permission == null || prefix == null || permission.length() <= prefix.length()) {
            return null;
        }
        if (!permission.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        String value = permission.substring(prefix.length());
        if (value.isBlank()) {
            return null;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return null;
            }
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
