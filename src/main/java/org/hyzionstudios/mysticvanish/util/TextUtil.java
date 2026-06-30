package org.hyzionstudios.mysticvanish.util;

import com.hypixel.hytale.server.core.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Text helpers for MysticVanish.
 *
 * <p>The Hytale {@link Message} API is component based: {@link Message#parse(String)} expects a JSON
 * document, not a legacy formatting string, and {@link Message#color(String)} takes a {@code #RRGGBB}
 * hex value. {@link #colorize(String)} bridges the familiar {@code &}-style codes used in our config
 * files into a properly built {@link Message} so authors can keep writing {@code &8[&bMysticVanish&8]}
 * style text. Use this everywhere instead of {@code Message.parse}.
 */
public final class TextUtil {
    private static final char CODE = '&';
    private static final char SECTION_CODE = '§';

    private TextUtil() {
    }

    public static String normalizeCommand(String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        return command.startsWith("/") ? command.substring(1) : command;
    }

    /**
     * Parses legacy {@code &} formatting codes into a Hytale {@link Message}.
     *
     * <p>Supported: colors {@code &0}-{@code &9}, {@code &a}-{@code &f}; hex colors {@code &#RRGGBB}
     * and {@code &x&R&R&G&G&B&B}; {@code &l} bold, {@code &o} italic, {@code &m} monospace;
     * {@code &r} reset. Section-sign variants are accepted too. A color or hex code resets active
     * styles (vanilla behaviour) so authors get predictable results.
     */
    public static Message colorize(String input) {
        if (input == null || input.isEmpty()) {
            return Message.empty();
        }

        List<Message> segments = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String color = null;
        boolean bold = false;
        boolean italic = false;
        boolean monospace = false;

        int i = 0;
        int length = input.length();
        while (i < length) {
            char current = input.charAt(i);
            if (!isFormatMarker(current) || i + 1 >= length) {
                buffer.append(current);
                i++;
                continue;
            }

            char code = Character.toLowerCase(input.charAt(i + 1));

            // Hex color: &#RRGGBB
            if (code == '#' && i + 8 <= length && isHex(input, i + 2, 6)) {
                flush(segments, buffer, color, bold, italic, monospace);
                color = "#" + input.substring(i + 2, i + 8).toUpperCase(java.util.Locale.ROOT);
                bold = italic = monospace = false;
                i += 8;
                continue;
            }

            // Expanded hex color: &x&R&R&G&G&B&B or §x§R§R§G§G§B§B
            if (code == 'x' && i + 14 <= length && isExpandedHex(input, i, current)) {
                flush(segments, buffer, color, bold, italic, monospace);
                color = "#" + unpackExpandedHex(input, i).toUpperCase(java.util.Locale.ROOT);
                bold = italic = monospace = false;
                i += 14;
                continue;
            }

            String legacyColor = legacyHex(code);
            if (legacyColor != null) {
                flush(segments, buffer, color, bold, italic, monospace);
                color = legacyColor;
                bold = italic = monospace = false;
                i += 2;
                continue;
            }

            switch (code) {
                case 'l' -> {
                    flush(segments, buffer, color, bold, italic, monospace);
                    bold = true;
                    i += 2;
                }
                case 'o' -> {
                    flush(segments, buffer, color, bold, italic, monospace);
                    italic = true;
                    i += 2;
                }
                case 'm' -> {
                    flush(segments, buffer, color, bold, italic, monospace);
                    monospace = true;
                    i += 2;
                }
                case 'r' -> {
                    flush(segments, buffer, color, bold, italic, monospace);
                    color = null;
                    bold = italic = monospace = false;
                    i += 2;
                }
                default -> {
                    // Unknown code: keep the characters literally so no text is silently lost.
                    buffer.append(current);
                    i++;
                }
            }
        }

        flush(segments, buffer, color, bold, italic, monospace);
        return segments.isEmpty() ? Message.empty() : Message.empty().insertAll(segments);
    }

    /**
     * Removes {@code &}/{@code §} formatting codes, returning plain text. Used for UI surfaces (custom
     * HUD labels) that take a raw {@code String} value and colour it via their own {@code Style}, where a
     * component {@link Message} would be rejected.
     */
    public static String stripCodes(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String result = input.replaceAll("(?i)[&§]#[0-9a-f]{6}", "");
        return result.replaceAll("(?i)[&§][0-9a-fk-or]", "");
    }

    /** Builds a single coloured plain-text message (no code parsing). */
    public static Message plain(String text, String hexColor) {
        Message message = Message.raw(text == null ? "" : text);
        return hexColor == null ? message : message.color(hexColor);
    }

    private static void flush(List<Message> segments, StringBuilder buffer, String color, boolean bold, boolean italic, boolean monospace) {
        if (buffer.length() == 0) {
            return;
        }
        Message segment = Message.raw(buffer.toString());
        if (color != null) {
            segment = segment.color(color);
        }
        if (bold) {
            segment = segment.bold(true);
        }
        if (italic) {
            segment = segment.italic(true);
        }
        if (monospace) {
            segment = segment.monospace(true);
        }
        segments.add(segment);
        buffer.setLength(0);
    }

    private static boolean isHex(String input, int start, int count) {
        for (int index = start; index < start + count; index++) {
            char c = input.charAt(index);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFormatMarker(char c) {
        return c == CODE || c == SECTION_CODE;
    }

    private static boolean isExpandedHex(String input, int start, char marker) {
        for (int index = start + 2; index < start + 14; index += 2) {
            if (input.charAt(index) != marker) {
                return false;
            }
            if (!isHex(input, index + 1, 1)) {
                return false;
            }
        }
        return true;
    }

    private static String unpackExpandedHex(String input, int start) {
        StringBuilder hex = new StringBuilder(6);
        for (int index = start + 3; index < start + 14; index += 2) {
            hex.append(input.charAt(index));
        }
        return hex.toString();
    }

    private static String legacyHex(char code) {
        return switch (code) {
            case '0' -> "#000000";
            case '1' -> "#0000AA";
            case '2' -> "#00AA00";
            case '3' -> "#00AAAA";
            case '4' -> "#AA0000";
            case '5' -> "#AA00AA";
            case '6' -> "#FFAA00";
            case '7' -> "#AAAAAA";
            case '8' -> "#555555";
            case '9' -> "#5555FF";
            case 'a' -> "#55FF55";
            case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555";
            case 'd' -> "#FF55FF";
            case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF";
            default -> null;
        };
    }
}
