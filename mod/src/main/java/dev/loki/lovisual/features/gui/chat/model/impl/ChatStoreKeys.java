package dev.loki.lovisual.features.gui.chat.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import dev.loki.lovisual.config.io.ConfigPaths;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatHoverCache;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * World/server key derivation, on-disk file resolution and per-key hover-cache ownership for
 * {@link BetterChatStoreManager}. Extracted so the manager facade keeps only the public API and
 * the active-store state.
 */
final class ChatStoreKeys {
    private static final Map<String, BetterChatHoverCache> HOVER_CACHES = new HashMap<>();

    private ChatStoreKeys() {
    }

    static Map<String, BetterChatHoverCache> hoverCaches() {
        return HOVER_CACHES;
    }

    static BetterChatHoverCache cacheFor(String key) {
        return HOVER_CACHES.computeIfAbsent(key, BetterChatHoverCache::load);
    }

    static File fileFor(String key) {
        return betterChatFile(key + ".json");
    }

    private static File betterChatFile(String fileName) {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath()
                .resolve(ConfigPaths.root())
                .resolve("betterchat")
                .resolve(fileName)
                .toFile();
    }

    static String key(Minecraft mc) {
        // IMPORTANT: Do not split history by dimension. One store + one cache per save/server.
        if (mc == null) return null;

        if (mc.hasSingleplayerServer()) {
            // Singleplayer worlds stored under singleplayer/<level>
            String levelName = mc.getSingleplayerServer() != null ? mc.getSingleplayerServer().getWorldData().getLevelName() : "world";
            return "singleplayer/" + sanitizeKeyPart(levelName);
        }

        ServerData info = mc.getCurrentServer();
        if (info != null) {
            // Multiplayer servers stored under servers/<host> (ignore port)
            String host = stripPort(info.ip);
            return "servers/" + sanitizeKeyPart(host);
        }

        // Fallback
        return "singleplayer/world";
    }

    private static String stripPort(String address) {
        if (address == null) return "unknown";
        String s = address.trim();
        if (s.isEmpty()) return "unknown";

        // IPv6 with brackets: [addr]:port
        if (s.startsWith("[")) {
            int end = s.indexOf(']');
            if (end > 1) {
                return s.substring(1, end);
            }
            return s;
        }

        int lastColon = s.lastIndexOf(':');
        if (lastColon > 0 && lastColon + 1 < s.length()) {
            String tail = s.substring(lastColon + 1);
            boolean digits = true;
            for (int i = 0; i < tail.length(); i++) {
                char c = tail.charAt(i);
                if (c < '0' || c > '9') {
                    digits = false;
                    break;
                }
            }
            if (digits) {
                return s.substring(0, lastColon);
            }
        }

        return s;
    }

    /**
     * Sanitizes a key part so it is safe as a file path component.
     */
    private static String sanitizeKeyPart(String s) {
        if (s == null) return "unknown";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "unknown";
        StringBuilder out = null;
        int length = trimmed.length();
        for (int i = 0; i < length; i++) {
            char c = trimmed.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '.'
                    || c == '_'
                    || c == '-';
            if (ok) {
                if (out != null) {
                    out.append(c);
                }
                continue;
            }
            if (out == null) {
                out = new StringBuilder(length);
                out.append(trimmed, 0, i);
            }
            out.append('_');
        }
        return out == null ? trimmed : out.toString();
    }
}
