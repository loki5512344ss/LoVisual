package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails.pose;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Спецификация концов крыльев (как в evo): они считаются по настоящей модели элитры на теле игрока, поэтому
 * симметричны, следуют за рысканием, масштабом и взмахами крыльев и лежат вплотную к игроку.
 */
class WingTipLocatorTest {

    private static final double EPS = 1.0e-4;

    /** Анимация крыльев при планировании по горизонтали (ElytraModel): крылья широко раскинуты. */
    private static final float[] SPREAD = {0.349f, 0f, -1.5708f};

    private static WingTipLocator.Pose spread(float yaw, float xRot, float ticks) {
        return new WingTipLocator.Pose(10, 70, -3, yaw, xRot, 1f, false, false, ticks, 0f, 0f, SPREAD[0], SPREAD[1], SPREAD[2]);
    }

    private static WingTipLocator.Pose pose(float yaw) {
        return new WingTipLocator.Pose(10, 70, -3, yaw, 0f, 1f, false, false, 100f, 0f, 0f, 0f, 0f, 0f);
    }

    private static Vec3 rel(Vec3 tip, WingTipLocator.Pose p) {
        return tip.subtract(p.x(), p.y(), p.z());
    }

    /** Поворот вокруг вертикали так, что направление +Z переходит в направление взгляда при рыскании yaw. */
    private static Vec3 turn(Vec3 v, float yawDeg) {
        double a = Math.toRadians(yawDeg);
        return new Vec3(v.x * Math.cos(a) - v.z * Math.sin(a), v.y, v.x * Math.sin(a) + v.z * Math.cos(a));
    }

    // ---- форма

    @Test
    void tipsAreMirrorImagesAcrossThePlaneOfTheBodyWithRelaxedWings() {
        WingTipLocator.Tips t = WingTipLocator.tips(pose(0f));
        Vec3 l = rel(t.left(), pose(0f)), r = rel(t.right(), pose(0f));
        assertEquals(l.x, -r.x, EPS);
        assertEquals(l.y, r.y, EPS);
        assertEquals(l.z, r.z, EPS);
    }

    @Test
    void withSpreadWingsTheLeftTipIsOnThePlayersLeftHandSide() {
        // Facing +Z (yaw 0) the player's left is +X; facing -X (yaw 90) it is +Z.
        WingTipLocator.Tips south = WingTipLocator.tips(spread(0f, 0f, 100f));
        assertTrue(south.left().x > south.right().x);
        WingTipLocator.Tips west = WingTipLocator.tips(spread(90f, 0f, 100f));
        assertTrue(west.left().z > west.right().z);
    }

    @Test
    void spreadWingsReachAboutAMeterAndAHalfToEachSideAndStayCloseToThePlayer() {
        for (float ticks : new float[]{0f, 5f, 100f}) {
            for (float xRot : new float[]{-30f, 0f, 60f, 90f}) {
                WingTipLocator.Pose p = spread(40f, xRot, ticks);
                WingTipLocator.Tips t = WingTipLocator.tips(p);
                double span = t.left().distanceTo(t.right());
                assertTrue(span > 2.2 && span < 3.2, "span " + span);
                Vec3 pos = new Vec3(p.x(), p.y(), p.z());
                assertTrue(t.left().distanceTo(pos) < 3.0 && t.right().distanceTo(pos) < 3.0);
            }
        }
    }

    @Test
    void standingWingsHangBehindTheBack() {
        for (float yaw : new float[]{0f, 63f, -140f, 270f}) {
            WingTipLocator.Pose p = new WingTipLocator.Pose(0, 0, 0, yaw, 0f, 1f, false, false, 0f, 0f, 0f, 0f, 0f, 0f);
            Vec3 facing = turn(new Vec3(0, 0, 1), yaw);
            WingTipLocator.Tips t = WingTipLocator.tips(p);
            assertTrue(t.left().dot(facing) < 0 && t.right().dot(facing) < 0, "yaw " + yaw);
        }
    }

    // ---- следование за телом

    @Test
    void tipsTurnWithTheBodyAroundTheVerticalThroughThePlayer() {
        Vec3 base = rel(WingTipLocator.tips(pose(0f)).left(), pose(0f));
        for (float yaw : new float[]{30f, 90f, -75f, 180f, 250f}) {
            Vec3 expected = turn(base, yaw);
            Vec3 actual = rel(WingTipLocator.tips(pose(yaw)).left(), pose(yaw));
            assertEquals(expected.x, actual.x, EPS);
            assertEquals(expected.y, actual.y, EPS);
            assertEquals(expected.z, actual.z, EPS);
        }
    }

    @Test
    void tipsFollowThePlayerWhenItMoves() {
        WingTipLocator.Pose a = pose(33f);
        WingTipLocator.Pose b = new WingTipLocator.Pose(17, 68, 4, 33f, 0f, 1f, false, false, 100f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(7.0, WingTipLocator.tips(b).left().x - WingTipLocator.tips(a).left().x, EPS);
        assertEquals(-2.0, WingTipLocator.tips(b).right().y - WingTipLocator.tips(a).right().y, EPS);
        assertEquals(7.0, WingTipLocator.tips(b).right().z - WingTipLocator.tips(a).right().z, EPS);
    }

    @Test
    void aBiggerPlayerHasProportionallyBiggerWings() {
        WingTipLocator.Pose one = new WingTipLocator.Pose(0, 0, 0, 20f, 10f, 1f, false, false, 100f, 0f, 0f, 0f, 0f, 0f);
        WingTipLocator.Pose two = new WingTipLocator.Pose(0, 0, 0, 20f, 10f, 2f, false, false, 100f, 0f, 0f, 0f, 0f, 0f);
        Vec3 a = WingTipLocator.tips(one).left(), b = WingTipLocator.tips(two).left();
        assertEquals(2 * a.x, b.x, EPS);
        assertEquals(2 * a.y, b.y, EPS);
        assertEquals(2 * a.z, b.z, EPS);
    }

    // ---- полёт

    @Test
    void whileGlidingTheBodyLiesForwardSoTheWingsMoveFromBehindTheBackToAhead() {
        double behind = WingTipLocator.tips(spread(0f, 0f, 0f)).left().z + 3;
        double ahead = WingTipLocator.tips(spread(0f, 0f, 100f)).left().z + 3;
        assertTrue(behind < 0.0, "standing: wings behind the back");
        assertTrue(ahead > behind + 1.0, "gliding: the body lies forward");
    }

    @Test
    void fallFlyingEasesInOverTheFirstTicks() {
        assertEquals(0f, WingTipLocator.fallFlyingScale(0f), 0f);
        assertEquals(0.25f, WingTipLocator.fallFlyingScale(5f), 1e-6f);
        assertEquals(1f, WingTipLocator.fallFlyingScale(10f), 1e-6f);
        assertEquals(1f, WingTipLocator.fallFlyingScale(400f), 0f);
        assertTrue(WingTipLocator.fallFlyingScale(6f) > WingTipLocator.fallFlyingScale(4f));
    }

    @Test
    void steeringTurnsTheBodyAlongTheFlightPath() {
        // Heading east while looking north: the body banks round by a quarter turn.
        assertEquals(0f, WingTipLocator.flyingYRot(1, 0, 1, 0), 1e-6f);
        assertEquals(0f, WingTipLocator.flyingYRot(0, 0, 1, 0), 0f, "no movement, no turn");
        assertEquals(0f, WingTipLocator.flyingYRot(1, 0, 0, 0), 0f, "looking straight up or down, no turn");
        float quarter = WingTipLocator.flyingYRot(1, 0, 0, 1);
        assertEquals(Math.PI / 2, Math.abs(quarter), 1e-5);
        assertEquals(-quarter, WingTipLocator.flyingYRot(-1, 0, 0, 1), 1e-5f, "opposite side, opposite sign");
        for (double a = 0; a < 6.3; a += 0.4) {
            assertTrue(Math.abs(WingTipLocator.flyingYRot(Math.cos(a), Math.sin(a), 1, 0)) <= Math.PI / 2 + 1e-6);
        }
    }

    @Test
    void spinAttackSpinsTheBodyButKeepsTheWingsAtTheSameDistanceFromThePlayer() {
        Vec3 first = null;
        double dist0 = Double.NaN;
        boolean moved = false;
        for (float time : new float[]{0f, 3f, 7.5f, 21f}) {
            WingTipLocator.Pose p = new WingTipLocator.Pose(0, 0, 0, 0f, 20f, 1f, false, true, 100f, 0f, time,
                    SPREAD[0], SPREAD[1], SPREAD[2]);
            Vec3 tip = WingTipLocator.tips(p).left();
            if (first == null) {
                first = tip;
                dist0 = tip.length();
            }
            assertEquals(dist0, tip.length(), EPS);
            moved |= tip.distanceTo(first) > 0.5;
        }
        assertTrue(moved, "the wings circle while spinning");
    }

    // ---- крылья и поза

    @Test
    void flappingMovesBothWingsAsMirrorImages() {
        WingTipLocator.Pose p = new WingTipLocator.Pose(0, 0, 0, 0f, 0f, 1f, false, false, 100f, 0f, 0f, 0.3f, 0.2f, 0.5f);
        WingTipLocator.Tips t = WingTipLocator.tips(p);
        assertEquals(t.left().x, -t.right().x, EPS);
        assertEquals(t.left().y, t.right().y, EPS);
        assertEquals(t.left().z, t.right().z, EPS);
        WingTipLocator.Pose still = new WingTipLocator.Pose(0, 0, 0, 0f, 0f, 1f, false, false, 100f, 0f, 0f, 0f, 0f, 0f);
        assertTrue(t.left().distanceTo(WingTipLocator.tips(still).left()) > 0.2, "flapping moves the tip");
    }

    @Test
    void sneakingShiftsBothWingsTogetherByLessThanAQuarterBlock() {
        WingTipLocator.Pose up = new WingTipLocator.Pose(0, 0, 0, 25f, 0f, 1f, false, false, 0f, 0f, 0f, 0f, 0f, 0f);
        WingTipLocator.Pose down = new WingTipLocator.Pose(0, 0, 0, 25f, 0f, 1f, true, false, 0f, 0f, 0f, 0f, 0f, 0f);
        Vec3 dl = WingTipLocator.tips(down).left().subtract(WingTipLocator.tips(up).left());
        Vec3 dr = WingTipLocator.tips(down).right().subtract(WingTipLocator.tips(up).right());
        assertTrue(dl.length() > 0.01 && dl.length() < 0.25);
        assertEquals(dl.x, dr.x, EPS);
        assertEquals(dl.y, dr.y, EPS);
        assertEquals(dl.z, dr.z, EPS);
    }
}
