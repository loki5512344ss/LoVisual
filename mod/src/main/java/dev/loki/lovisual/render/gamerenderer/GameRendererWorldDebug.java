package dev.loki.lovisual.render.gamerenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.logging.DebugLog;
import org.joml.Matrix4f;

/**
 * Manual-only render debug probes (compile-time disabled). Extracted from
 * GameRendererMixin to keep the mixin hook-only.
 */
public final class GameRendererWorldDebug {

    private static final boolean CLIP_SPACE = false;
    private static final boolean PRIMITIVE = false;
    private static final long INTERVAL_MS = 2000L;

    private Renderer3D clipSpaceRenderer;
    private long lastPrimitiveLogMs;

    /** Renders the NDC clip-space probe; when it ran the caller must return early. */
    public boolean clipSpaceIfNeeded() {
        if (!CLIP_SPACE) return false;
        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();

        MeshRenderer.setProjection(new Matrix4f().identity());
        RenderState.rendering3D = false;

        if (clipSpaceRenderer == null) {
            clipSpaceRenderer = new Renderer3D(
                    LoVisualRenderPipelines.WORLD_COLORED_LINES,
                    LoVisualRenderPipelines.WORLD_COLORED
            );
        }

        clipSpaceRenderer.begin();

        // Clip-space square + diagonals (NDC)
        clipSpaceRenderer.line(-0.8, -0.8, 0.0, 0.8, -0.8, 0.0, 255, 0, 0, 255);
        clipSpaceRenderer.line(0.8, -0.8, 0.0, 0.8, 0.8, 0.0, 0, 255, 0, 255);
        clipSpaceRenderer.line(0.8, 0.8, 0.0, -0.8, 0.8, 0.0, 0, 0, 255, 255);
        clipSpaceRenderer.line(-0.8, 0.8, 0.0, -0.8, -0.8, 0.0, 255, 255, 0, 255);
        clipSpaceRenderer.line(-0.8, -0.8, 0.0, 0.8, 0.8, 0.0, 255, 0, 255, 255);
        clipSpaceRenderer.line(0.8, -0.8, 0.0, -0.8, 0.8, 0.0, 0, 255, 255, 255);

        clipSpaceRenderer.render(new PoseStack());

        mv.popMatrix();
        RenderState.rendering3D = false;
        return true;
    }

    /** Draws wire cubes 3 blocks ahead of the camera plus a throttled matrix diag log. */
    public void primitiveIfNeeded(Renderer3D renderer, Renderer3D depthRenderer, Camera camera, Matrix4f projection) {
        if (!PRIMITIVE) return;
        Vec3 camPos = camera.position();
        float yaw = camera.yRot() * ((float) Math.PI / 180.0f);
        float pitch = camera.xRot() * ((float) Math.PI / 180.0f);
        Vec3 forward = new Vec3(
                -Mth.sin(yaw) * Mth.cos(pitch),
                -Mth.sin(pitch),
                Mth.cos(yaw) * Mth.cos(pitch)
        );

        Vec3 center = camPos.add(forward.scale(3.0));
        double r = 0.5;
        double x1 = center.x - r, x2 = center.x + r;
        double y1 = center.y - r, y2 = center.y + r;
        double z1 = center.z - r, z2 = center.z + r;
        double ox1 = -1.0, oy1 = -1.0, oz1 = -1.0;
        double ox2 = 1.0, oy2 = 1.0, oz2 = 1.0;

        // Wire cube (no depth) and depth variant
        Renderer3D[] targets = new Renderer3D[]{renderer, depthRenderer};
        for (Renderer3D rdr : targets) {
            drawDebugCube(rdr, x1, y1, z1, x2, y2, z2);
            drawDebugCube(rdr, ox1, oy1, oz1, ox2, oy2, oz2);
        }

        long now = System.currentTimeMillis();
        if (now - lastPrimitiveLogMs >= INTERVAL_MS) {
            lastPrimitiveLogMs = now;
            Matrix4f mvMat = RenderSystem.getModelViewMatrixCopy();
            DebugLog.renderThread(
                    "[LoVisual] Debug diag: cam=" + camPos
                            + " proj=[" + projection.m00() + "," + projection.m11() + "," + projection.m22() + "," + projection.m33() + "]"
                            + " mv=[" + mvMat.m30() + "," + mvMat.m31() + "," + mvMat.m32() + "]"
            );
        }
    }

    private static void drawDebugCube(Renderer3D rdr,
                                      double x1, double y1, double z1,
                                      double x2, double y2, double z2) {
        rdr.line(x1, y1, z1, x2, y1, z1, 255, 0, 0, 255);
        rdr.line(x1, y2, z1, x2, y2, z1, 255, 0, 0, 255);
        rdr.line(x1, y1, z2, x2, y1, z2, 255, 0, 0, 255);
        rdr.line(x1, y2, z2, x2, y2, z2, 255, 0, 0, 255);

        rdr.line(x1, y1, z1, x1, y2, z1, 0, 255, 0, 255);
        rdr.line(x2, y1, z1, x2, y2, z1, 0, 255, 0, 255);
        rdr.line(x1, y1, z2, x1, y2, z2, 0, 255, 0, 255);
        rdr.line(x2, y1, z2, x2, y2, z2, 0, 255, 0, 255);

        rdr.line(x1, y1, z1, x1, y1, z2, 0, 0, 255, 255);
        rdr.line(x2, y1, z1, x2, y1, z2, 0, 0, 255, 255);
        rdr.line(x1, y2, z1, x1, y2, z2, 0, 0, 255, 255);
        rdr.line(x2, y2, z1, x2, y2, z2, 0, 0, 255, 255);
    }
}
