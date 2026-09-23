/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.player.health.PlayerHealthResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class RadarTextRenderer {

    private RadarTextRenderer() {}

    static void renderText(Radar host, TextRenderer textRenderer, int screenW, int screenH, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        boolean ready = mc != null && mc.player != null && mc.level != null;
        if (!ready && !preview) {
            host.setEmpty();
            return;
        }

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer renderer = Fonts.renderer("Iosevka", FontInfo.Type.Regular, fallback);

        float scale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * Radar.TEXT_SCALE_MULT;

        renderer.begin(scale, false, false);
        float lineH = (float) renderer.getHeight(false);
        renderer.end();

        int textColor = host.colorMode.get() == Radar.ColorMode.CUSTOM
                ? 0xFF000000 | (host.textColor.getArgb() & 0x00FFFFFF)
                : theme().accent();

        float maxW = 0f;
        float yOff = 0f;
        if (ready) {
            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;
                String line = buildLine(host, player);
                if (line.isEmpty()) continue;

                renderer.begin(scale, false, false);
                float w = (float) renderer.getWidth(line, false);
                renderer.render(line, host.getX(), host.getY() + yOff, new RenderColor(textColor), false);
                renderer.end();

                maxW = Math.max(maxW, w);
                yOff += lineH;
            }
        }

        if (preview && yOff <= 0f) {
            String line = "Player 20 40ms";
            renderer.begin(scale, false, false);
            float w = (float) renderer.getWidth(line, false);
            renderer.render(line, host.getX(), host.getY(), new RenderColor(textColor), false);
            renderer.end();
            maxW = w;
            yOff = lineH;
        }

        host.setSize(maxW, Math.max(lineH, yOff));
    }

    static String buildLine(Radar host, Player player) {
        StringBuilder out = new StringBuilder(64);
        appendField(host, out, host.field1.get(), player);
        appendField(host, out, host.field2.get(), player);
        appendField(host, out, host.field3.get(), player);
        appendField(host, out, host.field4.get(), player);
        appendField(host, out, host.field5.get(), player);
        return out.toString().trim();
    }

    static void appendField(Radar host, StringBuilder out, Radar.Field field, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (field == null || field == Radar.Field.NONE) return;
        if (out.length() > 0) out.append(' ');
        switch (field) {
            case HP -> {
                int hp = (int) Math.ceil(PlayerHealthResolver.totalHealth(player));
                out.append(hp);
            }
            case DISTANCE -> {
                int dist = (int) Math.ceil(mc.player.distanceTo(player));
                out.append(dist).append('m');
            }
            case PING -> {
                int ping = getPlayerPing(player);
                if (ping >= 0) {
                    out.append(ping).append("ms");
                } else {
                    out.append("--");
                }
            }
            case NAME -> out.append(player.getName().getString());
            default -> {
                // ignore
            }
        }
    }

    static int getPlayerPing(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return -1;
        PlayerInfo entry = mc.getConnection().getPlayerInfo(player.getUUID());
        return entry != null ? entry.getLatency() : -1;
    }
}
