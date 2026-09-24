package dev.loki.lovisual.util.physics;

/**
 * Чистая физика подвесной "игрушки"-компаньона (идея Soup Trinket): шарик на
 * невидимой пружине у ног игрока, падает под гравитацией, отскакивает от
 * условной земли (offsetY == 0 — уровень ног владельца) и притягивается
 * обратно, если укатился дальше {@code tetherRadius}. Все координаты — смещение
 * от игрока, не мировые. Без Minecraft-типов, покрыто тестами.
 */
public final class TrinketPhysics {

    private static final double SPRING_STRENGTH = 6.0;

    private double x, y, z;
    private double vx, vy, vz;

    public void reset() {
        x = y = z = 0.0;
        vx = vy = vz = 0.0;
    }

    /**
     * Один шаг симуляции.
     *
     * @return true, если в этом шаге произошёл отскок от земли (offsetY достиг 0 сверху)
     */
    public boolean tick(double dt, double gravity, double weight, double friction,
                        double bounce, double groundFriction, double tetherRadius) {
        vy -= gravity * weight * dt;

        x += vx * dt;
        y += vy * dt;
        z += vz * dt;

        boolean bounced = false;
        if (y <= 0.0 && vy < 0.0) {
            y = 0.0;
            vy = -vy * bounce;
            vx *= groundFriction;
            vz *= groundFriction;
            bounced = true;
        }

        double horizontalDist = Math.sqrt(x * x + z * z);
        if (horizontalDist > tetherRadius && horizontalDist > 1.0e-6) {
            double pull = (horizontalDist - tetherRadius) * SPRING_STRENGTH * dt;
            double nx = x / horizontalDist;
            double nz = z / horizontalDist;
            vx -= nx * pull;
            vz -= nz * pull;
        }

        vx *= friction;
        vz *= friction;

        return bounced;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double speed() {
        return Math.sqrt(vx * vx + vy * vy + vz * vz);
    }
}
