package dev.loki.lovisual.features.module.modules.visuals.wings.model;

import dev.loki.lovisual.util.bedrock.geo.GeoBone;
import dev.loki.lovisual.util.bedrock.geo.GeoCube;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Draws a loaded wing model on a player's back. The anchor chain follows the vanilla player model (body
 * pivot, crouch lean) and is then moved by the catalog offsets; the bones are posed by the model's animator.
 */
public final class WingModelRenderer {
    private static final float MODEL_HEIGHT = 1.501f;
    private static final float ATTACHMENT_SHIFT = -0.3f;
    private static final float CHEST_ARMOR_OFFSET = 0.078125f;
    private static final float CROUCH_BODY_DROP = 0.2f;
    private static final float CROUCH_LEAN = 0.5f;
    private static final float FLIGHT_BLEND_PER_FRAME = 0.1f;
    private static final float CLOCK_WRAP_SECONDS = 360.0f;
    private static final long FRAME_MILLIS = 150L;
    private static final float MIN_LIGHT = 0.3f;

    /** Per-frame knobs resolved by the module. */
    public record Style(float scale, int tintRgb, boolean depthTest) {
    }

    private static final Map<UUID, Float> FLIGHT = new HashMap<>();

    private WingModelRenderer() {
    }

    public static void render(Renderer3D renderer, Player player, float tickDelta, float bodyYaw,
                              WingModels.Loaded wing, Style style) {
        WingModelDef def = wing.def();
        float seconds = (player.tickCount + tickDelta) / 20.0f % CLOCK_WRAP_SECONDS;
        wing.animator().apply(wing.model(), seconds, flight(player));

        Matrix4f anchor = anchor(player, tickDelta, bodyYaw, def, style.scale());
        MeshBuilder mesh = renderer.batchTextured(
                style.depthTest() ? LoVisualRenderPipelines.WORLD_TEXTURED_DEPTH : LoVisualRenderPipelines.WORLD_TEXTURED,
                wing.texture(), style.depthTest() ? Renderer3D.DepthMode.MAIN : Renderer3D.DepthMode.NONE);
        if (mesh == null) return;

        float light = light(player);
        int r = Math.round((style.tintRgb() >> 16 & 0xFF) * light);
        int g = Math.round((style.tintRgb() >> 8 & 0xFF) * light);
        int b = Math.round((style.tintRgb() & 0xFF) * light);
        int frame = def.frames() > 1 ? (int) ((System.currentTimeMillis() / FRAME_MILLIS) % def.frames()) : 0;
        Sink sink = new Sink(mesh, wing.model().textureWidth(), wing.model().textureHeight(), def.frames(), frame, r, g, b);
        for (GeoBone root : wing.model().roots()) draw(root, anchor, sink);
    }

    /** translate(player) · yaw · body (crouch) · flip · catalog offset · scale, in blocks. */
    static Matrix4f anchor(Player player, float tickDelta, float bodyYaw, WingModelDef def, float userScale) {
        Vec3 pos = player.getPosition(tickDelta);
        Matrix4f m = new Matrix4f()
                .translate((float) pos.x, (float) pos.y, (float) pos.z)
                .rotateY((float) Math.toRadians(180.0f - bodyYaw))
                .scale(-1.0f, -1.0f, 1.0f)
                .translate(0.0f, -MODEL_HEIGHT, 0.0f);
        if (player.isShiftKeyDown()) m.translate(0.0f, CROUCH_BODY_DROP, 0.0f).rotateX(CROUCH_LEAN);
        float back = player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0f : CHEST_ARMOR_OFFSET;
        m.rotateZ((float) Math.PI)
                .translate(0.0f, def.offsetY() + ATTACHMENT_SHIFT, def.offsetZ() + back)
                .scale(def.scale() * userScale);
        return m;
    }

    private static void draw(GeoBone bone, Matrix4f parent, Sink sink) {
        Matrix4f matrix = bone.transform(new Matrix4f(parent));
        for (GeoCube cube : bone.cubes) {
            Matrix4f cubeMatrix = cube.rotated() ? cube.transform(new Matrix4f(matrix)) : matrix;
            for (float[] face : cube.faces()) sink.quad(cubeMatrix, face);
        }
        for (GeoBone child : bone.children) draw(child, matrix, sink);
    }

    private static float flight(Player player) {
        float target = player.isFallFlying() ? 1.0f : 0.0f;
        float current = FLIGHT.getOrDefault(player.getUUID(), target);
        current += Math.max(-FLIGHT_BLEND_PER_FRAME, Math.min(FLIGHT_BLEND_PER_FRAME, target - current));
        FLIGHT.put(player.getUUID(), current);
        return current;
    }

    private static float light(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 1.0f;
        int level = mc.level.getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition()));
        return MIN_LIGHT + (1.0f - MIN_LIGHT) * (level / 15.0f);
    }

    /** Writes transformed quads to the mesh, mapping uv to the current texture frame. */
    private static final class Sink {
        private final MeshBuilder mesh;
        private final float texW;
        private final float texH;
        private final int frames;
        private final int frame;
        private final int r;
        private final int g;
        private final int b;
        private final Vector3f point = new Vector3f();

        Sink(MeshBuilder mesh, int texW, int texH, int frames, int frame, int r, int g, int b) {
            this.mesh = mesh;
            this.texW = texW;
            this.texH = texH;
            this.frames = frames;
            this.frame = frame;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        void quad(Matrix4f matrix, float[] face) {
            mesh.ensureQuadCapacity();
            int[] index = new int[4];
            for (int i = 0; i < 4; i++) {
                point.set(face[i * 5] / 16.0f, face[i * 5 + 1] / 16.0f, face[i * 5 + 2] / 16.0f);
                matrix.transformPosition(point);
                float u = face[i * 5 + 3] / texW;
                float v = (face[i * 5 + 4] / texH + frame) / frames;
                index[i] = mesh.vec3(point.x, point.y, point.z).vec2(u, v).color(r, g, b, 255).next();
            }
            mesh.quad(index[0], index[1], index[2], index[3]);
        }
    }
}
