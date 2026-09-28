package dev.loki.lovisual.features.module.modules.visuals.wings;

import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a pair of wings on a player's back: anchor transform (position/body yaw/pose,
 * mirroring the reference PoseStack chain via joml matrices) then the fan of glow/core/
 * fill triangles + outline + ribs per side. Ported from the WiNGS reference onto
 * Renderer3D batches (world-space vertices, pipelines supply depth behavior).
 */
public final class WingsRenderer {
    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int DEFAULT_ALPHA = 220;

    private WingsRenderer() {
    }

    /** Per-frame style knobs resolved by the module. */
    public record Style(String type, float scale, boolean flapping, float flapStrength, float flapSpeed,
                        int baseColorArgb, boolean glow, boolean outline, boolean ribs, boolean depthTest) {
    }

    public static void render(Renderer3D renderer, Player player, float tickDelta, float bodyYaw, Style s) {
        if (renderer == null || player == null) return;

        WingShapes.Pose pose = resolvePose(player, tickDelta);
        if (pose == null) return;

        WingShapes.Point[] shape = WingShapes.shape(s.type());
        int[] ribIndices = WingShapes.ribs(shape);
        Vec3 pos = player.getPosition(tickDelta);
        float move = Mth.clamp((float) player.getDeltaMovement().horizontalDistance() * 10f, 0f, 1f);

        float flap = 0f;
        if (s.flapping()) {
            float effectiveSpeed = pose.flapSpeed() * (s.flapSpeed() / 3.0f);
            float effectiveAmp = pose.flapAmplitude() * (s.flapStrength() / 30.0f);
            flap = Mth.sin((player.tickCount + tickDelta) * effectiveSpeed) * effectiveAmp;
        }
        float open = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost()) * pose.openMultiplier();
        float ws = s.scale() * pose.scaleMultiplier();

        int glowColor = interpolate(s.baseColorArgb(), 0xFFFFFF, 0.28f);
        int coreColor = interpolate(s.baseColorArgb(), 0xFFFFFF, 0.55f);

        Meshes meshes = new Meshes(renderer, s.depthTest());
        for (float side = -1f; side <= 1f; side += 2f) {
            drawWingSide(meshes, pos, bodyYaw, pose, shape, ribIndices, side, open, ws, s, glowColor, coreColor);
        }
    }

    private static final class Meshes {
        final MeshBuilder glow;
        final MeshBuilder fill;
        final MeshBuilder outline;
        final Renderer3D.DepthMode depthMode;

        Meshes(Renderer3D renderer, boolean depthTest) {
            this.depthMode = depthTest ? Renderer3D.DepthMode.MAIN : Renderer3D.DepthMode.NONE;
            this.glow = renderer.batch(depthTest
                    ? LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH
                    : LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE, depthMode);
            this.fill = renderer.batch(depthTest
                    ? LoVisualRenderPipelines.WORLD_COLORED_DEPTH
                    : LoVisualRenderPipelines.WORLD_COLORED, depthMode);
            this.outline = renderer.batch(depthTest
                    ? LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH
                    : LoVisualRenderPipelines.WORLD_COLORED_LINES, depthMode);
        }
    }

    /**
     * Builds the matrix chain of the reference: translate(player) · Y(180-yaw) · preTranslate ·
     * X(pitch) · Z(roll) · translate(anchor) · scale · [per side] translate(side) · Y(side·open) ·
     * Z(side·sideRoll) · X(sidePitch). Wing-local verts are (side·x, y, 0).
     */
    private static void drawWingSide(Meshes m, Vec3 pos, float bodyYaw, WingShapes.Pose pose,
                                     WingShapes.Point[] shape, int[] ribIndices, float side,
                                     float open, float ws, Style s, int glowColor, int coreColor) {
        Matrix4f mat = new Matrix4f()
                .translate((float) pos.x, (float) pos.y, (float) pos.z)
                .rotateY((float) Math.toRadians(180f - bodyYaw))
                .translate(0f, pose.preTranslateY(), pose.preTranslateZ())
                .rotateX((float) Math.toRadians(pose.pitchRotation()))
                .rotateZ((float) Math.toRadians(pose.rollRotation()))
                .translate(0f, pose.anchorY(), pose.anchorZ())
                .scale(ws)
                .translate(side * pose.sideOffset(), pose.sideYOffset(), pose.sideZOffset())
                .rotateY((float) Math.toRadians(side * open))
                .rotateZ((float) Math.toRadians(side * pose.sideRoll()))
                .rotateX((float) Math.toRadians(pose.sidePitch()));

        int baseAlpha = DEFAULT_ALPHA;
        int rootGlow = setAlpha(glowColor, (int) (baseAlpha * 0.22f));
        int rootCore = setAlpha(coreColor, (int) (baseAlpha * 0.26f));
        int rootFill = setAlpha(s.baseColorArgb(), baseAlpha);
        int edgeFill = setAlpha(s.baseColorArgb(), 10);
        int rootRib = setAlpha(glowColor, Math.max(8, (int) (alphaOf(glowColor) * 0.75f)));

        fanLayer(m.glow, mat, shape, 1.22f, side, rootGlow, setAlpha(glowColor, 0));
        fanLayer(m.glow, mat, shape, 0.84f, side, rootCore, setAlpha(coreColor, 0));
        fanLayer(m.fill, mat, shape, 1.0f, side, rootFill, edgeFill);
        if (s.outline()) outline(m.outline, mat, shape, side, 1.0f, setAlpha(s.baseColorArgb(), (int) (baseAlpha * 0.62f)));
        if (s.ribs()) ribs(m.glow, mat, shape, ribIndices, side, rootRib);
    }

    private static void fanLayer(MeshBuilder mesh, Matrix4f mat, WingShapes.Point[] shape,
                                 float layerScale, float side, int rootColor, int edgeColor) {
        if (mesh == null) return;
        int rootIdx = put(mesh, mat, 0f, 0f, 0f, rootColor);
        for (int i = 0; i < shape.length; i++) {
            WingShapes.Point cur = shape[i];
            WingShapes.Point next = shape[(i + 1) % shape.length];
            int curAlpha = setAlpha(edgeColor, Math.round(alphaOf(edgeColor) * cur.alphaMul()));
            int nextAlpha = setAlpha(edgeColor, Math.round(alphaOf(edgeColor) * next.alphaMul()));
            int i1 = put(mesh, mat, side * cur.x() * layerScale, cur.y() * layerScale, 0f, curAlpha);
            int i2 = put(mesh, mat, side * next.x() * layerScale, next.y() * layerScale, 0f, nextAlpha);
            mesh.triangle(rootIdx, i1, i2);
        }
    }

    private static void outline(MeshBuilder mesh, Matrix4f mat, WingShapes.Point[] shape,
                                float side, float scale, int color) {
        if (mesh == null) return;
        int first = put(mesh, mat, side * shape[0].x() * scale, shape[0].y() * scale, 0f, color);
        int prev = first;
        for (int i = 1; i < shape.length; i++) {
            WingShapes.Point p = shape[i];
            int idx = put(mesh, mat, side * p.x() * scale, p.y() * scale, 0f, color);
            mesh.line(prev, idx);
            prev = idx;
        }
        mesh.line(prev, first);
    }

    private static void ribs(MeshBuilder mesh, Matrix4f mat, WingShapes.Point[] shape, int[] ribIndices,
                             float side, int color) {
        if (mesh == null) return;
        int rootIdx = put(mesh, mat, 0f, 0f, 0f, color);
        for (int idx : ribIndices) {
            if (idx >= shape.length) continue;
            WingShapes.Point p = shape[idx];
            int tip = put(mesh, mat, side * p.x() * 0.96f, p.y() * 0.96f, 0f,
                    setAlpha(color, Math.max(0, Math.round(alphaOf(color) * p.alphaMul()))));
            mesh.line(rootIdx, tip);
        }
    }

    private static int put(MeshBuilder mesh, Matrix4f mat, float x, float y, float z, int argb) {
        Vec3 world = transformPosition(mat, x, y, z);
        mesh.vec3((float) world.x, (float) world.y, (float) world.z)
                .color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
        return mesh.next();
    }

    /**
     * Transforms a local-space point by {@code mat} (dest = M * v). JOML's
     * Matrix4f is column-major (m&lt;col&gt;&lt;row&gt;), so a position transform
     * reads DOWN columns 0/1/2 - see {@code Vector3f.mulPositionGeneric}. A
     * previous row-wise implementation here read mat.m03()/m13()/m23() (the
     * always-zero perspective row on an affine matrix) instead of the real
     * translation column (m30/m31/m32), so the player's world position was
     * never applied and every wing vertex landed near the world origin
     * instead of on the player's back - the module rendered nothing visible.
     */
    static Vec3 transformPosition(Matrix4f mat, float x, float y, float z) {
        float wx = mat.m00() * x + mat.m10() * y + mat.m20() * z + mat.m30();
        float wy = mat.m01() * x + mat.m11() * y + mat.m21() * z + mat.m31();
        float wz = mat.m02() * x + mat.m12() * y + mat.m22() * z + mat.m32();
        return new Vec3(wx, wy, wz);
    }

    private static WingShapes.Pose resolvePose(Player player, float tickDelta) {
        if (player.isFallFlying()) {
            float flightTicks = (float) player.getFallFlyingTicks() + tickDelta;
            float progress = Mth.clamp(flightTicks * flightTicks / 100f, 0f, 1f);
            float pitch = Mth.lerp(tickDelta, player.xRotO, player.getXRot());
            return WingShapes.glidePose(pitch, progress);
        }
        if (player.isInWater()) return null;
        if (player.isShiftKeyDown()) return WingShapes.POSE_SNEAK;
        return WingShapes.POSE_IDLE;
    }

    static int setAlpha(int color, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    static int alphaOf(int color) {
        return (color >>> 24) & 0xFF;
    }

    /** Linear lerp of rgb toward a white-ish target rgb (alpha preserved from source). */
    static int interpolate(int fromArgb, int targetRgb, float t) {
        int r = Math.round(((fromArgb >>> 16) & 0xFF) + ((((targetRgb >>> 16) & 0xFF)) - ((fromArgb >>> 16) & 0xFF)) * t);
        int g = Math.round(((fromArgb >>> 8) & 0xFF) + ((((targetRgb >>> 8) & 0xFF)) - ((fromArgb >>> 8) & 0xFF)) * t);
        int b = Math.round((fromArgb & 0xFF) + ((targetRgb & 0xFF) - (fromArgb & 0xFF)) * t);
        return setAlpha((r << 16) | (g << 8) | b, alphaOf(fromArgb));
    }
}
