package dev.loki.lovisual.render.engine.optimize.parallel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.loki.lovisual.render.engine.optimize.parallel.ModelGeometry.Cube;
import dev.loki.lovisual.render.engine.optimize.parallel.ParallelModels.Job;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

/**
 * Writes one posed model's vertices into native memory, mirroring
 * {@code ModelPart.render}/{@code translateAndRotate}/{@code Cube.compile}
 * but reading the per-model pose snapshot instead of the shared part instance
 * — that is what makes the build safe off the render thread.
 */
final class ModelBuilder {

    static final int STRIDE;
    static final int FIELDS = 9;

    static {
        VertexFormat format = ParallelModels.FORMAT;
        STRIDE = format.getVertexSize();
    }

    private ModelBuilder() {
    }

    static void build(Job job) {
        PoseStack poses = ParallelModels.stacks();
        poses.last().set(job.matrix);
        long[] ptr = {job.address};
        for (int i = 0; i < job.geometry.parts.length; ) {
            i = part(job, poses, i, ptr);
        }
    }

    private static int normal(float c) {
        return (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F) & 0xFF;
    }

    private static int part(Job job, PoseStack poses, int i, long[] ptr) {
        ModelGeometry geometry = job.geometry;
        int end = geometry.end[i];
        if ((job.flags[i] & 1) == 0 || geometry.empty[i]) {
            return end;
        }
        float[] p = job.pose;
        int o = i * FIELDS;
        poses.pushPose();
        poses.translate(p[o] / 16.0F, p[o + 1] / 16.0F, p[o + 2] / 16.0F);
        if (p[o + 3] != 0.0F || p[o + 4] != 0.0F || p[o + 5] != 0.0F) {
            // what PoseStack.mulPose does; spelled out for the worker thread
            Quaternionf rotation = new Quaternionf().rotationZYX(p[o + 5], p[o + 4], p[o + 3]);
            poses.last().pose().rotate(rotation);
            poses.last().normal().rotate(rotation);
        }
        if (p[o + 6] != 1.0F || p[o + 7] != 1.0F || p[o + 8] != 1.0F) {
            poses.scale(p[o + 6], p[o + 7], p[o + 8]);
        }
        if ((job.flags[i] & 2) == 0) {
            PoseStack.Pose pose = poses.last();
            Matrix4f matrix = pose.pose();
            Vector3f v = new Vector3f();
            // what SpriteCoordinateExpander does to every vertex; the sprite's
            // bounds are final, safe to read here
            TextureAtlasSprite sprite = job.sprite;
            for (Cube cube : geometry.cubes[i]) {
                // a cube's 24 vertices share 8 corners: transform the corners once
                float[] at = ParallelModels.corners();
                if (at.length < cube.corners.length) {
                    at = new float[cube.corners.length];
                    ParallelModels.setCorners(at);
                }
                for (int c = 0; c < cube.corners.length; c += 3) {
                    matrix.transformPosition(cube.corners[c], cube.corners[c + 1], cube.corners[c + 2], v);
                    at[c] = v.x();
                    at[c + 1] = v.y();
                    at[c + 2] = v.z();
                }
                int k = 0;
                for (int q = 0; q < cube.sizes.length; q++) {
                    Vector3f n = pose.transformNormal(cube.normals[q * 3], cube.normals[q * 3 + 1], cube.normals[q * 3 + 2], v);
                    int packedNormal = normal(n.x()) | normal(n.y()) << 8 | normal(n.z()) << 16;
                    for (int e = k + cube.sizes[q]; k < e; k++) {
                        int c = cube.index[k] * 3;
                        float u = cube.uv[k * 2];
                        float w = cube.uv[k * 2 + 1];
                        if (sprite != null) {
                            u = sprite.getU(u);
                            w = sprite.getV(w);
                        }
                        long v0 = ptr[0];
                        MemoryUtil.memPutFloat(v0, at[c]);
                        MemoryUtil.memPutFloat(v0 + 4, at[c + 1]);
                        MemoryUtil.memPutFloat(v0 + 8, at[c + 2]);
                        MemoryUtil.memPutInt(v0 + 12, job.color);
                        MemoryUtil.memPutFloat(v0 + 16, u);
                        MemoryUtil.memPutFloat(v0 + 20, w);
                        MemoryUtil.memPutInt(v0 + 24, job.overlay);
                        MemoryUtil.memPutInt(v0 + 28, job.light);
                        MemoryUtil.memPutInt(v0 + 32, packedNormal);
                        ptr[0] += STRIDE;
                    }
                }
            }
        }
        for (int c = i + 1; c < end; ) {
            c = part(job, poses, c, ptr);
        }
        poses.popPose();
        return end;
    }

    static int countVertices(ModelGeometry geometry, Job job) {
        int vertices = 0;
        for (int i = 0; i < geometry.parts.length; ) {
            if ((job.flags[i] & 1) == 0) {
                i = geometry.end[i];
                continue;
            }
            if ((job.flags[i] & 2) == 0) {
                vertices += geometry.vertices[i];
            }
            i++;
        }
        return vertices;
    }

    static void snapshotPose(ModelPart part, Job job, int i) {
        int o = i * FIELDS;
        job.pose[o] = part.x;
        job.pose[o + 1] = part.y;
        job.pose[o + 2] = part.z;
        job.pose[o + 3] = part.xRot;
        job.pose[o + 4] = part.yRot;
        job.pose[o + 5] = part.zRot;
        job.pose[o + 6] = part.xScale;
        job.pose[o + 7] = part.yScale;
        job.pose[o + 8] = part.zScale;
        job.flags[i] = (byte) ((part.visible ? 1 : 0) | (part.skipDraw ? 2 : 0));
    }
}
