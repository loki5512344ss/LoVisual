package dev.loki.lovisual.features.module.modules.visuals.hats.tazik;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.player.HeadAnchor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Head-mounted bowl ("tazik") geometry: rim ellipse + inner top ellipse joined
 * by a wall, with a filled top disc and a highlighted rim ring. Colour is
 * supplied by the caller through {@link Shade} callbacks so this class stays
 * free of config state. Also hosts the two small motion / equipment helpers the
 * facade needs per player.
 */
public final class TazikHatModel {

    private TazikHatModel() {
    }

    /** Number of quads around the bowl; higher values cost more vertices. */
    static final int SEGMENTS = 56;

    static final float BOWL_RIM_RADIUS = 0.42f;
    static final float BOWL_TOP_RADIUS = 0.245f;
    static final float BOWL_HEIGHT = 0.18f;
    static final float BOWL_RIM_WIDTH = 0.052f;

    /** Per-vertex colour provider: {@code (segmentIndex, verticalLight, alphaMul) -> ARGB}. */
    public interface Shade {
        int color(int index, float verticalLight, float alphaMul);
    }

    /** Idle / hit-driven pitch-and-roll wobble applied to the hat. */
    public record Wobble(float pitchDeg, float rollDeg) {
    }

    /** Sum of an idle sway and an easeOutBack hit impulse, both scaled by {@code strength}. */
    public static Wobble resolveWobble(Player player, float tickDelta, float strength) {
        float hurt = Math.max(0.0f, player.hurtTime - tickDelta);
        float hit = AnimationUtility.easeOutBack(AnimationUtility.clamp01(hurt / 10.0f), 1.25f);
        float time = (player.tickCount + tickDelta) * 0.35f + player.getId() * 0.17f;
        float hitAmp = hit * 5.0f * strength;
        float idleAmp = 1.3f * strength;
        float pitch = Mth.sin(time * 3.7f) * hitAmp + Mth.sin(time * 1.2f) * idleAmp;
        float roll = Mth.cos(time * 3.1f) * hitAmp + Mth.cos(time * 0.9f) * idleAmp;
        return new Wobble(pitch, roll);
    }

    /** Any real helmet (leather..netherite) — used to slightly enlarge the hat on top. */
    public static boolean hasHelmet(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        return !stack.isEmpty() && (
                stack.is(Items.LEATHER_HELMET)
                        || stack.is(Items.CHAINMAIL_HELMET)
                        || stack.is(Items.IRON_HELMET)
                        || stack.is(Items.GOLDEN_HELMET)
                        || stack.is(Items.DIAMOND_HELMET)
                        || stack.is(Items.NETHERITE_HELMET)
        );
    }

    /** Emit triangles + outline lines for one bowl, anchored to the player's head. */
    public static void build(MeshBuilder tris,
                             MeshBuilder lines,
                             HeadAnchor anchor,
                             float size,
                             float yawDeg,
                             float pitchDeg,
                             float rollDeg,
                             Shade bowl,
                             Shade rim,
                             float alphaMul,
                             int seed) {
        float rimRadius = BOWL_RIM_RADIUS * size;
        float topRadius = BOWL_TOP_RADIUS * size;
        float height = BOWL_HEIGHT * size;
        float rimWidth = BOWL_RIM_WIDTH * size;
        double rimOuterY = 0.055 * size;
        double rimInnerY = 0.073 * size;
        double topOuterY = height * 0.82;
        double topInnerY = height;

        Vec3[] rimOuter = new Vec3[SEGMENTS];
        Vec3[] rimInner = new Vec3[SEGMENTS];
        Vec3[] topOuter = new Vec3[SEGMENTS];
        Vec3[] topInner = new Vec3[SEGMENTS];

        for (int i = 0; i < SEGMENTS; i++) {
            float angle = (float) ((Math.PI * 2.0 * i) / SEGMENTS);
            float sx = Mth.sin(angle);
            float sz = Mth.cos(angle);
            float rimWave = 1.0f + 0.010f * Mth.sin(angle * 4.0f + seed * 0.013f);
            float topWave = 1.0f + 0.006f * Mth.cos(angle * 3.0f + seed * 0.011f);
            float outerRim = rimRadius * rimWave;
            float innerRim = Math.max(0.02f, outerRim - rimWidth);
            float outerTop = topRadius * topWave;
            float innerTop = Math.max(0.02f, outerTop - rimWidth * 0.35f);
            rimOuter[i] = anchor.localRotated(sx * outerRim, rimOuterY, sz * outerRim, yawDeg, pitchDeg, rollDeg);
            rimInner[i] = anchor.localRotated(sx * innerRim, rimInnerY, sz * innerRim, yawDeg, pitchDeg, rollDeg);
            topOuter[i] = anchor.localRotated(sx * outerTop, topOuterY, sz * outerTop, yawDeg, pitchDeg, rollDeg);
            topInner[i] = anchor.localRotated(sx * innerTop, topInnerY, sz * innerTop, yawDeg, pitchDeg, rollDeg);
        }

        Vec3 topCenter = anchor.localRotated(0.0, topInnerY + 0.002 * size, 0.0, yawDeg, pitchDeg, rollDeg);
        draw(tris, lines, rimOuter, rimInner, topOuter, topInner, topCenter, bowl, rim, alphaMul);
    }

    private static void draw(MeshBuilder tris,
                             MeshBuilder lines,
                             Vec3[] rimOuter,
                             Vec3[] rimInner,
                             Vec3[] topOuter,
                             Vec3[] topInner,
                             Vec3 topCenter,
                             Shade bowl,
                             Shade rim,
                             float alphaMul) {
        for (int i = 0; i < SEGMENTS; i++) {
            int next = (i + 1) % SEGMENTS;
            int rimLow1 = bowl.color(i, 0.76f, alphaMul);
            int rimLow2 = bowl.color(next, 0.76f, alphaMul);
            int wallTop1 = bowl.color(i, 1.02f, alphaMul);
            int wallTop2 = bowl.color(next, 1.02f, alphaMul);
            int innerTop1 = bowl.color(i, 0.66f, alphaMul * 0.90f);
            int innerTop2 = bowl.color(next, 0.66f, alphaMul * 0.90f);
            int innerLow1 = bowl.color(i, 0.50f, alphaMul * 0.82f);
            int innerLow2 = bowl.color(next, 0.50f, alphaMul * 0.82f);
            int rim1 = rim.color(i, 1.0f, alphaMul);
            int rim2 = rim.color(next, 1.0f, alphaMul);
            int topCenterColor = bowl.color(i, 1.10f, alphaMul * 0.95f);

            quadGradient(tris, rimOuter[i], rimLow1, rimOuter[next], rimLow2, topOuter[next], wallTop2, topOuter[i], wallTop1);
            quadGradient(tris, topInner[i], innerTop1, topInner[next], innerTop2, rimInner[next], innerLow2, rimInner[i], innerLow1);
            quadGradient(tris, rimInner[i], rim1, rimInner[next], rim2, rimOuter[next], rim2, rimOuter[i], rim1);
            triangle(tris, topCenter, topInner[i], topInner[next], topCenterColor);
            lineGradient(lines, rimOuter[i], rimOuter[next], rim1, rim2);
            lineGradient(lines, topOuter[i], topOuter[next], wallTop1, wallTop2);
        }
    }

    private static void quadGradient(MeshBuilder mesh,
                                     Vec3 p1, int c1,
                                     Vec3 p2, int c2,
                                     Vec3 p3, int c3,
                                     Vec3 p4, int c4) {
        mesh.ensureQuadCapacity();
        int i1 = putColorVertex(mesh, p1, c1);
        int i2 = putColorVertex(mesh, p2, c2);
        int i3 = putColorVertex(mesh, p3, c3);
        int i4 = putColorVertex(mesh, p4, c4);
        mesh.quad(i1, i2, i3, i4);
    }

    private static void triangle(MeshBuilder mesh, Vec3 p1, Vec3 p2, Vec3 p3, int argb) {
        mesh.ensureTriCapacity();
        int i1 = putColorVertex(mesh, p1, argb);
        int i2 = putColorVertex(mesh, p2, argb);
        int i3 = putColorVertex(mesh, p3, argb);
        mesh.triangle(i1, i2, i3);
    }

    private static void lineGradient(MeshBuilder mesh, Vec3 p1, Vec3 p2, int c1, int c2) {
        mesh.ensureLineCapacity();
        int i1 = putColorVertex(mesh, p1, c1);
        int i2 = putColorVertex(mesh, p2, c2);
        mesh.line(i1, i2);
    }

    private static int putColorVertex(MeshBuilder mesh, Vec3 p, int argb) {
        mesh.vec3(p.x, p.y, p.z)
                .color((argb >>> 16) & 255, (argb >>> 8) & 255, argb & 255, (argb >>> 24) & 255);
        return mesh.next();
    }
}
