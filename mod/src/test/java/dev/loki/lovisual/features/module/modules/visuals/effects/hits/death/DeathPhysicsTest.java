package dev.loki.lovisual.features.module.modules.visuals.effects.hits.death;

import dev.loki.lovisual.features.module.modules.visuals.effects.hits.death.DeathPhysics.Part;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.death.DeathPhysics.Style;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DeathPhysicsTest {

    private static Part part(float y) {
        return new Part(1, 0, y, 0, 0.25f, 0.375f, 0.125f);
    }

    @Test
    void shatterFallsAndRestsOnGround() {
        Part p = part(1.1f);
        DeathPhysics.launch(p, Style.SHATTER, 1.0f, 1.0f, 2.5f, 0, 1.0f, 0, new Random(1));
        for (int i = 0; i < 400; i++) DeathPhysics.step(p, Style.SHATTER, 0.02f, i * 0.02f, 0.0f, false);
        assertEquals(0.375f, p.y, 1e-4f);
        assertEquals(0.0f, p.vy, 1e-6f);
    }

    @Test
    void bounceKeepsPartAboveFloorAndLosesEnergy() {
        Part p = part(1.5f);
        p.vy = -6.0f;
        float maxAfterFirst = 0;
        boolean bounced = false;
        for (int i = 0; i < 100; i++) {
            DeathPhysics.step(p, Style.SHATTER, 0.02f, i * 0.02f, 0.0f, true);
            assertTrue(p.y >= 0.375f - 1e-4f);
            if (p.vy > 0) bounced = true;
            if (bounced) maxAfterFirst = Math.max(maxAfterFirst, p.y);
        }
        assertTrue(bounced);
        assertTrue(maxAfterFirst < 1.5f);
    }

    @Test
    void ascendRisesAndFadesOut() {
        Part p = part(1.0f);
        DeathPhysics.launch(p, Style.ASCEND, 1.0f, 1.0f, 2.5f, 0, 1.0f, 0, new Random(2));
        for (int i = 0; i < 50; i++) DeathPhysics.step(p, Style.ASCEND, 0.02f, i * 0.02f, 0.0f, false);
        assertTrue(p.y > 1.4f);
        assertEquals(1.0f, DeathPhysics.alpha(p, Style.ASCEND, 0.1f, 2.5f), 1e-6f);
        assertEquals(0.0f, DeathPhysics.alpha(p, Style.ASCEND, 2.5f, 2.5f), 1e-6f);
    }

    @Test
    void disintegrateDissolvesTopFirst() {
        Part head = new Part(0, 0, 1.75f, 0, 0.25f, 0.25f, 0.25f);
        Part leg = new Part(4, 0, 0.375f, 0, 0.125f, 0.375f, 0.125f);
        Random r = new Random(3);
        DeathPhysics.launch(head, Style.DISINTEGRATE, 1f, 1f, 2.5f, 0, 1f, 0, r);
        DeathPhysics.launch(leg, Style.DISINTEGRATE, 1f, 1f, 2.5f, 0, 1f, 0, r);
        assertTrue(head.delay < leg.delay);
        float t = (head.delay + leg.delay) * 0.5f + 0.3f;
        assertTrue(DeathPhysics.alpha(head, Style.DISINTEGRATE, t, 2.5f) < DeathPhysics.alpha(leg, Style.DISINTEGRATE, t, 2.5f));
    }

    @Test
    void explosionLaunchesFasterThanShatter() {
        Part a = part(1.1f), b = part(1.1f);
        a.x = 0.4f;
        b.x = 0.4f;
        DeathPhysics.launch(a, Style.SHATTER, 1f, 1f, 2.5f, 0, 1f, 0, new Random(5));
        DeathPhysics.launch(b, Style.EXPLOSION, 1f, 1f, 2.5f, 0, 1f, 0, new Random(5));
        assertTrue(Math.abs(b.vx) > Math.abs(a.vx));
    }

    @Test
    void bodyHasSixPartsAroundFeet() {
        var parts = DeathBody.build(10, 64, -5, 0, 1.8f);
        assertEquals(6, parts.size());
        assertEquals(64 + 1.75f, parts.get(0).y, 1e-4f);
        assertTrue(parts.get(2).x < parts.get(3).x);
    }
}
