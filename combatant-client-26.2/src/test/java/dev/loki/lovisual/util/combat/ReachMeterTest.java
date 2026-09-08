/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.combat;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ReachMeterTest {

    private static final double EPS = 1e-6;

    @BeforeEach
    void reset() {
        ReachMeter.reset();
    }

    @Test
    void distanceToBoxOutsidePoint() {
        AABB box = new AABB(0, 0, 0, 1, 1, 1);

        assertEquals(2.0, ReachMeter.distanceToBox(new Vec3(3, 0.5, 0.5), box), EPS);
        assertEquals(Math.sqrt(3.0), ReachMeter.distanceToBox(new Vec3(2, 2, 2), box), EPS);
    }

    @Test
    void distanceToBoxInsideIsZero() {
        AABB box = new AABB(0, 0, 0, 1, 1, 1);

        assertEquals(0.0, ReachMeter.distanceToBox(new Vec3(0.5, 0.5, 0.5), box), EPS);
        assertEquals(0.0, ReachMeter.distanceToBox(new Vec3(0, 1, 0.5), box), EPS);
    }

    @Test
    void distanceToBoxHandlesNulls() {
        assertEquals(-1.0, ReachMeter.distanceToBox(null, new AABB(0, 0, 0, 1, 1, 1)), EPS);
        assertEquals(-1.0, ReachMeter.distanceToBox(Vec3.ZERO, null), EPS);
    }

    @Test
    void recordTracksLastAndMax() {
        ReachMeter.record(2.5, 1000L);
        ReachMeter.record(3.4, 2000L);
        ReachMeter.record(1.2, 3000L);

        assertEquals(1.2, ReachMeter.lastDistance(), EPS);
        assertEquals(3.4, ReachMeter.sessionMax(), EPS);
        assertEquals(3000L, ReachMeter.lastAttackAtMs());
    }

    @Test
    void invalidDistancesIgnored() {
        ReachMeter.record(2.5, 1000L);
        ReachMeter.record(Double.NaN, 2000L);
        ReachMeter.record(-5.0, 3000L);

        assertEquals(2.5, ReachMeter.lastDistance(), EPS);
        assertEquals(1000L, ReachMeter.lastAttackAtMs());
    }

    @Test
    void resetClearsState() {
        ReachMeter.record(4.0, 1000L);
        ReachMeter.reset();

        assertEquals(-1.0, ReachMeter.lastDistance(), EPS);
        assertEquals(-1.0, ReachMeter.sessionMax(), EPS);
        assertEquals(0L, ReachMeter.lastAttackAtMs());
    }

    @Test
    void formatMeters() {
        assertEquals("-", ReachMeter.format(-1.0));
        assertEquals("3.00 m", ReachMeter.format(3.0));
        assertEquals("3.12 m", ReachMeter.format(3.124));
    }
}
