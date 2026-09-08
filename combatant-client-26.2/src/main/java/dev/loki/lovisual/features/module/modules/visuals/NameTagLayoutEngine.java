/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.relations.CategoryService;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.text.TextRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

final class NameTagLayoutEngine {

    static final float NAMEPLATE_SCALE = 1.08f;
    static final int DIST_TEXT_GAP = 5;
    static final int NAMEPLATE_PAD_X = 4;
    static final int NAMEPLATE_PAD_Y = 2;
    static final float TOTEM_BADGE_TEXT_GAP = 2.0f;
    static final float TOTEM_BADGE_TEXT_SCALE = 0.72f;
    private static final int PING_TEXT_GAP = 4;
    private static final int HP_TEXT_GAP = 4;
    private static final float MATTE_HEAD_MIN_SIZE = 12.0f;
    private static final float MATTE_HEAD_DIVIDER_GAP = 4.0f;
    private static final float MATTE_HEAD_DIVIDER_WIDTH = 1.0f;
    private static final float MATTE_HEAD_TEXT_GAP = 5.0f;
    private static final float TOTEM_BADGE_GAP = 4.0f;
    private static final float TOTEM_BADGE_DIVIDER_WIDTH = 1.0f;
    private static final float TOTEM_BADGE_DIVIDER_GAP = 4.0f;
    private static final float TOTEM_BADGE_ICON_SIZE = 10.0f;
    private static final float NAMEPLATE_LIFT_MAX = 12f;
    private static final float NAMEPLATE_LIFT_RANGE = 20f;
    private static final int ESP_NAME_SECTION_LIFT = 9;
    private static final int NAMEPLATE_LIFT_POWER = 4;

    private NameTagLayoutEngine() {
    }

    static NameTagModel.NameplateLayout buildLayout(NameTags self, Player player, TextRenderer nameRenderer,
                                                    TextRenderer hpRenderer, NameTagModel.LabelInfo info,
                                                    double dist, int centerX, int nameY,
                                                    int totemPopCount, float totemPopAnimation) {
        if (info == null) return null;
        float labelScale = NAMEPLATE_SCALE;
        String prefix = info.pvpPrefix();
        String name = info.name();
        String hp = NameTagLabelBuilder.formatNameplateHp(info.hp());
        String pingText = info.pingText();
        int pingWidth = pingText.isEmpty() ? 0 : (int) (nameRenderer.getWidth(pingText) * labelScale);
        int pingGap = pingWidth > 0 ? PING_TEXT_GAP : 0;
        double prefixWidth = prefix.isEmpty() ? 0.0 : nameRenderer.getWidth(prefix);
        double rawNameWidth = info.styledName() != null
                ? measureStyledWidth(nameRenderer, info.styledName(), CategoryService.getColor(player))
                : nameRenderer.getWidth(name);
        int nameWidth = (int) ((prefixWidth + rawNameWidth) * labelScale);
        int hpWidth = hp == null || hp.isEmpty() ? 0 : (int) (hpRenderer.getWidth(hp) * labelScale);
        int hpGap = hpWidth > 0 ? HP_TEXT_GAP : 0;
        boolean showDist = shouldShowDistanceText(self, player);
        String distText = showDist ? (int) Math.round(dist) + "m" : "";
        int distWidth = showDist ? (int) (nameRenderer.getWidth(distText) * labelScale) : 0;
        int distGap = distWidth > 0 ? DIST_TEXT_GAP : 0;
        int nameHeight = (int) (nameRenderer.getHeight() * labelScale);
        boolean renderHead = NameTagFaceRenderer.shouldRenderPlayerHead(self, player);
        float headSize = renderHead ? Math.max(MATTE_HEAD_MIN_SIZE, nameHeight + NAMEPLATE_PAD_Y * 1.5f) : 0.0f;
        float headBlock = renderHead
                ? headSize + MATTE_HEAD_DIVIDER_GAP + MATTE_HEAD_DIVIDER_WIDTH + MATTE_HEAD_TEXT_GAP
                : 0.0f;
        int textWidth = pingWidth + pingGap + nameWidth + hpGap + hpWidth + distGap + distWidth;
        int scaledHeight = Math.max(nameHeight, renderHead ? Math.round(headSize) : nameHeight);
        boolean renderTotemBadge = self.totemPopCounter.get() && totemPopCount > 0 && totemPopAnimation > 0.001f;
        String totemText = renderTotemBadge ? "x" + totemPopCount : "";
        float totemIconSize = renderTotemBadge
                ? Math.min(TOTEM_BADGE_ICON_SIZE, Math.max(8.0f, scaledHeight - 2.0f))
                : 0.0f;
        float totemTextWidth = renderTotemBadge
                ? (float) hpRenderer.getWidth(totemText) * TOTEM_BADGE_TEXT_SCALE
                : 0.0f;
        float totemBadgeHeight = renderTotemBadge ? scaledHeight : 0.0f;
        float totemBadgeWidth = renderTotemBadge
                ? totemIconSize + TOTEM_BADGE_TEXT_GAP + totemTextWidth
                : 0.0f;
        float totemBlock = renderTotemBadge
                ? TOTEM_BADGE_GAP + TOTEM_BADGE_DIVIDER_WIDTH + TOTEM_BADGE_DIVIDER_GAP + totemBadgeWidth
                : 0.0f;
        int totalWidth = Math.round(headBlock + textWidth + totemBlock);
        int scaledWidth = totalWidth;
        int contentX = centerX - scaledWidth / 2;
        int nameX = Math.round(contentX + headBlock);

        float bgX = contentX - NAMEPLATE_PAD_X;
        float bgY = nameY - NAMEPLATE_PAD_Y;
        float bgW = scaledWidth + NAMEPLATE_PAD_X * 2f;
        float bgH = scaledHeight + NAMEPLATE_PAD_Y * 2f;
        float headX = contentX;
        float headY = nameY + (scaledHeight - headSize) * 0.5f;
        float dividerX = headX + headSize + MATTE_HEAD_DIVIDER_GAP;
        float dividerH = Math.max(3.0f, bgH * 0.48f);
        float dividerY = bgY + (bgH - dividerH) * 0.5f;
        float totemDividerX = renderTotemBadge ? contentX + headBlock + textWidth + TOTEM_BADGE_GAP : 0.0f;
        float totemDividerH = renderTotemBadge ? Math.max(3.0f, bgH * 0.48f) : 0.0f;
        float totemDividerY = renderTotemBadge ? bgY + (bgH - totemDividerH) * 0.5f : 0.0f;
        float totemBadgeX = renderTotemBadge ? totemDividerX + TOTEM_BADGE_DIVIDER_WIDTH + TOTEM_BADGE_DIVIDER_GAP : 0.0f;
        float totemBadgeY = renderTotemBadge ? nameY : 0.0f;

        return new NameTagModel.NameplateLayout(nameX, nameY, scaledWidth, scaledHeight, labelScale, pingText, distText, bgX, bgY, bgW, bgH,
                pingWidth, pingGap, nameWidth, hpWidth, hpGap, renderHead, headX, headY, headSize,
                dividerX, dividerY, MATTE_HEAD_DIVIDER_WIDTH, dividerH,
                renderTotemBadge, totemDividerX, totemDividerY, TOTEM_BADGE_DIVIDER_WIDTH, totemDividerH,
                totemBadgeX, totemBadgeY, totemBadgeWidth, totemBadgeHeight, totemIconSize);
    }

    static int computeNameY(NameTagModel.ScreenRect rect, int anchorY, float distOffset) {
        if (rect != null) {
            float rectH = (float) (rect.maxY() - rect.minY());
            float dynamicGap = Math.max(3f, rectH * 0.06f);
            return Math.round((float) rect.minY() - dynamicGap - distOffset);
        }
        return Math.round(anchorY - 7 - distOffset);
    }

    static int computeEspNameSectionLift(Player player) {
        ESP esp = Modules.get(ESP.class);
        if (esp == null || !esp.shouldLiftNameTagsSection(player)) return 0;
        return ESP_NAME_SECTION_LIFT;
    }

    static int computeItemsY(NameTagModel.ScreenRect rect, int nameY, float distOffset) {
        int iconSize = NameTagEquipmentRenderer.equipIconSize();
        if (rect != null) {
            float rectH = (float) (rect.maxY() - rect.minY());
            float itemGap = Math.max(4f, rectH * 0.08f);
            return Math.round((float) rect.minY() - itemGap - iconSize - 6 - distOffset);
        }
        return Math.round(nameY - iconSize - 6 - distOffset);
    }

    static float computeDistanceOffset(double dist) {
        float d = (float) Math.max(0.0, dist);
        float range = Math.max(1f, NAMEPLATE_LIFT_RANGE);
        float max = Math.max(0f, NAMEPLATE_LIFT_MAX);
        float power = Mth.clamp(NAMEPLATE_LIFT_POWER, 1, 6);
        float t = Mth.clamp(d / range, 0f, 1f);
        float eased = 1.0f - (float) Math.pow(1.0f - t, power);
        return max * eased;
    }

    static boolean shouldShowDistanceText(NameTags self, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (!self.toggles.get("Show distance")) return false;
        if (player == null || mc.player == null) return false;
        if (player != mc.player) return true;
        Freecam freecam = Modules.get(Freecam.class);
        return freecam != null && freecam.isEnabled();
    }

    private static float measureStyledWidth(TextRenderer textRenderer, Component text, int defaultColor) {
        if (text == null) return 0.0f;
        float width = 0.0f;
        for (TextRenderUtil.Part part : TextRenderUtil.flattenStyled(text, defaultColor)) {
            width += (float) textRenderer.getWidth(part.text());
        }
        return width;
    }
}
