package dev.loki.lovisual.features.module.modules.combat.targetesp.sword;

import java.util.Random;

/**
 * Spring physics of the "swords" TargetESP mode: every sword is pulled towards its slot on an
 * orbit around the target, gets kicked outwards when the target is hurt, and leans into its
 * motion. Pure math, no Minecraft types.
 */
final class SwordPhysics {
    static final int COUNT = 8;
    private static final double SPRING = 0.22;
    private static final double DAMPING = 0.78;

    private SwordPhysics() {
    }

    static final class Sword {
        double x, y, z;
        double vx, vy, vz;
        float roll;
        float pitch;
    }

    /** Frame inputs; angles in radians, {@code step} = frame time in 1/60 s units clamped to [0.2, 2]. */
    record Frame(double targetX, double targetZ, double centerY, double radius, double orbitAngle,
                 double timeMs, double bobAmplitude, float step, boolean kick, boolean physics,
                 double cameraYawRad) {
    }

    /** Places every sword on its orbit slot at rest. */
    static void place(Sword[] swords, double targetX, double targetZ, double centerY, double radius, double orbitAngle) {
        for (int i = 0; i < swords.length; i++) {
            Sword s = swords[i];
            double a = orbitAngle + i * (Math.PI * 2.0 / swords.length);
            s.x = targetX + Math.cos(a) * radius;
            s.y = centerY;
            s.z = targetZ + Math.sin(a) * radius;
            s.vx = 0;
            s.vy = 0;
            s.vz = 0;
            s.roll = 0;
            s.pitch = 0;
        }
    }

    static void step(Sword[] swords, Frame f, Random rnd) {
        for (int i = 0; i < swords.length; i++) {
            Sword s = swords[i];
            double a = f.orbitAngle() + i * (Math.PI * 2.0 / swords.length);
            double bob = Math.sin(f.timeMs() * 0.0035 + i * (Math.PI / 4.0)) * f.bobAmplitude();
            double idealX = f.targetX() + Math.cos(a) * f.radius();
            double idealY = f.centerY() + bob;
            double idealZ = f.targetZ() + Math.sin(a) * f.radius();
            if (f.kick()) kick(s, f, rnd);
            if (!f.physics()) {
                s.x = idealX;
                s.y = idealY;
                s.z = idealZ;
                s.vx = 0;
                s.vy = 0;
                s.vz = 0;
                s.roll = 0;
                s.pitch = 0;
                continue;
            }
            double spring = SPRING * f.step();
            double damping = Math.pow(DAMPING, f.step());
            s.vx = (s.vx + (idealX - s.x) * spring) * damping;
            s.vy = (s.vy + (idealY - s.y) * spring) * damping;
            s.vz = (s.vz + (idealZ - s.z) * spring) * damping;
            s.x += s.vx * f.step();
            s.y += s.vy * f.step();
            s.z += s.vz * f.step();
            lean(s, f);
        }
    }

    private static void kick(Sword s, Frame f, Random rnd) {
        double dx = s.x - f.targetX();
        double dz = s.z - f.targetZ();
        double len = Math.hypot(dx, dz);
        if (len > 0.001) {
            double power = 0.16 + rnd.nextDouble() * 0.06;
            s.vx += dx / len * power;
            s.vz += dz / len * power;
            s.vy += 0.07;
        }
        s.roll += (float) ((rnd.nextDouble() - 0.5) * 28.0);
    }

    /** Roll follows sideways speed as seen from the camera, pitch follows vertical speed. */
    private static void lean(Sword s, Frame f) {
        double sideSpeed = -s.vx * Math.cos(f.cameraYawRad()) - s.vz * Math.sin(f.cameraYawRad());
        float targetRoll = (float) Math.max(-42.0, Math.min(42.0, sideSpeed * 130.0));
        float targetPitch = (float) Math.max(-32.0, Math.min(32.0, -s.vy * 85.0));
        float k = Math.min(1f, 0.25f * f.step());
        s.roll += (targetRoll - s.roll) * k;
        s.pitch += (targetPitch - s.pitch) * k;
    }
}
