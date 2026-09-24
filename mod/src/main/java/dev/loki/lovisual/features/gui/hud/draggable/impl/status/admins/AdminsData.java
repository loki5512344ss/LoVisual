package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.relations.StaffData;
import dev.loki.lovisual.features.relations.StaffTracker;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import dev.loki.lovisual.util.text.format.LegacyTextUtil;
import dev.loki.lovisual.util.text.format.TextRenderUtil;
import dev.loki.lovisual.util.text.format.TextRenderUtil.Part;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class AdminsData {

    private AdminsData() {}

    static List<StaffData.StaffEntry> filterStaff(List<StaffData.StaffEntry> in) {
        List<StaffData.StaffEntry> out = new ArrayList<>(in.size());
        for (StaffData.StaffEntry entry : in) {
            if (entry.status() == StaffTracker.Status.OFFLINE && entry.offlineSince() <= 0) continue;
            if (entry.status() == StaffTracker.Status.WAITING && entry.lastGone() <= 0) continue;
            out.add(entry);
        }
        return out;
    }

    static int resolveStatusColor(Admins host, StaffTracker.Status status) {
        return switch (status) {
            case GM0 -> 0xFF55FF55;
            case GM1 -> 0xFF55DDFF;
            case GM2 -> 0xFFFFFF55;
            case GM3 -> 0xFFFF55FF;
            case VANISH -> 0xFFFF7755;
            case WAITING -> 0xFFAAAAAA;
            case OFFLINE -> 0xFF888888;
            default -> host.uiMuted != 0 ? host.uiMuted : 0xFF888888;
        };
    }

    static String formatStatus(StaffData.StaffEntry entry, long now) {
        return switch (entry.status()) {
            case OFFLINE -> entry.offlineSince() <= 0 ? "--:--" : formatTime(now - entry.offlineSince());
            case WAITING -> entry.lastGone() <= 0 ? "--:--" : formatTime(now - entry.lastGone());
            case VANISH -> "Vanish";
            default -> entry.status().name();
        };
    }

    static String formatTime(long ms) {
        if (ms < 0) return "--:--";
        long sec = ms / 1000L;
        long min = sec / 60L;
        long hrs = min / 60L;
        if (hrs == 0) return String.format("%02d:%02d", min, sec % 60L);
        return String.format("%02d:%02d:%02d", hrs, min % 60L, sec % 60L);
    }

    static float measureLegacyName(TextRenderer renderer, Component displayName) {
        Component converted = LegacyTextUtil.convertLegacyCodes(displayName);
        List<Part> parts = TextRenderUtil.flattenStyled(converted, theme().textPrimary());
        float out = 0.0f;
        for (Part part : parts) {
            out += (float) renderer.getWidth(part.text(), false);
        }
        return out;
    }

    static List<LinkedHashMap<String, Object>> legacyParts(TextRenderer renderer,
                                                           Component displayName,
                                                           float alpha,
                                                           int fallbackColor) {
        Component converted = LegacyTextUtil.convertLegacyCodes(displayName);
        List<Part> parts = TextRenderUtil.flattenStyled(converted, fallbackColor);
        List<LinkedHashMap<String, Object>> out = new ArrayList<>(parts.size());
        float cursorX = 0.0f;
        for (Part part : parts) {
            int color = dev.loki.lovisual.features.gui.hud.config.HudRenderUtil.scaleAlpha(part.color(), alpha);
            out.add(dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.textPart(part.text(), color, cursorX));
            cursorX += (float) renderer.getWidth(part.text(), false);
        }
        return out;
    }

    static AbstractClientPlayer resolvePlayerByName(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (name == null || name.isEmpty() || mc == null || mc.level == null) return null;
        for (Player player : mc.level.players()) {
            if (player instanceof AbstractClientPlayer clientPlayer) {
                String playerName = clientPlayer.getGameProfile() != null
                        ? clientPlayer.getGameProfile().name()
                        : clientPlayer.getName().getString();
                if (playerName != null && playerName.equalsIgnoreCase(name)) return clientPlayer;
            }
        }
        return null;
    }

    static Identifier resolveSkinByName(String name) {
        AbstractClientPlayer player = resolvePlayerByName(name);
        if (player != null) return PlayerSkinResolver.resolveProfileSkin(player.getGameProfile());
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getConnection() != null) {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                var profile = entry.getProfile();
                if (profile == null || profile.name() == null) continue;
                if (!profile.name().equalsIgnoreCase(name)) continue;
                Identifier skin = PlayerSkinResolver.resolveProfileSkin(profile);
                if (skin != null) return skin;
            }
        }
        return null;
    }

    static Identifier resolvePreviewSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            return PlayerSkinResolver.resolveProfileSkin(mc.player.getGameProfile());
        }
        return null;
    }
}
