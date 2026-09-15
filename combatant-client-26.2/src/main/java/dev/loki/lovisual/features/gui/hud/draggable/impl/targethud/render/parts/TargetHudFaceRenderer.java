/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.render.parts;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import dev.loki.lovisual.util.pvp.opponents.TotemPopSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.*;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHudFormatter;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.render.TargetHudTextRenderer;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.render.parts.TargetHudEquipmentRenderer;

public final class TargetHudFaceRenderer {

    private static ItemStack totemBadgeStack;

    private TargetHudFaceRenderer() {
    }

    private static ItemStack totemBadgeStack() {
        if (totemBadgeStack == null || totemBadgeStack.isEmpty()) {
            totemBadgeStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        }
        return totemBadgeStack;
    }

    public static void updateTotemPopAnimation(TargetHud element, LivingEntity target, TotemPopSnapshot snapshot, float dt) {
        UUID id = target instanceof Player player ? player.getUUID() : null;
        if (id == null || !id.equals(element.totemPopTargetId)) {
            element.totemPopTargetId = id;
            element.totemPopAnimation = 0.0f;
            element.totemPopTextPulse = 0.0f;
            element.totemPopLastCount = 0;
        }

        int count = snapshot != null ? Math.max(0, snapshot.count()) : 0;
        if (count > 0 && count != element.totemPopLastCount) {
            element.totemPopLastCount = count;
            element.totemPopTextPulse = 1.0f;
        } else if (count <= 0) {
            element.totemPopLastCount = 0;
        }

        float targetAnimation = snapshot != null && snapshot.visible() ? 1.0f : 0.0f;
        element.totemPopAnimation = AnimationUtility.approach(element.totemPopAnimation, targetAnimation, dt, TOTEM_BADGE_ANIM_SPEED);
        element.totemPopAnimation = AnimationUtility.snap(element.totemPopAnimation, targetAnimation, 0.001f);
        element.totemPopTextPulse = AnimationUtility.approach(element.totemPopTextPulse, 0.0f, dt, TOTEM_BADGE_PULSE_SPEED);
        element.totemPopTextPulse = AnimationUtility.snap(element.totemPopTextPulse, 0.0f, 0.001f);
    }

    public static void drawFace(TargetHud element, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, LivingEntity target, float x, float y, float scaleFactor, float alphaFactor, TotemPopSnapshot totemPopSnapshot) {
        float faceSize = FACE_SIZE * scaleFactor;
        float faceX = x + (FACE_X + CONTENT_X_OFFSET) * scaleFactor;
        float faceY = y + (FACE_Y + CONTENT_Y_OFFSET) * scaleFactor;
        float faceRadius = FACE_RADIUS * scaleFactor;

        float hurtPercent = target.hurtTime > 0 ? Math.min(1.0f, target.hurtTime / 10.0f) : 0.0f;
        int red = 255;
        int green = Math.round(255.0f * (1.0f - hurtPercent));
        int blue = Math.round(255.0f * (1.0f - hurtPercent));
        int tint = TargetHudFormatter.rgba(red, green, blue, Math.round(255.0f * alphaFactor));

        if (target instanceof Player player) {
            drawPlayerFace(player, faceX, faceY, faceSize, faceRadius, tint);
        } else {
            drawPlaceholderFace(element, renderer, textRenderer, target, faceX, faceY, faceSize, faceRadius, alphaFactor, hurtPercent);
        }

        drawTotemPopBadge(element, renderer, textRenderer, totemPopSnapshot, faceX, faceY, faceSize, faceRadius, scaleFactor, alphaFactor);
    }

    private static void drawPlayerFace(Player player, float x, float y, float size, float radius, int tint) {
        Identifier skin = PlayerSkinResolver.resolveProfileSkin(player.getGameProfile());
        if (skin == null) return;

        Renderer2D.TEXTURE.roundedTexRect(
                x, y, size, size,
                radius, 1.0f,
                FACE_U0, FACE_V0, FACE_U1, FACE_V1,
                tint,
                skin
        );

        float hatSize = size * 1.1f;
        float hatOffset = (hatSize - size) * 0.5f;
        Renderer2D.TEXTURE.roundedTexRect(
                x - hatOffset, y - hatOffset, hatSize, hatSize,
                radius * 1.05f, 1.0f,
                HAT_U0, HAT_V0, HAT_U1, HAT_V1,
                tint,
                skin
        );
    }

    public static void drawPlaceholderFace(TargetHud element, Renderer2D renderer, TextRenderer textRenderer, LivingEntity target, float x, float y, float size, float radius, float alphaFactor, float hurtPercent) {
        Minecraft mc = Minecraft.getInstance();
        int light = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(element.uiBgSecondary, element.uiHealthEnd, 0.28f), alphaFactor);
        int dark = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(element.uiBgPrimary, 0xFF000000, 0.22f), alphaFactor);
        renderer.roundedRectGradient(x, y, size, size, radius, 1.0f, light, dark, 90.0f);
        renderer.roundedRectStroke(
                x, y, size, size, radius, 1.0f,
                0.65f * size / FACE_SIZE,
                HudRenderUtil.scaleAlpha(element.uiStroke, alphaFactor)
        );

        ItemStack mobIcon = resolveMobIcon(target);
        if (mobIcon != null && !mobIcon.isEmpty()) {
            float iconSize = size * 0.72f;
            float itemScale = Math.max(0.35f, iconSize / 16.0f);
            float ix = x + (size - iconSize) * 0.5f;
            float iy = y + (size - iconSize) * 0.5f;
            // Slight hurt tint via background already; keep icon full alpha
            TargetHudEquipmentRenderer.drawItem(renderer, mobIcon, ix, iy, itemScale, 0, alphaFactor);
            // Add tiny hurt overlay as text shadow alternative is already via tint
            return;
        }

        String name = target.getName().getString();
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        TextRenderer faceRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        float textScale = PLACEHOLDER_TEXT_SCALE * element.scale.get().floatValue() * HudScale.scale(
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight()
        );
        faceRenderer.begin(textScale, false, false);
        try {
            float textWidth = (float) faceRenderer.getWidth(letter, false);
            float textHeight = (float) faceRenderer.getHeight(false);
            float tx = x + (size - textWidth) * 0.5f;
            float ty = y + (size - textHeight) * 0.5f - element.scale.get().floatValue();
            int letterColor = HudRenderUtil.scaleAlpha(
                    HudRenderUtil.mixColor(element.uiTextPrimary, element.uiTextSecondary, hurtPercent * 0.35f),
                    alphaFactor
            );
            faceRenderer.render(letter, tx, ty, new RenderColor(letterColor), false);
        } finally {
            faceRenderer.end();
        }
    }

    private static ItemStack resolveMobIcon(LivingEntity target) {
        if (target == null) return null;
        try {
            EntityType<?> type = target.getType();
            Optional<Holder<Item>> eggHolder = SpawnEggItem.byId(type);
            if (eggHolder.isPresent()) {
                return new ItemStack(eggHolder.get().value());
            }
        } catch (Throwable ignored) {
        }
        // Fallback skulls / heads for bosses and special mobs without spawn eggs
        try {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
            if (id.contains("ender_dragon")) return new ItemStack(Items.DRAGON_HEAD);
            if (id.contains("wither")) return new ItemStack(Items.WITHER_SKELETON_SKULL);
            if (id.contains("iron_golem")) return new ItemStack(Items.IRON_BLOCK);
            if (id.contains("snow_golem")) return new ItemStack(Items.SNOW_BLOCK);
            if (id.contains("giant")) return new ItemStack(Items.ZOMBIE_HEAD);
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void drawTotemPopBadge(TargetHud element, Renderer2D renderer, TextRenderer textRenderer, TotemPopSnapshot snapshot, float faceX, float faceY, float faceSize, float faceRadius, float scaleFactor, float alphaFactor) {
        if (snapshot == null || snapshot.count() <= 0 || element.totemPopAnimation <= 0.001f) {
            return;
        }

        float anim = AnimationUtility.easeInOutCubic(element.totemPopAnimation);
        float visualAlpha = alphaFactor * anim;
        if (visualAlpha <= 0.001f) {
            return;
        }

        String text = "x" + snapshot.count();
        TextRenderer badgeRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        float pulse = AnimationUtility.easeOutBack(element.totemPopTextPulse, 1.12f);
        float groupScale = (0.86f + 0.14f * anim) * (1.0f + 0.13f * pulse);
        float textScale = TOTEM_BADGE_TEXT_SCALE * scaleFactor * groupScale;
        float iconSize = Math.min(faceSize * 0.38f, TOTEM_BADGE_ICON_SIZE * scaleFactor * groupScale);
        float gap = TOTEM_BADGE_TEXT_GAP * scaleFactor;
        float textWidth = TargetHudTextRenderer.measureText(badgeRenderer, text, textScale);
        float textHeight = TargetHudTextRenderer.getTextHeight(badgeRenderer, textScale);
        float groupWidth = iconSize + gap + textWidth;
        float groupHeight = Math.max(iconSize, textHeight);

        float groupX = faceX + (faceSize - groupWidth) * 0.5f;
        float groupY = faceY + faceSize - groupHeight - TOTEM_BADGE_BOTTOM_PAD * scaleFactor
                - (1.0f - anim) * 1.4f * scaleFactor;
        float iconX = groupX;
        float iconY = groupY + (groupHeight - iconSize) * 0.5f;
        TargetHudEquipmentRenderer.drawItem(renderer, totemBadgeStack(), iconX, iconY, Math.max(0.1f, iconSize / 16.0f), 713, visualAlpha);

        float textX = iconX + iconSize + gap;
        float textY = groupY + (groupHeight - textHeight) * 0.5f - 0.12f * scaleFactor;
        int textColor = HudRenderUtil.setAlpha(element.totemPopTextColor.getArgb(), Math.round(255.0f * visualAlpha));
        badgeRenderer.begin(textScale, false, false);
        try {
            badgeRenderer.render(text, textX, textY, new RenderColor(textColor), true);
        } finally {
            badgeRenderer.end();
        }
    }
}
