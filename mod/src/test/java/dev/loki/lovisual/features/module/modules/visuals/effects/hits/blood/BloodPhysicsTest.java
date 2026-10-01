package dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood;

import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Drop;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Splat;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BloodPhysicsTest {
    private static final BloodPhysics.Surface FLOOR = new BloodPhysics.Surface() {
        @Override
        public BloodPhysics.Hit cast(Vec3 from, Vec3 to) {
            if (from.y > 0 && to.y <= 0) return new BloodPhysics.Hit(new Vec3(to.x, 0, to.z), 0, 1, 0);
            return null;
        }

        @Override
        public float light(Vec3 pos) {
            return 1.0f;
        }
    };

    @Test
    void burstSpawnsRequestedCountNearCenter() {
        List<Drop> out = new ArrayList<>();
        BloodPhysics.burst(out, new Vec3(0, 1, 0), 0.6f, 1.8f, 40, 1.0f, 1.0f, 0xFF8A0303,
                BloodPhysics.Style.BURST, new Vec3(1, 0, 0), new Random(1));
        assertEquals(40, out.size());
        for (Drop d : out) {
            assertTrue(Math.abs(d.pos.x) <= 0.3 && Math.abs(d.pos.y - 1) <= 0.45);
            assertEquals(0xFF, d.color >>> 24);
        }
    }

    @Test
    void stylesShapeTheEruption() {
        Vec3 away = new Vec3(1, 0, 0);
        for (BloodPhysics.Style style : BloodPhysics.Style.values()) {
            List<Drop> out = new ArrayList<>();
            BloodPhysics.burst(out, Vec3.ZERO, 0.6f, 1.8f, 60, 1.0f, 1.0f, 0xFF8A0303, style, away, new Random(7));
            double upAvg = out.stream().mapToDouble(d -> d.vel.y).average().orElse(0);
            double awayAvg = out.stream().mapToDouble(d -> d.vel.x).average().orElse(0);
            switch (style) {
                case FOUNTAIN -> assertTrue(upAvg > 0.4);
                case RING -> assertTrue(upAvg < 0.1);
                case SPRAY -> assertTrue(awayAvg > 0.1);
                default -> assertTrue(upAvg > 0.1 && upAvg < 0.4);
            }
        }
        assertEquals(BloodPhysics.Style.SPRAY, BloodPhysics.Style.of("spray"));
        assertEquals(BloodPhysics.Style.BURST, BloodPhysics.Style.of("nope"));
    }

    @Test
    void dropFallsAndLeavesSplatOnFloor() {
        Drop d = new Drop(new Vec3(0, 1, 0), new Vec3(0, 0, 0), 0.05f, 0xFF8A0303, 1.0f, false);
        List<Splat> splats = new ArrayList<>();
        List<Drop> children = new ArrayList<>();
        int ticks = 0;
        while (BloodPhysics.step(d, FLOOR, splats, children, 0L, new Random(2))) {
            assertTrue(++ticks < BloodPhysics.MAX_FLIGHT_TICKS);
        }
        assertEquals(1, splats.size());
        assertEquals(0.0, splats.get(0).pos().y, 1e-9);
    }

    @Test
    void childDropsDoNotSplitAgain() {
        Drop d = new Drop(new Vec3(0, 0.1, 0), new Vec3(0, -1, 0), 0.1f, 0xFF8A0303, 1.0f, true);
        List<Drop> children = new ArrayList<>();
        BloodPhysics.step(d, FLOOR, new ArrayList<>(), children, 0L, new Random(3));
        assertTrue(children.isEmpty());
    }

    @Test
    void dropExpiresInOpenAir() {
        Drop d = new Drop(new Vec3(0, 1000, 0), new Vec3(0, 0, 0), 0.05f, 0xFF8A0303, 1.0f, false);
        boolean alive = true;
        for (int i = 0; i <= BloodPhysics.MAX_FLIGHT_TICKS && alive; i++) {
            alive = BloodPhysics.step(d, FLOOR, new ArrayList<>(), new ArrayList<>(), 0L, new Random(4));
        }
        assertFalse(alive);
    }

    @Test
    void shadeKeepsAlphaAndClamps() {
        assertEquals(0x80FF0000, BloodGeometry.shade(0x80800000, 2.0f));
        assertEquals(0xCC, BloodGeometry.withAlpha(0xFF112233, 0xCC) >>> 24);
    }
}
