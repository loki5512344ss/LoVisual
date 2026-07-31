/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.xaero.worldmap;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.compat.xaero.TriangulatorXaeroWorldMapCompat;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.SupportXaeroMinimap;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointRenderContext;
import xaero.map.mods.gui.WaypointRenderProvider;

import java.util.ArrayDeque;
import java.util.Queue;

@Pseudo
@Mixin(WaypointRenderProvider.class)
public final class WaypointRenderProviderMixin {

    @Shadow
    @Final
    private SupportXaeroMinimap minimap;

    @Unique
    private Queue<Waypoint> lovisual$pendingWaypoints;

    @Unique
    private boolean lovisual$originalHadNext;

    @Inject(method = "begin*", at = @At("HEAD"))
    private void lovisual$begin(ElementRenderLocation location,
                                 WaypointRenderContext context,
                                 CallbackInfo ci) {
        lovisual$pendingWaypoints = new ArrayDeque<>();
        Waypoint triangulatorWaypoint = TriangulatorXaeroWorldMapCompat.getWaypoint(minimap);
        if (triangulatorWaypoint != null) {
            lovisual$pendingWaypoints.add(triangulatorWaypoint);
        }
        lovisual$originalHadNext = false;
    }

    @Inject(method = "hasNext*", at = @At("RETURN"), cancellable = true)
    private void lovisual$hasNext(ElementRenderLocation location,
                                   WaypointRenderContext context,
                                   CallbackInfoReturnable<Boolean> cir) {
        lovisual$originalHadNext = cir.getReturnValue();
        cir.setReturnValue(lovisual$originalHadNext
                || (lovisual$pendingWaypoints != null && !lovisual$pendingWaypoints.isEmpty()));
    }

    @Inject(method = "getNext*", at = @At("HEAD"), cancellable = true)
    private void lovisual$getNext(ElementRenderLocation location,
                                   WaypointRenderContext context,
                                   CallbackInfoReturnable<Waypoint> cir) {
        if (lovisual$originalHadNext) {
            lovisual$originalHadNext = false;
            return;
        }
        if (lovisual$pendingWaypoints != null && !lovisual$pendingWaypoints.isEmpty()) {
            cir.setReturnValue(lovisual$pendingWaypoints.poll());
        }
    }

    @Inject(method = "end*", at = @At("HEAD"))
    private void lovisual$end(ElementRenderLocation location,
                               WaypointRenderContext context,
                               CallbackInfo ci) {
        lovisual$pendingWaypoints = null;
        lovisual$originalHadNext = false;
    }
}
