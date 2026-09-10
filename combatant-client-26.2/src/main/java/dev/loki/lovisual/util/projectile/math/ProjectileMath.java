/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.projectile.math;

public enum ProjectileMath {
    ;

    public static MinimumResult findFunctionMinimumByBisect(double from, double to, double minDelta, DoubleUnaryFunction function) {

        double lowerBound = from;
        double upperBound = to;

        while (upperBound - lowerBound > minDelta) {
            double mid = (lowerBound + upperBound) / 2.0;
            double leftValue = function.apply((lowerBound + mid) / 2.0);
            double rightValue = function.apply((mid + upperBound) / 2.0);

            if (leftValue < rightValue) {
                upperBound = mid;
            } else {
                lowerBound = mid;
            }
        }

        double x = (lowerBound + upperBound) / 2.0;
        double y = function.apply(x);
        return new MinimumResult(x, y);
    }

    @FunctionalInterface
    public interface DoubleUnaryFunction {

        double apply(double value);
    }

    public record MinimumResult(double x, double y) {

    }
}
