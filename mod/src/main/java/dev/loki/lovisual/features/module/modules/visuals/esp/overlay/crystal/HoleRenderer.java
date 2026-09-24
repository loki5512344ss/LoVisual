package dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.renderer.Renderer3DCulling;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a detected {@link Hole} for {@code CrystalHoles}: a vertical fade-filled box (opaque at
 * the base, transparent at the top) plus a solid bottom outline, with the whole marker's alpha
 * eased down by the player's horizontal distance so far holes fade out. Static geometry helpers;
 * the distance falloff reads the module's {@code rangeXZ} setting. Extracted so the module class
 * stays under the file-size cap.
 */
public final class HoleRenderer {

    private static final int BASE_FILL_ALPHA = 60;

    private final NumberValue<Integer> rangeXZ;

    public HoleRenderer(NumberValue<Integer> rangeXZ) {
        this.rangeXZ = rangeXZ;
    }

    /** Whether the hole is inside the camera frustum and its chunk section is visible. */
    public static boolean shouldRender(AABB box) {
        return Renderer3DCulling.isInFrustum(box) && Renderer3DCulling.isSectionVisible(box);
    }

    /** Render one hole: distance-faded gradient fill plus a bottom outline. */
    public void render(Renderer3D renderer, Hole hole) {
        AABB box = hole.box();
        int fillBottom = colorForDistance(box, hole.argb(), BASE_FILL_ALPHA);
        int fillTop = colorForDistance(box, hole.argb(), 0);
        int line = colorForDistance(box, hole.argb(), (hole.argb() >>> 24) & 0xFF);

        addVerticalFadeBox(renderer, box, fillBottom, fillTop);
        addBottomOutline(renderer, box, line);
    }

    private int colorForDistance(AABB box, int argb, int alpha) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 center = box.getCenter();
        double dx = center.x - mc.player.getX();
        double dz = center.z - mc.player.getZ();
        double distSqr = dx * dx + dz * dz;
        double maxSqr = Math.max(1.0, (double) rangeXZ.get() * rangeXZ.get());
        float factor = (float) (distSqr / maxSqr);
        factor = 1.0f - easeOutExpo(factor);
        factor = Mth.clamp(factor, 0.0f, 1.0f);

        int baseAlpha = (argb >>> 24) & 0xFF;
        int outAlpha = Mth.clamp((int) (factor * Math.min(alpha, baseAlpha)), 0, 255);
        return (outAlpha << 24) | (argb & 0x00FFFFFF);
    }

    private static float easeOutExpo(float x) {
        return x >= 1.0f ? 1.0f : (float) (1.0f - Math.pow(2.0, -10.0f * x));
    }

    private static void addVerticalFadeBox(Renderer3D renderer, AABB box, int bottomArgb, int topArgb) {
        MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED, Renderer3D.DepthMode.MAIN);
        if (mesh == null) {
            return;
        }

        int bottomA = (bottomArgb >>> 24) & 0xFF;
        int bottomR = (bottomArgb >>> 16) & 0xFF;
        int bottomG = (bottomArgb >>> 8) & 0xFF;
        int bottomB = bottomArgb & 0xFF;
        int topA = (topArgb >>> 24) & 0xFF;
        int topR = (topArgb >>> 16) & 0xFF;
        int topG = (topArgb >>> 8) & 0xFF;
        int topB = topArgb & 0xFF;

        addGradientQuad(mesh, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ,
                box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        addGradientQuad(mesh, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ,
                bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        addGradientQuad(mesh, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ,
                bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        addGradientQuad(mesh, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ,
                box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
    }

    private static void addGradientQuad(MeshBuilder mesh,
                                        double x1, double y1, double z1,
                                        double x2, double y2, double z2,
                                        double x3, double y3, double z3,
                                        double x4, double y4, double z4,
                                        int bottomR, int bottomG, int bottomB, int bottomA,
                                        int topR, int topG, int topB, int topA) {
        mesh.ensureQuadCapacity();
        double minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        double maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        double midY = (minY + maxY) * 0.5;
        int i1 = fadeVertex(mesh, x1, y1, z1, midY, bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        int i2 = fadeVertex(mesh, x2, y2, z2, midY, bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        int i3 = fadeVertex(mesh, x3, y3, z3, midY, bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        int i4 = fadeVertex(mesh, x4, y4, z4, midY, bottomR, bottomG, bottomB, bottomA, topR, topG, topB, topA);
        mesh.quad(i1, i2, i3, i4);
    }

    private static int fadeVertex(MeshBuilder mesh,
                                  double x, double y, double z, double midY,
                                  int bottomR, int bottomG, int bottomB, int bottomA,
                                  int topR, int topG, int topB, int topA) {
        boolean bottom = y <= midY;
        return mesh.vec3(x, y, z)
                .color(
                        bottom ? bottomR : topR,
                        bottom ? bottomG : topG,
                        bottom ? bottomB : topB,
                        bottom ? bottomA : topA
                )
                .next();
    }

    private static void addBottomOutline(Renderer3D renderer, AABB box, int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a <= 0) {
            return;
        }
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        renderer.line(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        renderer.line(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        renderer.line(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        renderer.line(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);
    }
}
