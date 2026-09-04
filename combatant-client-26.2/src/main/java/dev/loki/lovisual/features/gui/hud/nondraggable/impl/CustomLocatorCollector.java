/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.util.Mth;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CustomLocatorCollector {

    private CustomLocatorCollector() {
    }

    static List<CustomBarModel.LocatorCandidate> collect(Minecraft mc, CustomBar bm, TrackedWaypoint.Camera yawProvider,
                                                         PartialTickSupplier tickProgress, Set<UUID> mapPlayers,
                                                         ClientWaypointManager handler) {
        List<CustomBarModel.LocatorCandidate> candidates = new ArrayList<>();

        handler.forEachWaypoint(mc.player, waypoint -> {
            if (isSourcePlayer(mc.player, waypoint)) return;
            UUID wpUuid = waypoint.id().left().orElse(null);
            if (wpUuid != null && !mapPlayers.isEmpty() && mapPlayers.contains(wpUuid)) return;

            double yaw = waypoint.yawAngleToCamera(mc.level, yawProvider, tickProgress);
            if (yaw <= -CustomBar.LOCATOR_ANGLE_RANGE || yaw >= CustomBar.LOCATOR_ANGLE_RANGE) return;

            float dist = Mth.sqrt((float) waypoint.distanceSquared(mc.player));
            String name = CustomLocatorResolver.getWaypointName(mc, waypoint, wpUuid);
            String key = locatorLabelKey(waypoint, wpUuid);

            candidates.add(new CustomBarModel.LocatorCandidate(
                    waypoint,
                    wpUuid,
                    (float) yaw,
                    dist,
                    name,
                    key
            ));
        });

        return candidates;
    }

    static boolean isSourcePlayer(LocalPlayer player, TrackedWaypoint waypoint) {
        if (player == null || waypoint == null) return false;
        return waypoint.id().left()
                .map(uuid -> uuid.equals(player.getUUID()))
                .orElse(false);
    }

    static String locatorLabelKey(TrackedWaypoint waypoint, UUID uuid) {
        if (uuid != null) return uuid.toString();
        return waypoint.id().right().orElse(null);
    }

    static final class Pass {
        final GuiGraphicsExtractor ctx;
        final Minecraft mc;
        final CustomBar bm;
        final PartialTickSupplier tickProgress;
        final float barY;
        final int startX;
        final boolean drawArrows;
        final boolean drawHeads;
        final boolean labelsEnabled;
        final CustomBarModel.LocatorLabelMode labelMode;
        final float labelAngle;
        final float dt;
        final Set<String> seenLabels;
        final List<CustomBarModel.LocatorLabel> labels;
        final List<CustomBarModel.LocatorHead> heads;

        Pass(GuiGraphicsExtractor ctx, Minecraft mc, CustomBar bm, PartialTickSupplier tickProgress,
             float barY, int startX, boolean drawArrows, boolean drawHeads,
             boolean labelsEnabled, CustomBarModel.LocatorLabelMode labelMode, float labelAngle, float dt,
             Set<String> seenLabels, List<CustomBarModel.LocatorLabel> labels, List<CustomBarModel.LocatorHead> heads) {
            this.ctx = ctx;
            this.mc = mc;
            this.bm = bm;
            this.tickProgress = tickProgress;
            this.barY = barY;
            this.startX = startX;
            this.drawArrows = drawArrows;
            this.drawHeads = drawHeads;
            this.labelsEnabled = labelsEnabled;
            this.labelMode = labelMode;
            this.labelAngle = labelAngle;
            this.dt = dt;
            this.seenLabels = seenLabels;
            this.labels = labels;
            this.heads = heads;
        }
    }
}
