package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация концов крыльев элитры: где они должны быть относительно планирующего игрока. */
class WingTipLocatorTest {

    private static final double EPS = 1.0e-6;
    private static final float[][] POSES = {{0, 0}, {37, -12}, {-120, 30}, {90, 55}, {200, -70}, {45, 89}};

    /** Единичный вектор взгляда по правилам Minecraft: yaw 0 смотрит на +Z, pitch>0 вниз. */
    private static Vec3 view(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    @Test
    void tipsAreOneFullSpanApartInEveryPose() {
        for (float[] pose : POSES) {
            WingTipLocator.Tips t = WingTipLocator.tips(new Vec3(10, 64, -3), pose[0], pose[1]);
            assertEquals(2.0 * WingTipLocator.HALF_SPAN, t.left().distanceTo(t.right()), EPS);
        }
    }

    @Test
    void wingSpanIsPerpendicularToTheFlightDirection() {
        for (float[] pose : POSES) {
            WingTipLocator.Tips t = WingTipLocator.tips(Vec3.ZERO, pose[0], pose[1]);
            assertEquals(0.0, t.left().subtract(t.right()).dot(view(pose[0], pose[1])), EPS);
        }
    }

    @Test
    void wingsStayLevelLikeAnAeroplaneWithoutRoll() {
        for (float[] pose : POSES) {
            WingTipLocator.Tips t = WingTipLocator.tips(new Vec3(0, 70, 0), pose[0], pose[1]);
            assertEquals(t.left().y, t.right().y, EPS);
        }
    }

    @Test
    void leftTipIsOnThePlayersLeftHandSide() {
        // Facing +Z (yaw 0) the player's left is +X; facing -X (yaw 90) it is +Z.
        WingTipLocator.Tips south = WingTipLocator.tips(Vec3.ZERO, 0.0f, 0.0f);
        assertTrue(south.left().x > south.right().x);
        WingTipLocator.Tips west = WingTipLocator.tips(Vec3.ZERO, 90.0f, 0.0f);
        assertTrue(west.left().z > west.right().z);
    }

    @Test
    void tipsAreSymmetricAroundAnAnchorBehindTheHead() {
        Vec3 eye = new Vec3(5, 80, 5);
        for (float[] pose : POSES) {
            WingTipLocator.Tips t = WingTipLocator.tips(eye, pose[0], pose[1]);
            Vec3 mid = t.left().add(t.right()).scale(0.5);
            // The wing roots sit BACK blocks behind the eye along the flight direction.
            assertEquals(-WingTipLocator.BACK, mid.subtract(eye).dot(view(pose[0], pose[1])), EPS);
        }
    }

    @Test
    void tipsFollowTheBodyWhenItMoves() {
        WingTipLocator.Tips a = WingTipLocator.tips(new Vec3(0, 0, 0), 33.0f, -20.0f);
        WingTipLocator.Tips b = WingTipLocator.tips(new Vec3(7, -2, 4), 33.0f, -20.0f);
        assertEquals(7.0, b.left().x - a.left().x, EPS);
        assertEquals(-2.0, b.right().y - a.right().y, EPS);
        assertEquals(4.0, b.right().z - a.right().z, EPS);
    }

    @Test
    void lookingStraightDownOrUpStillGivesAMirroredPair() {
        for (float pitch : new float[]{90.0f, -90.0f}) {
            WingTipLocator.Tips t = WingTipLocator.tips(Vec3.ZERO, 30.0f, pitch);
            assertEquals(2.0 * WingTipLocator.HALF_SPAN, t.left().distanceTo(t.right()), EPS);
            assertFalse(Double.isNaN(t.left().x) || Double.isNaN(t.right().z));
        }
    }
}
