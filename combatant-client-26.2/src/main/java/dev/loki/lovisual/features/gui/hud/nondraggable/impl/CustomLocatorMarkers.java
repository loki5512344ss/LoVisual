/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.Locale;
import java.util.UUID;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBar.*;

final class CustomLocatorMarkers {

    private CustomLocatorMarkers() {
    }

    static void process(CustomLocatorCollector.Pass pass, CustomBarModel.LocatorCandidate candidate) {
        CustomBar bm = pass.bm;
        Minecraft mc = pass.mc;

        float yaw = candidate.yaw;
        int offset = Mth.floor(yaw * (LOCATOR_HEAD_RANGE / 2f) / LOCATOR_ANGLE_RANGE);
        float drawX = pass.startX + offset;

        float dist = candidate.dist;
        UUID wpUuid = candidate.uuid;

        boolean isTarget = CustomLocatorResolver.isLocatorTarget(wpUuid, bm);
        boolean isDimmed = CustomLocatorResolver.isLocatorDimmed(wpUuid, bm);
        float headScale = locatorHeadScale(dist, isTarget, isDimmed, bm);
        float headSize = LOCATOR_HEAD_SIZE * headScale;
        float headRadius = LOCATOR_HEAD_RADIUS * headScale;
        float headY = pass.barY + (LOCATOR_BAR_HEIGHT - headSize) * 0.5f + LOCATOR_HEAD_GAP;

        int baseColor = CustomLocatorResolver.resolveWaypointColor(candidate.waypoint);
        RenderColor headColor = new RenderColor(CustomXpBarRenderer.applyBarAlpha(0xFFFFFFFF, bm));
        RenderColor outlineColor = isTarget
                ? new RenderColor(CustomXpBarRenderer.applyBarAlpha(0xFF000000 | bm.getLocatorSearchColorRgb(), bm))
                : locatorOutlineColor(baseColor, dist, bm);
        float outlineThickness = locatorOutlineThickness(dist) * headScale;
        if (isTarget) {
            outlineThickness = Math.max(outlineThickness, 1.85f * headScale);
        }

        AbstractClientPlayer player = CustomLocatorResolver.resolveWaypointPlayer(mc, wpUuid);
        Identifier skin = (player == null) ? CustomLocatorResolver.resolveWaypointSkin(mc, wpUuid, candidate.waypoint) : null;

        if (pass.drawHeads) {
            pass.heads.add(new CustomBarModel.LocatorHead(
                    drawX, headY, headSize, headRadius,
                    player, skin, headColor, outlineColor, outlineThickness, isTarget
            ));
        }

        TrackedWaypoint.PitchDirection pitch = candidate.waypoint.pitchDirectionToCamera(mc.level, mc.gameRenderer, pass.tickProgress);
        if (pass.drawArrows && (pitch == TrackedWaypoint.PitchDirection.UP || pitch == TrackedWaypoint.PitchDirection.DOWN)) {
            Identifier arrow = (pitch == TrackedWaypoint.PitchDirection.DOWN) ? LOCATOR_ARROW_DOWN : LOCATOR_ARROW_UP;
            int xOffset = Mth.floor(drawX + (headSize - LOCATOR_ARROW_W) * 0.5f);
            int yOffset = (pitch == TrackedWaypoint.PitchDirection.DOWN)
                    ? Mth.floor(headY + headSize + LOCATOR_ARROW_GAP)
                    : Mth.floor(headY - LOCATOR_ARROW_H - LOCATOR_ARROW_GAP);
            pass.ctx.blitSprite(
                    LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP,
                    arrow,
                    xOffset,
                    yOffset,
                    LOCATOR_ARROW_W,
                    LOCATOR_ARROW_H,
                    CustomXpBarRenderer.applyBarAlpha(CustomXpBarRenderer.colors(bm).locatorTexture(), bm)
            );
        }

        if (!pass.labelsEnabled || pass.labels == null || pass.seenLabels == null) return;

        String labelKey = candidate.key != null ? candidate.key : candidate.nameSort;
        boolean showLabel = shouldShowLocatorLabel(pass.labelMode, yaw, pass.labelAngle, isTarget);
        if (showLabel && labelKey != null && !labelKey.isBlank()) {
            pass.seenLabels.add(labelKey);
        }
        float anim = labelKey != null && !labelKey.isBlank()
                ? CustomLocatorLabelCache.updateAnim(labelKey, showLabel ? 1f : 0f, pass.dt)
                : (showLabel ? 1f : 0f);
        if (anim <= 0.01f) return;

        String name = candidate.name;
        if (name == null || name.isBlank()) return;

        String distanceText = "";
        if (bm.isLocatorLabelDistanceEnabled()) {
            distanceText = formatDistance(dist);
        }

        CustomBarModel.LocatorLabel label = new CustomBarModel.LocatorLabel(name, distanceText, anim, isTarget, showLabel, candidate.nameSort);
        pass.labels.add(label);
        if (labelKey != null && !labelKey.isBlank()) {
            CustomLocatorLabelCache.cache(labelKey, label);
        }
    }

    private static float locatorHeadScale(float dist, boolean isTarget, boolean isDimmed, CustomBar bm) {
        if (isTarget) return 1.0f;
        boolean dimmedOnly = bm != null && bm.isLocatorDimmedScaleOnly();
        if (dimmedOnly && !isDimmed) return 1.0f;

        float range = LOCATOR_HEAD_SCALE_END - LOCATOR_HEAD_SCALE_START;
        if (range <= 0.0f) return 1.0f;
        float t = Mth.clamp((dist - LOCATOR_HEAD_SCALE_START) / range, 0f, 1f);
        return Mth.lerp(t, 1.0f, LOCATOR_HEAD_MIN_SCALE);
    }

    private static RenderColor locatorOutlineColor(int baseArgb, float dist, CustomBar bm) {
        float t = Mth.clamp(dist / LOCATOR_OUTLINE_DISTANCE, 0f, 1f);
        int baseRgb = baseArgb & 0x00FFFFFF;
        int rgb = dev.loki.lovisual.render.engine.math.ColorMath.mixRgb(baseRgb, 0x000000, 0.25f + 0.35f * t);
        int alpha = Math.round(Mth.lerp(230f, 120f, t));
        int argb = CustomXpBarRenderer.applyBarAlpha((alpha << 24) | rgb, bm);
        return new RenderColor(argb);
    }

    private static float locatorOutlineThickness(float dist) {
        float t = Mth.clamp(dist / LOCATOR_OUTLINE_DISTANCE, 0f, 1f);
        return Mth.lerp(LOCATOR_OUTLINE_MAX, LOCATOR_OUTLINE_MIN, t);
    }

    private static boolean shouldShowLocatorLabel(CustomBarModel.LocatorLabelMode mode, float yaw, float angle, boolean isTarget) {
        float abs = Math.abs(yaw);
        return switch (mode) {
            case ALWAYS -> true;
            case CENTER -> abs <= angle;
            case TARGETS -> isTarget;
            case CENTER_TARGETS -> isTarget || abs <= angle;
        };
    }

    private static String formatDistance(float dist) {
        if (!Float.isFinite(dist)) return "";
        return String.format(Locale.ROOT, "%.0fm", dist);
    }
}
