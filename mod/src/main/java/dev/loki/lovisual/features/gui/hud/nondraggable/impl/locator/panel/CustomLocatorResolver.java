package dev.loki.lovisual.features.gui.hud.nondraggable.impl.locator.panel;

import com.mojang.authlib.GameProfile;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar;

public final class CustomLocatorResolver {

    private CustomLocatorResolver() {
    }

    public static int resolveWaypointColor(TrackedWaypoint waypoint) {
        CustomBar bm = CustomBar.get();

        UUID wpUuid = waypoint.id().left().orElse(null);
        boolean isPlayerWaypoint = wpUuid != null;

        if (bm != null && bm.isLocatorSearchEnabled() && isPlayerWaypoint) {
            Set<String> targets = bm.getLocatorSearchPlayers();
            if (targets != null && !targets.isEmpty()) {

                if (isPlayerInTargets(wpUuid, targets)) {
                    return 0xFF000000 | bm.getLocatorSearchColorRgb();
                }

                return 0xFF000000 | bm.getLocatorDimmedColorRgb();
            }
        }

        var cfg = waypoint.icon();
        if (cfg != null && cfg.color.isPresent()) {
            return 0xFF000000 | (cfg.color.get() & 0x00FFFFFF);
        }

        int rgb = waypoint.id()
                .map(CustomLocatorResolver::colorFromUuid, CustomLocatorResolver::colorFromName);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public static AbstractClientPlayer resolveWaypointPlayer(Minecraft mc, UUID uuid) {
        if (mc == null || mc.level == null || uuid == null) return null;
        var player = mc.level.getPlayerByUUID(uuid);
        return player instanceof AbstractClientPlayer clientPlayer ? clientPlayer : null;
    }

    public static Identifier resolveWaypointSkin(Minecraft mc, UUID uuid, TrackedWaypoint waypoint) {
        GameProfile profile = null;
        if (mc != null && mc.getConnection() != null && uuid != null) {
            PlayerInfo entry = mc.getConnection().getPlayerInfo(uuid);
            if (entry != null && entry.getProfile() != null) {
                profile = entry.getProfile();
            }
        }

        if (profile == null && uuid != null) {
            profile = new GameProfile(uuid, uuid.toString());
        }

        if (profile == null) {
            String name = waypoint.id().right().orElse(null);
            if (name != null && !name.isBlank()) {
                profile = new GameProfile(offlineUuid(name), name);
            }
        }

        return PlayerSkinResolver.resolveProfileSkin(profile);
    }

    public static String getWaypointName(Minecraft mc, TrackedWaypoint waypoint, UUID uuid) {

        if (mc != null && mc.getConnection() != null && uuid != null) {
            PlayerInfo entry = mc.getConnection().getPlayerInfo(uuid);
            if (entry != null && entry.getProfile() != null) {
                String name = entry.getProfile().name();
                if (name != null && !name.isBlank()) return name;
            }
        }

        String fallback = waypoint.id().right().orElse(null);
        if (fallback != null && !fallback.isBlank()) return fallback;
        return uuid != null ? uuid.toString() : null;
    }

    public static boolean isLocatorTarget(UUID wpUuid, CustomBar bm) {
        if (bm == null || wpUuid == null || !bm.isLocatorSearchEnabled()) return false;
        Set<String> targets = bm.getLocatorSearchPlayers();
        if (targets == null || targets.isEmpty()) return false;
        return isPlayerInTargets(wpUuid, targets);
    }

    public static boolean isLocatorDimmed(UUID wpUuid, CustomBar bm) {
        if (bm == null || wpUuid == null || !bm.isLocatorSearchEnabled()) return false;
        Set<String> targets = bm.getLocatorSearchPlayers();
        if (targets == null || targets.isEmpty()) return false;
        return !isPlayerInTargets(wpUuid, targets);
    }

    public static UUID offlineUuid(String name) {
        if (name == null || name.isBlank()) return new UUID(0L, 0L);
        String key = "OfflinePlayer:" + name;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isPlayerInTargets(UUID wpUuid, Set<String> targets) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return false;

        for (String raw : targets) {
            if (raw == null) continue;
            String t = raw.trim();
            if (t.isEmpty()) continue;

            if (t.equalsIgnoreCase(wpUuid.toString())) {
                return true;
            }

            PlayerInfo entry = findEntryByName(mc, t);
            if (entry != null && entry.getProfile() != null) {
                if (wpUuid.equals(entry.getProfile().id())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static PlayerInfo findEntryByName(Minecraft mc, String name) {
        for (PlayerInfo e : mc.getConnection().getOnlinePlayers()) {
            if (e.getProfile() != null
                    && e.getProfile().name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    public static int colorFromUuid(UUID uuid) {
        if (uuid == null) return 0xFFFFFFFF;
        return colorFromHash(uuid.hashCode());
    }

    public static int colorFromName(String name) {
        if (name == null) return 0xFFFFFFFF;
        return colorFromHash(name.hashCode());
    }

    public static int colorFromHash(int hash) {
        float hue = (hash & 0xFFFF) / 65535.0f;
        int rgb = Mth.hsvToRgb(hue, 0.9f, 0.9f);
        return rgb & 0x00FFFFFF;
    }
}
