package dev.loki.lovisual.features.module.modules.visuals.knife.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.lovisual.features.module.modules.visuals.knife.model.KnifeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Sends the posed bones of a knife model to the renderer as one custom-geometry layer. */
final class KnifeGeometry {
    private KnifeGeometry() {
    }

    static void submit(KnifeBones bones, KnifeModel model, Identifier texture, PoseStack stack,
                       SubmitNodeCollector collector, int light) {
        int count = model.boneCount();
        collector.submitCustomGeometry(stack, RenderTypes.entityTranslucent(texture), (pose, consumer) -> {
            Matrix3f normalMatrix = new Matrix3f();
            Vector3f point = new Vector3f();
            Vector3f normal = new Vector3f();
            for (int bone = 0; bone < count; bone++) {
                float[] mesh = model.meshOf(bone);
                if (mesh == null || !bones.visible[bone]) continue;
                Matrix4f world = bones.world[bone];
                world.normal(normalMatrix);
                for (int v = 0; v + KnifeModel.FLOATS_PER_VERTEX <= mesh.length; v += KnifeModel.FLOATS_PER_VERTEX) {
                    point.set(mesh[v], mesh[v + 1], mesh[v + 2]);
                    world.transformPosition(point);
                    normal.set(mesh[v + 5], mesh[v + 6], mesh[v + 7]).mul(normalMatrix).normalize();
                    vertex(pose, consumer, point, normal, mesh[v + 3], mesh[v + 4], light);
                }
            }
        });
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vector3f point, Vector3f normal,
                               float u, float v, int light) {
        consumer.addVertex(pose, point.x, point.y, point.z)
                .setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }
}
