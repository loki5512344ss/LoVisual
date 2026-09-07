/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator;

import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

/**
 * Pure triangulation math and formatting: weighted pairwise line
 * intersections of cached throws plus the user-facing status strings.
 */
public final class TriangulatorSolver {
    private static final double MIN_SOLVABLE_ANGLE_DEG = 0.05;
    private static final int COPY_COORD_Y = 64;

    public static final List<CachedThrow> PREVIEW_THROWS = List.of(
            new CachedThrow(1L, 110.0, 58.0, 0.9238795, -0.3826834, 0.91, 0.996, 0.08, 8, 0L),
            new CachedThrow(2L, 188.0, -24.0, 0.7071067, -0.7071067, 0.87, 0.992, 0.12, 7, 0L),
            new CachedThrow(3L, 254.0, -102.0, 0.4713967, -0.8819212, 0.82, 0.989, 0.19, 6, 0L)
    );

    private TriangulatorSolver() {
    }

    public static SolveResult previewResult() {
        return solve(PREVIEW_THROWS, 6.0);
    }

    public static SolveResult solve(List<CachedThrow> throwsToSolve, double minAngleThreshold) {
        if (throwsToSolve == null || throwsToSolve.isEmpty()) {
            return null;
        }
        if (throwsToSolve.size() == 1) {
            CachedThrow only = throwsToSolve.get(0);
            return new SolveResult(
                    false,
                    false,
                    only.pointX(),
                    only.pointZ(),
                    0.0,
                    0.0,
                    only.quality(),
                    Mth.floor(only.pointX() / 16.0),
                    Mth.floor(only.pointZ() / 16.0)
            );
        }

        double qualitySum = 0.0;
        double strongestAngle = 0.0;
        double pairX = 0.0;
        double pairZ = 0.0;
        double pairWeightSum = 0.0;
        double crossThreshold = Math.sin(Math.toRadians(MIN_SOLVABLE_ANGLE_DEG));

        for (int i = 0; i < throwsToSolve.size(); i++) {
            CachedThrow throwI = throwsToSolve.get(i);
            qualitySum += throwI.quality();

            for (int j = i + 1; j < throwsToSolve.size(); j++) {
                CachedThrow throwJ = throwsToSolve.get(j);
                double dot = Math.abs(throwI.dirX() * throwJ.dirX() + throwI.dirZ() * throwJ.dirZ());
                double angle = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));
                strongestAngle = Math.max(strongestAngle, angle);

                double cross = throwI.dirX() * throwJ.dirZ() - throwI.dirZ() * throwJ.dirX();
                if (Math.abs(cross) <= crossThreshold) {
                    continue;
                }

                double deltaX = throwJ.pointX() - throwI.pointX();
                double deltaZ = throwJ.pointZ() - throwI.pointZ();
                double t = (deltaX * throwJ.dirZ() - deltaZ * throwJ.dirX()) / cross;
                double intersectionX = throwI.pointX() + throwI.dirX() * t;
                double intersectionZ = throwI.pointZ() + throwI.dirZ() * t;
                double pairWeight = (0.35 + throwI.quality() * 0.65)
                        * (0.35 + throwJ.quality() * 0.65)
                        * Mth.clamp(0.35 + angle / Math.max(0.35, minAngleThreshold), 0.35, 4.0);

                pairX += intersectionX * pairWeight;
                pairZ += intersectionZ * pairWeight;
                pairWeightSum += pairWeight;
            }
        }

        if (pairWeightSum <= 1.0e-8) {
            return new SolveResult(false, false, 0.0, 0.0, Double.POSITIVE_INFINITY, strongestAngle, 0.0, 0, 0);
        }

        double x = pairX / pairWeightSum;
        double z = pairZ / pairWeightSum;
        double errorSum = 0.0;
        for (CachedThrow cachedThrow : throwsToSolve) {
            double d = distanceToLine(x, z, cachedThrow);
            errorSum += d * d;
        }
        double rmse = Math.sqrt(errorSum / throwsToSolve.size());
        double quality = qualitySum / throwsToSolve.size();
        double effectiveAngleThreshold = Math.max(0.35, Math.min(minAngleThreshold, 2.0));
        double angleScore = Mth.clamp(
                (strongestAngle - effectiveAngleThreshold) / Math.max(0.5, 12.0 - effectiveAngleThreshold),
                0.0,
                1.0
        );
        double errorScore = Mth.clamp(1.0 - rmse / 64.0, 0.0, 1.0);
        double countBonus = Mth.clamp((throwsToSolve.size() - 1.0) / 3.0, 0.0, 1.0);
        double confidence = Mth.clamp(
                quality * 0.45 + angleScore * 0.25 + errorScore * 0.20 + countBonus * 0.10,
                0.0,
                1.0
        );
        boolean ready = strongestAngle >= effectiveAngleThreshold && rmse <= 96.0 && confidence >= 0.55;
        return new SolveResult(
                true,
                ready,
                x,
                z,
                rmse,
                strongestAngle,
                confidence,
                Mth.floor(x / 16.0),
                Mth.floor(z / 16.0)
        );
    }

    public static double distanceToLine(double x, double z, CachedThrow cachedThrow) {
        double nx = -cachedThrow.dirZ();
        double nz = cachedThrow.dirX();
        return Math.abs((x - cachedThrow.pointX()) * nx + (z - cachedThrow.pointZ()) * nz);
    }

    public static String formatPercent(double value) {
        return Math.round(Mth.clamp(value, 0.0, 1.0) * 100.0) + "%";
    }

    public static String formatThrowPoint(CachedThrow cachedThrow) {
        return String.format(Locale.ROOT, "%d %d",
                Math.round(cachedThrow.pointX()),
                Math.round(cachedThrow.pointZ()));
    }

    public static String formatCopyCoordinates(SolveResult result) {
        if (result == null || !result.hasSolution()) {
            return "";
        }
        return String.format(Locale.ROOT, "%d %d %d", Math.round(result.x()), COPY_COORD_Y, Math.round(result.z()));
    }

    public static String formatCoordinateLine(double overworldX, double overworldZ, boolean nether) {
        if (nether) {
            return String.format(Locale.ROOT, "N %d  %d", Math.round(overworldX / 8.0), Math.round(overworldZ / 8.0));
        }
        return String.format(Locale.ROOT, "X %d  Z %d", Math.round(overworldX), Math.round(overworldZ));
    }

    public static String formatResultLine(double overworldX, double overworldZ, double rmse, boolean nether) {
        if (nether) {
            return String.format(Locale.ROOT, "OW %d %d  Err %.1fb",
                    Math.round(overworldX), Math.round(overworldZ), rmse);
        }
        return String.format(Locale.ROOT, "Err %.1fb  Chunk %d %d",
                rmse, Mth.floor(overworldX / 16.0), Mth.floor(overworldZ / 16.0));
    }

    public record CachedThrow(long serial,
                              double pointX,
                              double pointZ,
                              double dirX,
                              double dirZ,
                              double quality,
                              double stability,
                              double deviation,
                              int samples,
                              long capturedAtMs) {
    }

    public record SolveResult(boolean hasSolution,
                              boolean ready,
                              double x,
                              double z,
                              double rmse,
                              double minPairAngleDeg,
                              double confidence,
                              int chunkX,
                              int chunkZ) {
    }
}
