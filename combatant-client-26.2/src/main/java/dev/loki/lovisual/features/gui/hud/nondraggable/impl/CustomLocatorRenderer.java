/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.ViewportContext;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.util.map.MapPlayersCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CustomLocatorRenderer {

    private CustomLocatorRenderer() {
    }

    static void renderLocatorBackground(Renderer2D r2d, Minecraft mc) {
        if (mc == null || mc.player == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;
        float scale = (float) mc.getWindow().getGuiScale();
        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        float baseY = screenH - 22f;
        float x = (screenW - CustomBar.XP_BAR_WIDTH) * 0.5f;
        float y = baseY - CustomBar.XP_BAR_OFFSET - CustomBar.LOCATOR_BAR_HEIGHT;
        CustomXpBarRenderer.drawVanillaGlassTrack(r2d, x, y, scale, 1.0f, bm, true);
    }

    static void renderLocatorTexture(GuiGraphicsExtractor ctx, Minecraft mc) {
        if (ctx == null || mc == null || mc.player == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;
        int x = Mth.floor((mc.getWindow().getGuiScaledWidth() - CustomBar.XP_BAR_WIDTH) * 0.5f);
        int y = Mth.floor(mc.getWindow().getGuiScaledHeight() - 22f - CustomBar.XP_BAR_OFFSET - CustomBar.LOCATOR_BAR_HEIGHT);
        ctx.blitSprite(LoVisualRenderPipelines.GUI_TEXTURE_LOOKUP, CustomBar.LOCATOR_BACKGROUND, x, y, 182, 5,
                CustomXpBarRenderer.applyBarAlpha(CustomXpBarRenderer.colors(bm).locatorTexture(), bm));
    }

    static void renderLocatorArrows(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        renderLocatorMarkers(ctx, tickCounter, mc, true, false);
    }

    static void renderLocatorHeads(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        renderLocatorMarkers(ctx, tickCounter, mc, false, true);
    }

    static void renderLocatorMarkers(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        renderLocatorMarkers(ctx, tickCounter, mc, true, true);
    }

    static void renderLocatorMarkers(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc,
                                     boolean drawArrows, boolean drawHeads) {
        if (ctx == null || mc == null || mc.player == null || mc.level == null) return;
        CustomBar bm = CustomBar.get();
        if (bm == null || !bm.isHudBarEnabled() || !bm.isHotbarXpBarEnabled()) return;
        if (mc.getConnection() == null) return;
        ClientWaypointManager handler = mc.getConnection().getWaypointManager();
        if (handler == null || !handler.hasWaypoints()) return;

        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();
        float baseY = screenH - 22f;
        float barX = (screenW - CustomBar.XP_BAR_WIDTH) * 0.5f;
        float barY = baseY - CustomBar.XP_BAR_OFFSET - CustomBar.LOCATOR_BAR_HEIGHT;

        int startX = Mth.floor(barX + CustomBar.LOCATOR_HEAD_RANGE * 0.5f);
        PartialTickSupplier tickProgress = entity -> tickCounter.getGameTimeDeltaPartialTick(false);
        var camera = mc.gameRenderer.mainCamera();

        TrackedWaypoint.Camera yawProvider = new TrackedWaypoint.Camera() {
            @Override
            public float yaw() {
                return camera.yRot();
            }

            @Override
            public Vec3 position() {
                return camera.position();
            }
        };

        Set<UUID> mapPlayers = MapPlayersCache.getPlayers(mc, bm);
        boolean labelsEnabled = drawHeads && bm.isLocatorLabelsEnabled();
        CustomBarModel.LocatorLabelMode labelMode = labelsEnabled
                ? bm.getLocatorLabelMode()
                : CustomBarModel.LocatorLabelMode.ALWAYS;
        float labelAngle = labelsEnabled ? bm.getLocatorLabelAngle() : 0f;
        float dt = drawHeads ? AnimationUtility.deltaTime() : 0.0f;

        List<CustomBarModel.LocatorCandidate> candidates =
                CustomLocatorCollector.collect(mc, bm, yawProvider, tickProgress, mapPlayers, handler);
        if (candidates.isEmpty() && !drawHeads) return;

        candidates.sort((a, b) -> {
            int cmp = Float.compare(a.dist, b.dist);
            if (cmp != 0) return cmp;
            cmp = a.nameSort.compareTo(b.nameSort);
            if (cmp != 0) return cmp;
            return a.keySort.compareTo(b.keySort);
        });

        List<CustomBarModel.LocatorHead> heads = new ArrayList<>();
        Set<String> seenLabels = labelsEnabled ? new HashSet<>() : null;
        List<CustomBarModel.LocatorLabel> labels = labelsEnabled ? new ArrayList<>() : null;

        CustomLocatorCollector.Pass pass = new CustomLocatorCollector.Pass(
                ctx, mc, bm, tickProgress, barY, startX, drawArrows, drawHeads,
                labelsEnabled, labelMode, labelAngle, dt, seenLabels, labels, heads);
        for (CustomBarModel.LocatorCandidate candidate : candidates) {
            CustomLocatorMarkers.process(pass, candidate);
        }

        if (!drawHeads) return;

        if (labelsEnabled && labels != null && seenLabels != null) {
            CustomLocatorLabelCache.decay(seenLabels, dt);
            labels.clear();
            CustomLocatorLabelCache.appendCached(labels);
        }

        if (heads.isEmpty() && (labels == null || labels.isEmpty())) return;

        float scale = (float) mc.getWindow().getGuiScale();
        ViewportContext.beginUnscaled(ctx);
        try {
            if (!heads.isEmpty()) {
                CustomLocatorHeads.render(ctx, bm, heads, scale);
            }
            if (labelsEnabled && labels != null && !labels.isEmpty()) {
                CustomLocatorLabels.render(ctx, bm, screenW, barY, scale, labels);
            }
        } finally {
            ViewportContext.end(ctx);
        }
    }

    static boolean beginOwnedColorBatch() {
        if (Renderer2D.UI_BATCHER.isActive()) {
            return false;
        }
        Renderer2D.COLOR.begin();
        return true;
    }

    static void renderOwnedColorBatch(boolean owned) {
        if (owned) {
            Renderer2D.COLOR.render();
        }
    }
}
