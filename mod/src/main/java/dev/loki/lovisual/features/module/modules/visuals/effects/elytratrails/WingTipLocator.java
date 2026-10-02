package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import net.minecraft.world.phys.Vec3;

/**
 * Концы крыльев планирующего игрока. Точка привязки лежит позади головы вдоль направления полёта,
 * размах крыльев перпендикулярен ему и всегда горизонтален (без крена). Чистая математика.
 */
public final class WingTipLocator {

    /** Половина размаха: расстояние от оси тела до конца крыла, блоки. */
    static final float HALF_SPAN = 1.0f;
    /** Корни крыльев лежат позади глаз на этом расстоянии вдоль направления полёта. */
    static final float BACK = 0.3f;
    /** Смещение по «верху» тела относительно глаз (корни крыльев чуть ниже глаз). */
    static final float HEIGHT = -0.15f;

    private WingTipLocator() {
    }

    public record Tips(Vec3 left, Vec3 right) {
    }

    /**
     * @param eye      позиция глаз (интерполированная)
     * @param yawDeg   рыскание в градусах (0 смотрит на +Z)
     * @param pitchDeg наклон в градусах, положительный вниз
     */
    public static Tips tips(Vec3 eye, float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        // Правая рука игрока зависит только от рыскания, поэтому пара не вырождается при взгляде вверх и вниз.
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 up = right.cross(forward);
        Vec3 anchor = eye.subtract(forward.scale(BACK)).add(up.scale(HEIGHT));
        return new Tips(anchor.subtract(right.scale(HALF_SPAN)), anchor.add(right.scale(HALF_SPAN)));
    }
}
