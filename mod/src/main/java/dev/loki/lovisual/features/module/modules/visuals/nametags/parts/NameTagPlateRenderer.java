package dev.loki.lovisual.features.module.modules.visuals.nametags.parts;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.material.MatteHudStyle;
import dev.loki.lovisual.render.engine.renderer.style.StyleSpecs;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;
import dev.loki.lovisual.render.helpers.material.SurfaceRenderer;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagLayoutEngine;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagModel;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.module.modules.visuals.nametags.parts.NameTagFaceRenderer;

public final class NameTagPlateRenderer {

    private static final float NAMEPLATE_RADIUS = 7.0f;
    private static final float MATTE_NAMEPLATE_RADIUS = 4.0f;
    private static final int NAMEPLATE_GLASS_VEIL = 0x2A141414;
    private static ItemStack totemBadgeStack;

    private NameTagPlateRenderer() {
    }

    private static ItemStack totemBadgeStack() {
        if (totemBadgeStack == null || totemBadgeStack.isEmpty()) {
            totemBadgeStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        }
        return totemBadgeStack;
    }

    static boolean isMatteNameplate(NameTags self) {
        return self.nameplateStyle.get() == NameTagModel.NameplateStyle.MATTE;
    }

    static float nameplateRadius(NameTags self, NameTagModel.NameplateLayout layout) {
        if (layout == null) return isMatteNameplate(self) ? MATTE_NAMEPLATE_RADIUS : NAMEPLATE_RADIUS;
        if (isMatteNameplate(self)) {
            return Math.min(MATTE_NAMEPLATE_RADIUS, Math.max(2.5f, layout.bgH() * 0.30f));
        }
        return Math.min(NAMEPLATE_RADIUS, Math.max(4.5f, layout.bgH() * 0.5f));
    }

    public static void drawBackground(NameTags self, Renderer2D renderer, Player player, NameTagModel.NameplateLayout layout) {
        if (layout == null) return;
        int alpha = clampAlpha(self.nameplateAlpha.get());
        if (alpha <= 0) return;

        float globalAlpha = alpha / 255.0f;
        float radius = nameplateRadius(self, layout);
        SurfaceRenderer.plateSpec(renderer,
                layout.bgX(),
                layout.bgY(),
                layout.bgW(),
                layout.bgH(),
                radius, radius, radius, radius,
                StyleSpecs.glass(false),
                1.0f,
                0xFFFFFFFF,
                globalAlpha,
                0.0f,
                isMatteNameplate(self) ? SurfaceStyle.FLAT : SurfaceStyle.GLASS);
        if (isMatteNameplate(self)) return;
        renderer.roundedRect(
                layout.bgX(),
                layout.bgY(),
                layout.bgW(),
                layout.bgH(),
                radius,
                1.0f,
                HudRenderUtil.scaleAlpha(NAMEPLATE_GLASS_VEIL, globalAlpha)
        );
    }

    public static void drawHead(NameTags self, Renderer2D renderer, GuiGraphicsExtractor ctx, Player player,
                         NameTagModel.NameplateLayout layout) {
        if (ctx == null || layout == null || !layout.renderHead()) return;
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        int alpha = clampAlpha(self.nameplateAlpha.get());
        if (alpha <= 0) return;
        float globalAlpha = alpha / 255.0f;

        NameTagFaceRenderer.drawRoundedHead(ctx, layout, clientPlayer, alpha);

        drawDivider(self, renderer,
                layout.dividerX(), layout.dividerY(), layout.dividerW(), layout.dividerH(), globalAlpha);
    }

    static void drawDivider(NameTags self, Renderer2D renderer,
                            float x, float y, float width, float height, float globalAlpha) {
        if (renderer == null || width <= 0.0f || height <= 0.0f || globalAlpha <= 0.001f) return;
        int color = isMatteNameplate(self)
                ? MatteHudStyle.strokeColor(globalAlpha * 0.82f)
                : MatteHudStyle.withAlpha(0xFFFFFF, Math.round(58.0f * globalAlpha));
        float drawWidth = Math.max(0.8f, width);
        renderer.roundedRect(
                x + (width - drawWidth) * 0.5f,
                y,
                drawWidth,
                height,
                Math.min(drawWidth, height) * 0.5f,
                0.0f,
                color
        );
    }

    public static void drawTotemBadge(NameTags self, Renderer2D renderer, TextRenderer fallbackRenderer,
                               NameTagModel.NameplateLayout layout, int count, float animation) {
        if (renderer == null || layout == null || !layout.renderTotemBadge()) return;
        if (count <= 0 || animation <= 0.001f) return;

        float anim = AnimationUtility.easeInOutCubic(animation);
        float globalAlpha = (clampAlpha(self.nameplateAlpha.get()) / 255.0f) * anim;
        if (globalAlpha <= 0.001f) return;

        drawDivider(self, renderer,
                layout.totemDividerX(),
                layout.totemDividerY(),
                layout.totemDividerW(),
                layout.totemDividerH(),
                globalAlpha
        );

        float blockX = layout.totemBadgeX();
        float blockY = layout.totemBadgeY();
        float blockH = layout.totemBadgeH();
        float iconScale = 0.86f + 0.14f * anim;
        float iconSize = Math.max(1.0f, layout.totemIconSize() * iconScale);
        float iconX = blockX;
        float iconY = blockY + (blockH - iconSize) * 0.5f;
        renderer.item(totemBadgeStack(), iconX, iconY, Math.max(0.1f, iconSize / 16.0f), 911,
                Renderer2D.ITEM_OVERLAY_NONE, null);

        String text = "x" + count;
        TextRenderer badgeRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, fallbackRenderer);
        float textScale = NameTagLayoutEngine.TOTEM_BADGE_TEXT_SCALE * (0.94f + 0.06f * anim);
        float textX = iconX + iconSize + NameTagLayoutEngine.TOTEM_BADGE_TEXT_GAP;
        float textH = (float) badgeRenderer.getHeight(false) * textScale;
        float textY = blockY + (blockH - textH) * 0.5f - 0.1f;
        int textColor = HudRenderUtil.setAlpha(theme().textPrimary(), Math.round(255.0f * globalAlpha));

        badgeRenderer.begin(textScale, false, false);
        try {
            if (globalAlpha > 0.96f) {
                int baseColor = textColor;
                int darkColor = HudRenderUtil.mixColor(baseColor,
                        HudRenderUtil.setAlpha(0xFF181B1F, Math.round(255.0f * globalAlpha)), 0.18f);
                int lightColor = HudRenderUtil.mixColor(baseColor,
                        HudRenderUtil.setAlpha(theme().accentSoft(), Math.round(255.0f * globalAlpha)), 0.24f);
                float time = (float) (net.minecraft.util.Util.getMillis() / 1000.0);
                float pulse = time * 2.35f;
                badgeRenderer.renderGradient(text, textX, textY, (idx, cp, gx, out) -> {
                    float p = pulse + idx * 0.62f;
                    float t0 = 0.5f + 0.5f * (float) Math.sin(p);
                    float t1 = 0.5f + 0.5f * (float) Math.sin(p + 1.05f);
                    out[0] = HudRenderUtil.mixColor(darkColor, lightColor, t0);
                    out[1] = HudRenderUtil.mixColor(darkColor, lightColor, t1);
                }, false);
            } else {
                badgeRenderer.render(text, textX, textY, new RenderColor(textColor), false);
            }
        } finally {
            badgeRenderer.end();
        }
    }

    static int clampAlpha(int a) {
        if (a < 0) return 0;
        if (a > 255) return 255;
        return a;
    }
}
