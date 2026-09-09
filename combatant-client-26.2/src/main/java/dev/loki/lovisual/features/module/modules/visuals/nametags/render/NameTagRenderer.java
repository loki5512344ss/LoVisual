/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.nametags.render;

import dev.loki.lovisual.features.relations.CategoryService;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.FontInfo;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.EffectIconRenderer;
import dev.loki.lovisual.render.helpers.ScissorFunction;
import dev.loki.lovisual.util.text.TextRenderUtil;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagLayoutEngine;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagModel;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import dev.loki.lovisual.features.module.modules.visuals.nametags.parts.NameTagEquipmentRenderer;
import dev.loki.lovisual.features.module.modules.visuals.nametags.parts.NameTagOpponentRenderer;
import dev.loki.lovisual.features.module.modules.visuals.nametags.parts.NameTagPlateRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class NameTagRenderer {

    static final float SMALL_TEXT_SCALE = 0.82f;
    private static final float NAMEPLATE_TEXT_SHADOW_OFFSET = 0.0f;

    private NameTagRenderer() {
    }

    public static void ensureFontCache(NameTags self, TextRenderer fallback) {
        self.cachedNameTr = Fonts.renderer("OnestBold", FontInfo.Type.Regular, fallback);
        self.cachedHpTr = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, self.cachedNameTr);
        self.cachedLevelTr = Fonts.renderer("OnestBold", FontInfo.Type.Regular, fallback);
        self.cachedTimeTr = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fallback);
    }

    public static void renderNameTagForeground(NameTags self, Renderer2D renderer, GuiGraphicsExtractor ctx,
                                        TextRenderer textRenderer, NameTagModel.RenderEntry entry) {
        Player player = entry.player();
        int centerX = entry.centerX();
        int nameY = entry.nameY();
        int itemsY = entry.itemsY();
        float distOffset = entry.distOffset();
        NameTagModel.ScreenRect rect = entry.rect();

        if (entry.renderNameplate() && entry.nameplateLayout() != null) {
            NameTagPlateRenderer.drawBackground(self, renderer, player, entry.nameplateLayout());
            NameTagPlateRenderer.drawHead(self, renderer, ctx, player, entry.nameplateLayout());
        }

        final int effectsLift = NameTagEffectsRenderer.scaleRow(
                NameTagEffectsRenderer.computeEnchantLift(self, textRenderer, entry.maxEnchantLines()));

        if (entry.renderSlots()) {
            NameTagEquipmentRenderer.drawRow(self, ctx, entry.player(), entry.slots(), centerX, itemsY);
        }

        if (entry.renderEffects()) {
            EffectIconRenderer.beginBatch();
            NameTagEffectsRenderer.drawRowIcons(ctx, entry.effects(), centerX, itemsY, effectsLift);
            EffectIconRenderer.endBatch();
        }

        NameTagOpponentRenderer.drawOpponentCooldownsStackedIcons(self, ctx, player, rect, centerX, nameY);

        boolean startedBaseText = beginTextRenderer(textRenderer, SMALL_TEXT_SCALE);
        try {
            if (entry.renderSlots()) {
                NameTagEquipmentRenderer.drawRowText(textRenderer, entry.slots(), entry.enchantLines(), centerX, itemsY);
            }

            if (entry.renderEffects()) {
                TextRenderer levelTr = self.cachedLevelTr != null ? self.cachedLevelTr : textRenderer;
                TextRenderer timeTr = self.cachedTimeTr != null ? self.cachedTimeTr : textRenderer;
                NameTagEffectsRenderer.drawRowIconOverlays(self, textRenderer, levelTr, timeTr,
                        entry.effects(), entry.player().getId(), centerX, itemsY, effectsLift);
            }

            NameTagOpponentRenderer.drawOpponentCooldownsStackedText(self, textRenderer, player, rect, centerX, nameY);
        } finally {
            endTextRenderer(textRenderer, startedBaseText);
        }

        if (entry.renderNameplate() && entry.labelInfo() != null) {
            NameTagModel.LabelInfo info = entry.labelInfo();
            NameTagModel.NameplateLayout layout = entry.nameplateLayout();
            if (layout != null) {
                TextRenderer nameTr = self.cachedNameTr != null ? self.cachedNameTr : textRenderer;
                TextRenderer hpTr = self.cachedHpTr != null ? self.cachedHpTr : nameTr;
                boolean clipped = pushScissor(layout);
                float scale = layout.labelScale();
                double nameX = layout.nameX();
                boolean startedName = beginTextRenderer(nameTr, scale);
                double cursorX = nameX;
                try {
                    if (!layout.pingText().isEmpty()) {
                        renderNameplateText(nameTr, layout.pingText(), cursorX, nameY,
                                NameTagLabelBuilder.resolvePingColor(info.ping()));
                        cursorX += layout.pingWidth() + layout.pingGap();
                    }
                    String prefix = info.pvpPrefix();
                    if (!prefix.isEmpty()) {
                        renderNameplateText(nameTr, prefix, cursorX, nameY, self.pvpPrefixColor.getArgb());
                        cursorX += nameTr.getWidth(prefix);
                    }
                    if (NameTagLabelBuilder.shouldUseTabNames(self)) {
                        renderStyledNameplateText(nameTr, info.styledName(), cursorX, nameY, CategoryService.getColor(player));
                    } else {
                        renderNameplateText(nameTr, info.name(), cursorX, nameY, CategoryService.getColor(player));
                    }
                } finally {
                    endTextRenderer(nameTr, startedName);
                }

                if (!info.hp().isEmpty()) {
                    double hpX = nameX + layout.pingWidth() + layout.pingGap() + layout.nameWidth() + layout.hpGap();
                    boolean startedHp = beginTextRenderer(hpTr, scale);
                    try {
                        renderNameplateText(hpTr, NameTagLabelBuilder.formatNameplateHp(info.hp()), hpX, nameY, entry.infoColor());
                    } finally {
                        endTextRenderer(hpTr, startedHp);
                    }
                }

                if (!layout.distText().isEmpty()) {
                    double distX = nameX + layout.pingWidth() + layout.pingGap() + layout.nameWidth() + layout.hpGap()
                            + layout.hpWidth() + (layout.hpWidth() > 0 ? NameTagLayoutEngine.DIST_TEXT_GAP : 0);
                    boolean startedDist = beginTextRenderer(nameTr, scale);
                    try {
                        renderNameplateText(nameTr, layout.distText(), distX, nameY, 0xFFD0D0D0);
                    } finally {
                        endTextRenderer(nameTr, startedDist);
                    }
                }
                if (clipped) ScissorFunction.pop();
                NameTagPlateRenderer.drawTotemBadge(self, renderer, textRenderer, layout,
                        entry.totemPopCount(), entry.totemPopAnimation());
            }
        }
    }

    static void renderNameplateText(TextRenderer textRenderer, String text, double x, double y, int color) {
        if (text == null || text.isEmpty()) return;
        if (NAMEPLATE_TEXT_SHADOW_OFFSET > 0.001f) {
            RenderColor shadow = new RenderColor(0x42000000);
            double o = NAMEPLATE_TEXT_SHADOW_OFFSET;
            textRenderer.render(text, x + o, y, shadow, false);
            textRenderer.render(text, x - o, y, shadow, false);
            textRenderer.render(text, x, y + o, shadow, false);
            textRenderer.render(text, x, y - o, shadow, false);
        }
        textRenderer.render(text, x, y, new RenderColor(color), false);
    }

    static void renderStyledNameplateText(TextRenderer textRenderer, Component text, double x, double y, int defaultColor) {
        if (text == null) return;
        List<TextRenderUtil.Part> parts = TextRenderUtil.flattenStyled(text, defaultColor);
        double cursorX = x;
        for (TextRenderUtil.Part part : parts) {
            renderNameplateText(textRenderer, part.text(), cursorX, y, part.color());
            cursorX += textRenderer.getWidth(part.text());
        }
    }

    static boolean beginTextRenderer(TextRenderer textRenderer, double scale) {
        if (textRenderer == null || textRenderer.isBuilding()) return false;
        textRenderer.begin(scale);
        return true;
    }

    static void endTextRenderer(TextRenderer textRenderer, boolean started) {
        if (!started || textRenderer == null || !textRenderer.isBuilding()) return;
        textRenderer.end();
    }

    static boolean pushScissor(NameTagModel.NameplateLayout layout) {
        if (layout == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return false;

        int sw = mc.getWindow().getScreenWidth();
        int sh = mc.getWindow().getScreenHeight();

        int x1 = Math.max(0, (int) Math.floor(layout.bgX()));
        int y1 = Math.max(0, (int) Math.floor(layout.bgY()));
        int x2 = Math.min(sw, (int) Math.ceil(layout.bgX() + layout.bgW()));
        int y2 = Math.min(sh, (int) Math.ceil(layout.bgY() + layout.bgH()));

        if (x2 <= x1 || y2 <= y1) return false;
        return ScissorFunction.pushRawRect(x1, y1, x2, y2);
    }
}
