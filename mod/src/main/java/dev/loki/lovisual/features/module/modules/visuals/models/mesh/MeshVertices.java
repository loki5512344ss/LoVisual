package dev.loki.lovisual.features.module.modules.visuals.models.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Triangle lists as plain floats: eight per vertex (position, uv, normal), three vertices per triangle.
 * Shared by every mesh model so they all emit geometry the same way.
 */
public final class MeshVertices {
    public static final int FLOATS_PER_VERTEX = 8;
    private static final int FLOATS_PER_TRIANGLE = 3 * FLOATS_PER_VERTEX;

    private MeshVertices() {
    }

    /** Submits the triangles as a cutout entity layer; a quad's fourth corner repeats the third. */
    public static void submit(PoseStack pose, SubmitNodeCollector collector, Identifier texture,
                              float[] triangles, int light, int overlay) {
        if (triangles.length == 0) return;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, consumer) -> {
            for (int t = 0; t + FLOATS_PER_TRIANGLE <= triangles.length; t += FLOATS_PER_TRIANGLE) {
                vertex(matrix, consumer, triangles, t, light, overlay);
                vertex(matrix, consumer, triangles, t + FLOATS_PER_VERTEX, light, overlay);
                vertex(matrix, consumer, triangles, t + 2 * FLOATS_PER_VERTEX, light, overlay);
                vertex(matrix, consumer, triangles, t + 2 * FLOATS_PER_VERTEX, light, overlay);
            }
        });
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float[] data, int i, int light, int overlay) {
        consumer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                .setColor(-1).setUv(data[i + 3], data[i + 4])
                .setOverlay(overlay).setLight(light)
                .setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
    }
}
