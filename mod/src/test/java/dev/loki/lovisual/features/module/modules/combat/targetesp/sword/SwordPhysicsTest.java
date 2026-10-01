package dev.loki.lovisual.features.module.modules.combat.targetesp.sword;

import dev.loki.lovisual.features.module.modules.combat.targetesp.sword.SwordPhysics.Frame;
import dev.loki.lovisual.features.module.modules.combat.targetesp.sword.SwordPhysics.Sword;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SwordPhysicsTest {
    private static Sword[] swords() {
        Sword[] s = new Sword[SwordPhysics.COUNT];
        for (int i = 0; i < s.length; i++) s[i] = new Sword();
        return s;
    }

    private static Frame frame(boolean kick, boolean physics) {
        return new Frame(0, 0, 1, 1.0, 0, 0, 0, 1f, kick, physics, 0);
    }

    @Test
    void placedSwordsSitOnTheOrbit() {
        Sword[] s = swords();
        SwordPhysics.place(s, 5, -3, 1, 2.0, 0.5);
        for (Sword sw : s) assertEquals(2.0, Math.hypot(sw.x - 5, sw.z + 3), 1e-9);
    }

    @Test
    void withoutPhysicsSwordsSnapToTheirSlot() {
        Sword[] s = swords();
        s[0].x = 100;
        SwordPhysics.step(s, frame(false, false), new Random(1));
        assertEquals(1.0, s[0].x, 1e-9);
        assertEquals(0f, s[0].roll);
    }

    @Test
    void springPullsADisplacedSwordBackAndSettles() {
        Sword[] s = swords();
        SwordPhysics.place(s, 0, 0, 1, 1.0, 0);
        s[0].x = 3;
        Random rnd = new Random(2);
        for (int i = 0; i < 300; i++) SwordPhysics.step(s, frame(false, true), rnd);
        assertEquals(1.0, s[0].x, 0.01);
        assertEquals(0.0, s[0].vx, 0.01);
    }

    @Test
    void hitKicksSwordsOutwardsAndUp() {
        Sword[] s = swords();
        SwordPhysics.place(s, 0, 0, 1, 1.0, 0);
        SwordPhysics.step(s, frame(true, true), new Random(3));
        assertTrue(s[0].vx > 0.05);
        assertTrue(s[0].vy > 0.0);
    }
}
