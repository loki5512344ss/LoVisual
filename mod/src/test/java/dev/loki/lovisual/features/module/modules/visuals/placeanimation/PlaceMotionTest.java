package dev.loki.lovisual.features.module.modules.visuals.placeanimation;

import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PlaceMotionTest {

    @Test
    void easeStartsAtZeroAndEndsAtOne() {
        for (float base : new float[]{PlaceMotion.easeBase(0.9f), PlaceMotion.easeBase(-0.08f), PlaceMotion.easeBase(-0.7f)}) {
            assertEquals(0.0f, PlaceMotion.ease(0.0f, base), 1e-5f);
            assertEquals(1.0f, PlaceMotion.ease(1.0f, base), 1e-5f);
        }
    }

    @Test
    void slideRemainingShrinksMonotonicallyToZero() {
        assertEquals(1.0f, PlaceMotion.slideRemaining(0.0f), 1e-5f);
        assertEquals(0.0f, PlaceMotion.slideRemaining(1.0f), 1e-5f);
        float previous = 1.0f;
        for (float p = 0.1f; p <= 1.0f; p += 0.1f) {
            float now = PlaceMotion.slideRemaining(p);
            assertTrue(now <= previous + 1e-6f);
            previous = now;
        }
    }

    @Test
    void tiltSettlesAndScaleLandsJustUnderFull() {
        assertEquals(0.5f, PlaceMotion.tilt(0.0f, 0.5f), 1e-5f);
        assertEquals(0.0f, PlaceMotion.tilt(1.0f, 0.5f), 1e-5f);
        assertEquals(0.4f, PlaceMotion.scale(0.4f, 0.0f), 1e-5f);
        assertEquals(PlaceMotion.FINAL_SCALE, PlaceMotion.scale(0.4f, 1.0f), 1e-5f);
    }

    @Test
    void progressIsCappedAtOne() {
        assertEquals(0.0f, PlaceMotion.progress(0, 0.0f, 3), 1e-6f);
        assertEquals(0.5f, PlaceMotion.progress(1, 1.0f, 3), 1e-6f);
        assertEquals(1.0f, PlaceMotion.progress(10, 0.5f, 3), 1e-6f);
    }

    @Test
    void slideStartPointsBackTowardsThePlayerAndFlipsWithTheHand() {
        // yaw 0 faces +Z, so "back" is -Z
        Vector3f right = PlaceMotion.slideStart(0.0f, 0.0f, true, 1.0f);
        Vector3f left = PlaceMotion.slideStart(0.0f, 0.0f, false, 1.0f);
        assertTrue(right.z < 0.0f);
        assertEquals(right.z, left.z, 1e-6f);
        assertEquals(-right.x, left.x, 1e-6f);
        assertEquals(0.0f, PlaceMotion.slideStart(0.0f, 0.0f, true, 0.0f).length(), 1e-6f);
    }

    @Test
    void slideStartDipsWhenLookingUpAndRisesWhenLookingDown() {
        assertTrue(PlaceMotion.slideStart(0.0f, -30.0f, true, 1.0f).y < 0.0f);
        assertTrue(PlaceMotion.slideStart(0.0f, 30.0f, true, 1.0f).y > 0.0f);
    }

    @Test
    void restrictZeroesOnlyComponentsPushingIntoBlockedSides() {
        Vector3f offset = new Vector3f(1.0f, -1.0f, 1.0f);
        Vector3f out = PlaceMotion.restrict(offset, EnumSet.of(Direction.EAST, Direction.UP));
        assertEquals(0.0f, out.x);
        assertEquals(-1.0f, out.y);
        assertEquals(1.0f, out.z);
        assertEquals(1.0f, offset.x, "input is not modified");
    }
}
