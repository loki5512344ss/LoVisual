package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles;

import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape.Shape;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape.ShapeGeometry;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape.ShapeMesh;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

import java.util.function.IntUnaryOperator;

/** Writes jump circles into world meshes: coloured shape triangles or one textured quad. */
public final class CircleRenderer {
    private static final float OUTLINE_BRIGHTNESS = 0.35f;
    private final ShapeMesh shapeMesh = new ShapeMesh();

    /** Where and how a circle is drawn this frame; angle and wavePhase are in radians. */
    public record Pose(double x, double y, double z, float radius, float angle, float alpha, float wavePhase) {
    }

    /** Additive batch for shapes (coloured) or for the texture when {@code shape} is null; depth mask is never written. */
    public MeshBuilder openMesh(Renderer3D renderer, Shape shape, Identifier texture, boolean depthTest) {
        Renderer3D.DepthMode depthMode = depthTest ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        if (shape != null) {
            RenderPipeline pipeline = depthTest
                    ? LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH
                    : LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE;
            return renderer.batch(pipeline, depthMode);
        }
        RenderPipeline pipeline = depthTest
                ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE
                : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE;
        return renderer.batchTextured(pipeline, texture, depthMode);
    }

    /** {@code palette} maps a 0..360 gradient phase to ARGB. */
    public void shape(MeshBuilder mesh, Pose pose, Shape shape, int segments, float thickness,
                      IntUnaryOperator palette) {
        ShapeGeometry.build(shape, shapeMesh, segments, thickness, pose.wavePhase());
        int vertices = shapeMesh.vertexCount();
        int indices = shapeMesh.indexCount();
        mesh.ensureCapacity(vertices, indices);
        float cos = (float) Math.cos(pose.angle());
        float sin = (float) Math.sin(pose.angle());
        int base = -1;
        for (int i = 0; i < vertices; i++) {
            float lx = shapeMesh.x(i) * pose.radius();
            float lz = shapeMesh.z(i) * pose.radius();
            int main = palette.applyAsInt(Math.round(shapeMesh.gradient(i) * 360f));
            int argb = AnimatedRenderColors.mixArgb(main,
                    AnimatedRenderColors.brighten(main, OUTLINE_BRIGHTNESS), shapeMesh.shade(i));
            int v = put(mesh, pose.x() + lx * cos - lz * sin, pose.y(), pose.z() + lx * sin + lz * cos,
                    AnimatedRenderColors.scaleAlpha(argb, pose.alpha()), false, 0, 0);
            if (base < 0) base = v;
        }
        for (int i = 0; i < indices; i += 3) {
            mesh.triangle(base + shapeMesh.index(i), base + shapeMesh.index(i + 1), base + shapeMesh.index(i + 2));
        }
    }

    public void texturedQuad(MeshBuilder mesh, Pose pose, IntUnaryOperator palette) {
        float r = pose.radius();
        float cos = (float) Math.cos(pose.angle()) * r;
        float sin = (float) Math.sin(pose.angle()) * r;
        mesh.ensureQuadCapacity();
        int a = corner(mesh, pose, -cos - sin, cos - sin, 0, 1, palette.applyAsInt(270));
        int b = corner(mesh, pose, cos - sin, cos + sin, 1, 1, palette.applyAsInt(0));
        int c = corner(mesh, pose, cos + sin, sin - cos, 1, 0, palette.applyAsInt(180));
        int d = corner(mesh, pose, -cos + sin, -sin - cos, 0, 0, palette.applyAsInt(90));
        mesh.quad(a, b, c, d);
    }

    private static int corner(MeshBuilder mesh, Pose pose, double dx, double dz, double u, double v, int argb) {
        return put(mesh, pose.x() + dx, pose.y(), pose.z() + dz,
                AnimatedRenderColors.scaleAlpha(argb, pose.alpha()), true, u, v);
    }

    private static int put(MeshBuilder mesh, double x, double y, double z, int argb,
                           boolean textured, double u, double v) {
        mesh.vec3(x, y, z);
        if (textured) mesh.vec2(u, v);
        mesh.color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24);
        return mesh.next();
    }
}
