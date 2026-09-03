package dev.loki.lovisual.features.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a rest-pose (no bb-animation) {@link CosmeticModel} bone/cube tree attached to a
 * player, as camera-relative textured quads through the engine's {@link Renderer3D}/{@link MeshBuilder}.
 */
public final class CosmeticRenderer {
    private static final float UNITS_PER_BLOCK = 16.0f;

    private CosmeticRenderer() {
    }

    public static void renderOnPlayer(Renderer3D renderer, Player player, CosmeticType type, float tickDelta) {
        if (renderer == null || player == null) return;

        CosmeticModel model = CosmeticManager.get().getActive(type);
        if (model == null) return;

        model.loadTexture();
        List<CosmeticBone> roots = model.getRenderRoots();
        if (roots.isEmpty()) return;

        Vec3 pos = RenderMath.getLerpedPos(player, tickDelta);
        float bodyYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);

        PoseStack matrices = new PoseStack();
        matrices.translate(pos.x, pos.y, pos.z);
        CosmeticAttachmentTransforms.apply(matrices, type, model);
        matrices.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));

        Map<Integer, List<PendingFace>> byTexture = new HashMap<>();
        for (CosmeticBone bone : roots) {
            collectBone(matrices, model, bone, byTexture);
        }
        if (byTexture.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = RenderState.cameraPos;

        for (Map.Entry<Integer, List<PendingFace>> entry : byTexture.entrySet()) {
            CosmeticTextureSlot slot = model.getTextureSlot(entry.getKey());
            if (slot == null) continue;
            if (!slot.isLoaded()) {
                model.ensureTextureSlotLoaded(slot);
            }
            if (!slot.isLoaded()) continue;

            AbstractTexture texture = mc.getTextureManager().getTexture(slot.getId());
            if (texture == null || texture.getTextureView() == null || texture.getSampler() == null) continue;

            Renderer3D.BatchBindings bindings = Renderer3D.BatchBindings.none()
                    .withSampler("u_Texture", texture.getTextureView(), texture.getSampler());
            MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_TEXTURED, Renderer3D.DepthMode.MAIN, bindings);
            if (mesh == null) continue;

            for (PendingFace face : entry.getValue()) {
                mesh.ensureQuadCapacity();

                int i1 = mesh.vec3(face.xs[0] - cam.x, face.ys[0] - cam.y, face.zs[0] - cam.z)
                        .raw2(face.us[0], face.vs[0]).color(255, 255, 255, 255).next();
                int i2 = mesh.vec3(face.xs[1] - cam.x, face.ys[1] - cam.y, face.zs[1] - cam.z)
                        .raw2(face.us[1], face.vs[1]).color(255, 255, 255, 255).next();
                int i3 = mesh.vec3(face.xs[2] - cam.x, face.ys[2] - cam.y, face.zs[2] - cam.z)
                        .raw2(face.us[2], face.vs[2]).color(255, 255, 255, 255).next();
                int i4 = mesh.vec3(face.xs[3] - cam.x, face.ys[3] - cam.y, face.zs[3] - cam.z)
                        .raw2(face.us[3], face.vs[3]).color(255, 255, 255, 255).next();

                mesh.quad(i1, i2, i3, i4);
            }
        }
    }

    private static void collectBone(PoseStack matrices, CosmeticModel model, CosmeticBone bone,
                                    Map<Integer, List<PendingFace>> out) {
        if (model.shouldSkipBone(bone)) return;

        matrices.pushPose();
        matrices.last().pose().mul(bone.buildMatrix());

        for (CosmeticCube cube : bone.getCubes()) {
            collectCube(matrices, cube, out);
        }
        for (CosmeticBone child : bone.getChildren()) {
            collectBone(matrices, model, child, out);
        }

        matrices.popPose();
    }

    private static void collectCube(PoseStack matrices, CosmeticCube cube, Map<Integer, List<PendingFace>> out) {
        matrices.pushPose();

        Vector3f origin = cube.getOrigin();
        Vector3f rotation = cube.getRotation();
        if (rotation.x != 0f || rotation.y != 0f || rotation.z != 0f) {
            matrices.translate(origin.x / UNITS_PER_BLOCK, origin.y / UNITS_PER_BLOCK, origin.z / UNITS_PER_BLOCK);
            matrices.mulPose(Axis.XP.rotationDegrees(rotation.x));
            matrices.mulPose(Axis.YP.rotationDegrees(rotation.y));
            matrices.mulPose(Axis.ZP.rotationDegrees(rotation.z));
            matrices.translate(-origin.x / UNITS_PER_BLOCK, -origin.y / UNITS_PER_BLOCK, -origin.z / UNITS_PER_BLOCK);
        }

        float inflate = cube.getInflate();
        float x1 = (cube.getFrom().x - inflate) / UNITS_PER_BLOCK;
        float y1 = (cube.getFrom().y - inflate) / UNITS_PER_BLOCK;
        float z1 = (cube.getFrom().z - inflate) / UNITS_PER_BLOCK;
        float x2 = (cube.getTo().x + inflate) / UNITS_PER_BLOCK;
        float y2 = (cube.getTo().y + inflate) / UNITS_PER_BLOCK;
        float z2 = (cube.getTo().z + inflate) / UNITS_PER_BLOCK;

        Matrix4f m = new Matrix4f(matrices.last().pose());
        CosmeticCube.CubeFace[] faces = cube.getFaces();
        int tw = cube.getTextureWidth();
        int th = cube.getTextureHeight();

        for (int side = 0; side < 6; side++) {
            CosmeticCube.CubeFace face = faces[side];
            if (face == null) continue;
            addFace(m, face, tw, th, x1, y1, z1, x2, y2, z2, side, out);
        }

        matrices.popPose();
    }

    private static void addFace(Matrix4f m, CosmeticCube.CubeFace face, float tw, float th,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                int side, Map<Integer, List<PendingFace>> out) {
        PendingFace quad = switch (side) {
            case 0 -> quad(m,
                    x2, y2, z1, x1, y2, z1, x1, y1, z1, x2, y1, z1);
            case 1 -> quad(m,
                    x2, y2, z2, x2, y2, z1, x2, y1, z1, x2, y1, z2);
            case 2 -> quad(m,
                    x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2);
            case 3 -> quad(m,
                    x1, y2, z1, x1, y2, z2, x1, y1, z2, x1, y1, z1);
            case 4 -> quad(m,
                    x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1);
            case 5 -> quad(m,
                    x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
            default -> null;
        };
        if (quad == null) return;

        float u1 = face.getU1() / tw;
        float v1 = face.getV1() / th;
        float u2 = face.getU2() / tw;
        float v2 = face.getV2() / th;
        quad.us[0] = u1;
        quad.vs[0] = v1;
        quad.us[1] = u2;
        quad.vs[1] = v1;
        quad.us[2] = u2;
        quad.vs[2] = v2;
        quad.us[3] = u1;
        quad.vs[3] = v2;

        out.computeIfAbsent(face.getTextureIndex(), k -> new ArrayList<>()).add(quad);
    }

    private static PendingFace quad(Matrix4f m,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4) {
        PendingFace face = new PendingFace();
        Vector3f p1 = m.transformPosition(new Vector3f(x1, y1, z1));
        Vector3f p2 = m.transformPosition(new Vector3f(x2, y2, z2));
        Vector3f p3 = m.transformPosition(new Vector3f(x3, y3, z3));
        Vector3f p4 = m.transformPosition(new Vector3f(x4, y4, z4));
        face.xs = new float[]{p1.x, p2.x, p3.x, p4.x};
        face.ys = new float[]{p1.y, p2.y, p3.y, p4.y};
        face.zs = new float[]{p1.z, p2.z, p3.z, p4.z};
        return face;
    }

    private static final class PendingFace {
        float[] xs;
        float[] ys;
        float[] zs;
        final float[] us = new float[4];
        final float[] vs = new float[4];
    }
}
