/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.nametags.render;

import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.util.player.PlayerHealthResolver;
import dev.loki.lovisual.util.pvp.PvpTargetState;
import dev.loki.lovisual.util.text.LegacyTextUtil;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagModel;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import dev.loki.lovisual.features.gui.hud.draggable.impl.stats.Ping;

public final class NameTagLabelBuilder {

    private static final int HP_GREEN = 0xFF55FF55;
    private static final int HP_CYAN = 0xFF55FFFF;
    private static final int HP_RED = 0xFFFF5555;

    private NameTagLabelBuilder() {
    }

    public static NameTagModel.LabelInfo buildLabelInfo(NameTags self, Player player) {
        Component styledName = resolveNameplateText(self, player);
        String name = styledName == null ? "" : styledName.getString();
        String prefix = "";
        if (self.pvpPrefix.get() && PvpTargetState.isTargetInPvp(player.getUUID())) {
            prefix = "[PVP] ";
        }
        int ping = getPlayerPing(self, player);
        String pingText = shouldShowPingText(self) && ping >= 0 ? ping + "ms" : "";
        if (isCreative(self, player)) {
            return new NameTagModel.LabelInfo(prefix, styledName, name, ping, pingText, "GM", PlayerRelations.get().colorStaff());
        }
        PlayerHealthResolver.HealthSnapshot health = PlayerHealthResolver.resolve(player);
        String hp = formatHealth(self, health.totalHealth());
        int color = resolveHealthColor(self, health.totalHealth(), health.maxHealth());
        return new NameTagModel.LabelInfo(prefix, styledName, name, ping, pingText, hp, color);
    }

    static Component resolveNameplateText(NameTags self, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null) return Component.empty();
        String fallback = player.getGameProfile().name();
        if (!shouldUseTabNames(self)) return Component.literal(fallback);

        var handler = mc.getConnection();
        if (handler == null) return Component.literal(fallback);
        PlayerInfo entry = handler.getPlayerInfo(player.getUUID());
        if (entry == null) return Component.literal(fallback);

        Component displayName = entry.getTabListDisplayName();
        if (displayName == null) return Component.literal(fallback);

        Component parsed = LegacyTextUtil.convertLegacyCodes(displayName);
        if (parsed == null || parsed.getString().isBlank()) return Component.literal(fallback);
        return parsed;
    }

    static String formatHealth(NameTags self, float hp) {
        if (self.hpDecimals.get()) {
            return formatTenths(hp);
        }
        int value = (int) Math.ceil(hp);
        return String.valueOf(value);
    }

    public static String formatNameplateHp(String hp) {
        return hp == null ? "" : hp;
    }

    private static String formatTenths(float value) {
        int tenth = Math.round(value * 10f);
        int whole = tenth / 10;
        int frac = Math.abs(tenth % 10);
        return whole + "." + frac;
    }

    static int resolveHealthColor(NameTags self, float current, float max) {
        if (current > max) return HP_CYAN;
        if ("Static".equalsIgnoreCase(self.hpColorMode.get())) {
            return HP_GREEN;
        }
        return gradientHealthColor(current, max);
    }

    private static int gradientHealthColor(float current, float max) {
        if (max <= 0f) return HP_RED;
        float clamped = Mth.clamp(current, 0f, max);
        if (max <= 20f) {
            float t = clamped / max;
            return lerpColor(HP_RED, HP_GREEN, t);
        }
        if (clamped <= 20f) {
            float t = clamped / 20f;
            return lerpColor(HP_RED, HP_GREEN, t);
        }
        float t = (clamped - 20f) / (max - 20f);
        return lerpColor(HP_GREEN, HP_CYAN, Mth.clamp(t, 0f, 1f));
    }

    static boolean isCreative(NameTags self, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player.isCreative()) return true;
        var handler = mc.getConnection();
        if (handler == null) return false;
        PlayerInfo entry = handler.getPlayerInfo(player.getUUID());
        if (entry == null) return false;
        GameType mode = entry.getGameMode();
        return mode == GameType.CREATIVE;
    }

    static boolean shouldUseTabNames(NameTags self) {
        return self.nameplateExtras.get("Tab names");
    }

    static boolean shouldShowPingText(NameTags self) {
        return self.nameplateExtras.get("Ping");
    }

    static int getPlayerPing(NameTags self, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc == null || mc.getConnection() == null) return -1;
        PlayerInfo entry = mc.getConnection().getPlayerInfo(player.getUUID());
        return entry != null ? entry.getLatency() : -1;
    }

    static int resolvePingColor(int ping) {
        if (ping < 0) {
            return 0xFFD0D0D0;
        }
        if (ping <= 70) {
            return 0xFF55FF55;
        }
        if (ping >= 200) {
            return 0xFFFF5555;
        }
        if (ping <= 135) {
            float t = (ping - 70.0f) / 65.0f;
            return mixColor(0xFF55FF55, 0xFFFFFF55, t);
        }
        float t = (ping - 135.0f) / 65.0f;
        return mixColor(0xFFFFFF55, 0xFFFF5555, t);
    }

    private static int mixColor(int start, int end, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int sa = (start >>> 24) & 0xFF;
        int sr = (start >>> 16) & 0xFF;
        int sg = (start >>> 8) & 0xFF;
        int sb = start & 0xFF;
        int ea = (end >>> 24) & 0xFF;
        int er = (end >>> 16) & 0xFF;
        int eg = (end >>> 8) & 0xFF;
        int eb = end & 0xFF;
        int a = (int) (sa + (ea - sa) * t + 0.5f);
        int r = (int) (sr + (er - sr) * t + 0.5f);
        int g = (int) (sg + (eg - sg) * t + 0.5f);
        int b = (int) (sb + (eb - sb) * t + 0.5f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpColor(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF;
        int aR = (a >>> 16) & 0xFF;
        int aG = (a >>> 8) & 0xFF;
        int aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF;
        int bR = (b >>> 16) & 0xFF;
        int bG = (b >>> 8) & 0xFF;
        int bB = b & 0xFF;
        int oA = Math.round(lerp(aA, bA, t));
        int oR = Math.round(lerp(aR, bR, t));
        int oG = Math.round(lerp(aG, bG, t));
        int oB = Math.round(lerp(aB, bB, t));
        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
