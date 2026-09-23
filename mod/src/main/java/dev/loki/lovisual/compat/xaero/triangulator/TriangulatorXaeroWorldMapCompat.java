/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.compat.xaero.triangulator;

import org.jetbrains.annotations.Nullable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.map.mods.SupportXaeroMinimap;

public enum TriangulatorXaeroWorldMapCompat {
    ;

    @Nullable
    public static xaero.map.mods.gui.Waypoint getWaypoint(SupportXaeroMinimap minimap) {
        TriangulatorXaeroState.XaeroSnapshot snapshot = TriangulatorXaeroState.snapshot();
        if (snapshot == null || minimap == null) {
            return null;
        }

        Waypoint baseWaypoint = new Waypoint(
                (int) Math.round(snapshot.overworldX()),
                TriangulatorXaeroState.DEFAULT_Y,
                (int) Math.round(snapshot.overworldZ()),
                TriangulatorXaeroState.NAME,
                TriangulatorXaeroState.SYMBOL,
                snapshot.ready() ? WaypointColor.GOLD : WaypointColor.RED,
                WaypointPurpose.NORMAL
        );
        baseWaypoint.setTemporary(false);
        baseWaypoint.setYIncluded(false);
        baseWaypoint.setDisabled(false);
        return minimap.convertWaypoint(baseWaypoint, false, TriangulatorXaeroState.SET, minimap.getDimDiv());
    }
}
