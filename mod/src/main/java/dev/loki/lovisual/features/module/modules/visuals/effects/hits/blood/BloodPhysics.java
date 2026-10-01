package dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood;

import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Drop;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.blood.BloodParticles.Splat;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/** Drop physics: ballistic flight, wall bounces, floor splats and secondary droplets. */
final class BloodPhysics {
    static final int MAX_FLIGHT_TICKS = 120;
    private static final double GRAVITY = 0.045;
    private static final double DRAG = 0.985;

    private BloodPhysics() {
    }

    record Hit(Vec3 at, int stepX, int stepY, int stepZ) {
    }

    /** World access, kept abstract so the physics stays testable. */
    interface Surface {
        /** First solid block hit on the segment, or null. */
        Hit cast(Vec3 from, Vec3 to);

        /** Brightness multiplier in [0.3, 1]. */
        float light(Vec3 pos);
    }

    /** Shape of the initial blood eruption. */
    enum Style {
        BURST, FOUNTAIN, RING, SPRAY;

        static Style of(String name) {
            for (Style s : values()) {
                if (s.name().equalsIgnoreCase(name)) return s;
            }
            return BURST;
        }
    }

    /**
     * @param away horizontal unit vector pointing from the attacker to the victim, used by SPRAY
     */
    static void burst(List<Drop> out, Vec3 center, float width, float height, int count, float power,
                      float scale, int color, Style style, Vec3 away, Random rnd) {
        for (int i = 0; i < count; i++) {
            Vec3 origin = center.add((rnd.nextDouble() - 0.5) * width * 0.6,
                    (rnd.nextDouble() - 0.5) * height * 0.5, (rnd.nextDouble() - 0.5) * width * 0.6);
            float size = (float) (0.025 + Math.pow(rnd.nextDouble(), 2.2) * 0.075) * scale;
            int shade = BloodGeometry.shade(color, 0.82f + rnd.nextFloat() * 0.3f);
            out.add(new Drop(origin, velocity(style, power, away, rnd), size, shade, scale, false));
        }
    }

    private static Vec3 velocity(Style style, double power, Vec3 away, Random rnd) {
        double angle = rnd.nextDouble() * Math.PI * 2.0;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return switch (style) {
            case FOUNTAIN -> {
                double h = (0.02 + rnd.nextDouble() * 0.07) * power;
                yield new Vec3(cos * h, (0.25 + rnd.nextDouble() * 0.35) * power * 1.6, sin * h);
            }
            case RING -> {
                double h = (0.18 + rnd.nextDouble() * 0.12) * power;
                yield new Vec3(cos * h, (0.04 + rnd.nextDouble() * 0.05) * power, sin * h);
            }
            case SPRAY -> {
                double forward = (0.12 + rnd.nextDouble() * 0.25) * power;
                double side = (rnd.nextDouble() - 0.5) * 0.35 * power;
                yield new Vec3(away.x * forward - away.z * side, (0.05 + rnd.nextDouble() * 0.2) * power,
                        away.z * forward + away.x * side);
            }
            default -> {
                double h = (0.05 + rnd.nextDouble() * 0.24) * power;
                yield new Vec3(cos * h, (0.06 + rnd.nextDouble() * 0.34) * power, sin * h);
            }
        };
    }

    /** Advances one tick; returns false once the drop is spent (landed or expired). */
    static boolean step(Drop d, Surface surface, List<Splat> splats, List<Drop> children, long nowMs, Random rnd) {
        d.prev = d.pos;
        if (++d.age > MAX_FLIGHT_TICKS) return false;

        Vec3 vel = new Vec3(d.vel.x * DRAG, (d.vel.y - GRAVITY) * DRAG, d.vel.z * DRAG);
        Vec3 next = d.pos.add(vel);
        Hit hit = surface.cast(d.pos, next);
        if (hit == null) {
            d.pos = next;
            d.vel = vel;
            if ((d.age & 3) == 0) d.light = surface.light(next);
            return true;
        }
        if (hit.stepY() > 0) {
            land(d, hit.at(), vel, surface, splats, children, nowMs, rnd);
            return false;
        }
        d.vel = bounce(hit, vel);
        d.pos = hit.at().add(hit.stepX() * 0.01, hit.stepY() * 0.01, hit.stepZ() * 0.01);
        return true;
    }

    private static Vec3 bounce(Hit hit, Vec3 vel) {
        if (hit.stepY() < 0) {
            return new Vec3(vel.x * 0.4, -Math.abs(vel.y) * 0.1, vel.z * 0.4);
        }
        double vx = hit.stepX() != 0 ? -vel.x * 0.12 : vel.x * 0.3;
        double vz = hit.stepZ() != 0 ? -vel.z * 0.12 : vel.z * 0.3;
        return new Vec3(vx, Math.min(vel.y, 0.0) * 0.5, vz);
    }

    private static void land(Drop d, Vec3 at, Vec3 vel, Surface surface, List<Splat> splats,
                             List<Drop> children, long nowMs, Random rnd) {
        float speed = (float) vel.length();
        float radius = d.size * (2.2f + Math.min(speed, 0.8f) * 4.0f);
        splats.add(new Splat(at, radius, d.color, nowMs, rnd.nextLong(), surface.light(at), rnd.nextInt(40)));
        if (d.child || d.size <= 0.055f * d.scale || speed <= 0.25f) return;
        int n = 2 + rnd.nextInt(3);
        for (int k = 0; k < n; k++) {
            double a = rnd.nextDouble() * Math.PI * 2.0;
            double h = 0.03 + rnd.nextDouble() * 0.07;
            Vec3 v = new Vec3(Math.cos(a) * h + vel.x * 0.25, 0.05 + rnd.nextDouble() * 0.08, Math.sin(a) * h + vel.z * 0.25);
            children.add(new Drop(at.add(0, 0.02, 0), v, d.size * 0.35f, d.color, d.scale, true));
        }
    }
}
