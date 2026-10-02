package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails.pose;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Концы крыльев элитры по настоящей модели: та же цепочка преобразований, что у отрисовки элитры на теле
 * игрока (масштаб, поворот корпуса, наклон в полёте, поворот по траектории, крыло с анимацией взмаха),
 * как в evo. Чистая математика без Minecraft-классов, кроме {@link Vec3}.
 */
public final class WingTipLocator {

    static final float MODEL_SCALE = 0.9375f;
    static final float BODY_OFFSET_Y = -1.501f;
    static final float WINGS_OFFSET_Z = 0.125f;
    static final float WING_PIVOT_X = 0.3125f;
    static final float WING_CROUCH_Y = 0.1875f;
    static final float TIP_X = 0.6875f;
    static final float TIP_Y = 1.3125f;
    static final float TIP_Z = 0.1875f;
    static final float SPIN_ATTACK_DEGREES = -75.0f;
    static final float FALL_FLYING_PITCH_OFFSET = -90.0f;
    static final float BODY_ROT_OFFSET = 180.0f;
    static final float FALL_FLYING_DIVISOR = 100.0f;
    static final float FLIGHT_ROT_EPSILON = 1.0E-5f;

    private static final float DEG = (float) (Math.PI / 180.0);

    private WingTipLocator() {
    }

    /**
     * Состояние игрока для расчёта.
     *
     * @param x,y,z           интерполированная позиция (ноги)
     * @param bodyYawDeg      интерполированное рыскание корпуса, градусы
     * @param xRotDeg         наклон взгляда, градусы
     * @param scale           масштаб сущности (атрибут размера)
     * @param spinAttack      крутится трезубцем (riptide)
     * @param fallFlyingTicks сколько тиков планирует (с долей кадра)
     * @param flyingYRot      см. {@link #flyingYRot}
     * @param spinTime        tickCount + partialTick для вращения riptide
     * @param rotX,rotY,rotZ  анимация крыла: у правого крыла знаки rotY и rotZ меняются
     */
    public record Pose(double x, double y, double z, float bodyYawDeg, float xRotDeg, float scale,
                       boolean crouching, boolean spinAttack, float fallFlyingTicks, float flyingYRot,
                       float spinTime, float rotX, float rotY, float rotZ) {
    }

    public record Tips(Vec3 left, Vec3 right) {
    }

    /** Доля «лёжа»: 0 в начале планирования, 1 после десяти тиков. */
    public static float fallFlyingScale(float fallFlyingTicks) {
        float f = fallFlyingTicks * fallFlyingTicks / FALL_FLYING_DIVISOR;
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    /**
     * Поворот корпуса вокруг вертикали при лёгком рысканьи: угол между горизонтальной скоростью и взглядом,
     * со знаком по векторному произведению; 0, если игрок почти стоит или смотрит вертикально.
     */
    public static float flyingYRot(double deltaX, double deltaZ, double viewX, double viewZ) {
        double dl = deltaX * deltaX + deltaZ * deltaZ;
        double vl = viewX * viewX + viewZ * viewZ;
        if (dl <= 1.0E-5 || vl <= 1.0E-5) return 0.0f;
        double dot = (deltaX * viewX + deltaZ * viewZ) / Math.sqrt(dl * vl);
        double cross = deltaX * viewZ - deltaZ * viewX;
        return (float) (Math.signum(cross) * Math.acos(Math.min(1.0, Math.abs(dot))));
    }

    public static Tips tips(Pose p) {
        Matrix4f body = new Matrix4f().scale(p.scale()).rotateY((BODY_ROT_OFFSET - p.bodyYawDeg()) * DEG);
        if (p.spinAttack()) {
            body.rotateX((FALL_FLYING_PITCH_OFFSET - p.xRotDeg()) * DEG).rotateY(p.spinTime() * SPIN_ATTACK_DEGREES * DEG);
        } else {
            body.rotateX(fallFlyingScale(p.fallFlyingTicks()) * (FALL_FLYING_PITCH_OFFSET - p.xRotDeg()) * DEG);
            if (Math.abs(p.flyingYRot()) > FLIGHT_ROT_EPSILON) body.rotateY(p.flyingYRot());
        }
        body.scale(-1.0f, -1.0f, 1.0f).scale(MODEL_SCALE).translate(0.0f, BODY_OFFSET_Y, WINGS_OFFSET_Z);
        float pivotY = p.crouching() ? WING_CROUCH_Y : 0.0f;
        Vec3 left = tip(body, p, WING_PIVOT_X, pivotY, p.rotZ(), p.rotY(), p.rotX(), -TIP_X);
        Vec3 right = tip(body, p, -WING_PIVOT_X, pivotY, -p.rotZ(), -p.rotY(), p.rotX(), TIP_X);
        return new Tips(left, right);
    }

    private static Vec3 tip(Matrix4f body, Pose p, float pivotX, float pivotY, float rz, float ry, float rx,
                            float tipX) {
        Matrix4f wing = new Matrix4f(body).translate(pivotX, pivotY, 0.0f).rotateZYX(rz, ry, rx);
        Vector4f v = new Vector4f(tipX, TIP_Y, TIP_Z, 1.0f);
        wing.transform(v);
        return new Vec3(p.x() + v.x, p.y() + v.y, p.z() + v.z);
    }
}
