/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator;

import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TrackedEye.TrackedCapture;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.CachedThrow;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.SolveResult;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model.CapturePolicy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TriangulatorTracker {
    private static final float SEARCH_RANGE = 196.0f;
    private static final double CAPTURE_DEVIATION_LIMIT = 0.35;
    private static final double CAPTURE_QUALITY_FLOOR = 0.55;
    private static final long MISSING_EYE_TIMEOUT_MS = 600L;

    private final Map<Integer, TrackedEye> activeEyes = new LinkedHashMap<>();
    private final List<CachedThrow> cachedThrows = new ArrayList<>();
    private long nextCaptureSerial = 1L;
    private long lastCaptureMs = 0L;
    private long lastWorldTime = Long.MIN_VALUE;
    private long resetMs = 0L;
    private String resetReason = "";

    public void dropEyes() {
        activeEyes.clear();
    }

    public void clear(String reason, boolean rememberReason, long now) {
        activeEyes.clear();
        cachedThrows.clear();
        nextCaptureSerial = 1L;
        lastCaptureMs = 0L;
        if (rememberReason) {
            resetReason = reason;
            resetMs = now;
        } else {
            resetReason = "";
            resetMs = 0L;
        }
    }

    public boolean trackingEye() {
        return !activeEyes.isEmpty();
    }

    public List<CachedThrow> cachedThrows() {
        return java.util.Collections.unmodifiableList(cachedThrows);
    }

    public int throwsCount() {
        return cachedThrows.size();
    }

    public String resetReason() {
        return resetReason;
    }

    public long resetMs() {
        return resetMs;
    }

    private boolean cacheExpired(long now, long lifetimeMs, SolveResult lastResult) {
        return lifetimeMs > 0L
                && !cachedThrows.isEmpty()
                && activeEyes.isEmpty()
                && (lastResult == null || !lastResult.hasSolution())
                && lastCaptureMs > 0L
                && now - lastCaptureMs > lifetimeMs;
    }

    public SolveResult tick(Level level, Player player, CapturePolicy policy, SolveResult lastResult) {
        long now = System.currentTimeMillis();
        long worldTime = level.getGameTime();
        if (lastWorldTime != Long.MIN_VALUE && worldTime + 200L < lastWorldTime) {
            activeEyes.clear();
        }
        lastWorldTime = worldTime;
        if (cacheExpired(now, policy.cacheLifetimeMs(), lastResult)) {
            clear("Cache expired", true, now);
        }

        if (level.dimension() != Level.OVERWORLD) {
            cleanupMissingEyes(Set.of(), now, policy, lastResult);
            return solve(policy);
        }

        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RANGE);
        List<EyeOfEnder> eyes = level.getEntitiesOfClass(
                EyeOfEnder.class,
                searchBox,
                entity -> entity != null && entity.isAlive()
        );

        Set<Integer> seen = new HashSet<>();
        for (EyeOfEnder eye : eyes) {
            seen.add(eye.getId());
            TrackedEye trackedEye = activeEyes.computeIfAbsent(eye.getId(), TrackedEye::new);
            trackedEye.observe(eye, now);

            if (!trackedEye.captured()) {
                TrackedCapture capture = trackedEye.tryCapture(
                        policy.minObservationTicks(),
                        policy.stabilityThreshold(),
                        CAPTURE_DEVIATION_LIMIT
                );
                if (capture != null) {
                    trackedEye.markCaptured();
                    accept(capture, now, policy, lastResult);
                }
            }
        }

        cleanupMissingEyes(seen, now, policy, lastResult);
        return solve(policy);
    }

    private SolveResult solve(CapturePolicy policy) {
        return TriangulatorSolver.solve(cachedThrows, policy.minPairAngleDeg());
    }

    private void cleanupMissingEyes(Set<Integer> seen, long now, CapturePolicy policy, SolveResult lastResult) {
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, TrackedEye> entry : activeEyes.entrySet()) {
            TrackedEye trackedEye = entry.getValue();
            if (seen.contains(entry.getKey())) {
                continue;
            }
            if (!trackedEye.captured()) {
                TrackedCapture capture = trackedEye.tryCapture(
                        policy.minObservationTicks(),
                        policy.stabilityThreshold() - 0.01,
                        CAPTURE_DEVIATION_LIMIT * 1.25
                );
                if (capture != null) {
                    trackedEye.markCaptured();
                    accept(capture, now, policy, lastResult);
                }
            }
            if (trackedEye.captured() || now - trackedEye.lastSeenMs() > MISSING_EYE_TIMEOUT_MS) {
                toRemove.add(entry.getKey());
            }
        }
        for (Integer id : toRemove) {
            activeEyes.remove(id);
        }
    }

    private void accept(TrackedCapture capture, long now, CapturePolicy policy, SolveResult lastResult) {
        if (capture.quality() < CAPTURE_QUALITY_FLOOR) {
            return;
        }

        CachedThrow cachedThrow = new CachedThrow(
                nextCaptureSerial++,
                capture.pointX(),
                capture.pointZ(),
                capture.dirX(),
                capture.dirZ(),
                capture.quality(),
                capture.stability(),
                capture.deviation(),
                capture.samples(),
                now
        );

        if (policy.autoReset() && !cachedThrows.isEmpty()) {
            SolveResult existing = lastResult != null ? lastResult : solve(policy);
            if (existing != null && existing.ready()
                    && TriangulatorSolver.distanceToLine(existing.x(), existing.z(), cachedThrow) > policy.autoResetDistance()) {
                clear("Different stronghold", true, now);
            }
        }

        cachedThrows.add(cachedThrow);
        while (cachedThrows.size() > policy.maxCachedThrows()) {
            cachedThrows.remove(0);
        }
        lastCaptureMs = now;
        resetReason = "";
    }
}
