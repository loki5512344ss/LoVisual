package dev.loki.lovisual.features.module.modules.visuals.effects.hits.death;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** Разбиение тела на 6 частей (голова, торс, две руки, две ноги) с UV-прямоугольниками стандартного скина 64x64. */
public final class DeathBody {

    /** Размеры в пикселях (w,h,d), начало UV (u,v) и смещение центра в блоках: kind -> {w,h,d,u,v,cx,cy}. */
    private static final float[][] LAYOUT = {
            {8, 8, 8, 0, 0, 0.0f, 1.75f},
            {8, 12, 4, 16, 16, 0.0f, 1.125f},
            {4, 12, 4, 40, 16, -0.375f, 1.125f},
            {4, 12, 4, 32, 48, 0.375f, 1.125f},
            {4, 12, 4, 0, 16, -0.125f, 0.375f},
            {4, 12, 4, 16, 48, 0.125f, 0.375f},
    };

    private DeathBody() {
    }

    public static float[] layout(int kind) {
        return LAYOUT[kind];
    }

    /**
     * Части в мировых координатах: поза «стоя», повёрнутая по yaw тела, масштаб по росту сущности.
     * Центр тела считается на высоте груди.
     */
    public static List<DeathPhysics.Part> build(double px, double py, double pz, float bodyYawDeg, float height) {
        float scale = Math.max(0.3f, height / 1.8f);
        double yaw = Math.toRadians(-bodyYawDeg);
        float cos = (float) Math.cos(yaw);
        float sin = (float) Math.sin(yaw);
        List<DeathPhysics.Part> parts = new ArrayList<>(LAYOUT.length);
        for (int i = 0; i < LAYOUT.length; i++) {
            float[] l = LAYOUT[i];
            float lx = l[5] * scale;
            float wx = lx * cos;
            float wz = -lx * sin;
            DeathPhysics.Part p = new DeathPhysics.Part(i, (float) px + wx, (float) py + l[6] * scale, (float) pz + wz,
                    l[0] / 32.0f * scale, l[1] / 32.0f * scale, l[2] / 32.0f * scale);
            p.ry = (float) yaw;
            parts.add(p);
        }
        return parts;
    }

    public static float height(LivingEntity e) {
        return e.getBbHeight();
    }
}
