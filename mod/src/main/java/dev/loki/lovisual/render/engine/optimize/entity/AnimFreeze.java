package dev.loki.lovisual.render.engine.optimize.entity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.loki.lovisual.features.module.modules.misc.optimize.entity.StateEntity;
import dev.loki.lovisual.render.engine.optimize.gl.RawVertices;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stage 3 step 4: far-entity animation freeze. A distant entity's model
 * vertices are captured once per game tick and, on the frames in between,
 * replayed as raw vertex copies shifted to the entity's current position —
 * the model is no longer rebuilt (and re-animated) every frame, only moved.
 *
 * <p>Distance gate: farther than {@link #distance()} blocks from the camera
 * (squared comparison against the pose translation). Close entities render
 * through the vanilla path untouched.
 *
 * <p>The capture is keyed per {@code (model, renderType, sprite)} triplet per
 * entity — one entity may submit the same model several times per frame
 * (body + armor layers); the n-th submission reuses the n-th capture. Parts
 * that are rebuilt every frame elsewhere (e.g. armor trim shimmer) are NOT
 * replayed — callers decide via {@link #freezes}, so those parts stay in sync
 * with the frozen rest instead of drifting.
 *
 * <p>Requires the raw-vertex push path ({@code RawVertices} + Sodium or the
 * vanilla {@code RawBuffer} mixin); when neither consumer accepts raw bytes,
 * {@link #render} returns {@code false} and vanilla runs.
 *
 * <p>Toggle: {@link #configure(boolean, int)}, pushed by the Optimize module
 * once per frame. The frame counter advances from the {@code renderFrame}
 * hook in {@code MinecraftMixin}.
 */
public enum AnimFreeze {
    ;

    /** Advances once per rendered frame; captures are reused within one frame. */
    public static volatile long frame;

    private static final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(16384);
    private static ByteBuffer emitBuffer = MemoryUtil.memAlloc(16384);

    private static volatile boolean enabled;
    private static volatile int distance = 16;

    /** Push a full snapshot — called by the Optimize module once per frame. */
    public static void configure(boolean enable, int distanceBlocks) {
        enabled = enable;
        distance = distanceBlocks;
    }

    /** Reset to "off" — used by {@code Optimize.onDisable}. */
    public static void reset() {
        configure(false, 0);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int distance() {
        return distance;
    }

    /**
     * True when this (state, pose) pair belongs to a far entity whose parts
     * must all replay the frozen capture — parts built every frame elsewhere
     * consult this so they don't drift off the frozen rest.
     */
    public static boolean freezes(Object state, Matrix4f pose) {
        if (!enabled || !(state instanceof EntityRenderState s)
                || ((StateEntity) s).lovisual$entity() == null) {
            return false;
        }
        float d = distance;
        return beyondDistance(pose.m30(), pose.m31(), pose.m32(), d);
    }

    /**
     * Capture-once-per-tick / replay-in-between for one model submission.
     *
     * @return {@code false} when not frozen (caller renders as usual)
     */
    public static boolean render(Entity entity, Model<?> model, RenderType type, TextureAtlasSprite sprite,
                                 Matrix4f pose, VertexConsumer target, Consumer<VertexConsumer> draw) {
        float x = pose.m30(), y = pose.m31(), z = pose.m32();
        float d = distance;
        if (!enabled || !beyondDistance(x, y, z, d) || !RawVertices.accepts(target, type.format())) {
            return false;
        }
        AnimHolder owner = (AnimHolder) entity;
        Holder holder = owner.lovisual$anim();
        if (holder == null) {
            holder = new Holder();
            owner.lovisual$anim(holder);
        }
        Capture capture = find(holder, model, type, sprite);
        long tick = entity.level().getGameTime();
        if (capture.tick != tick) {
            BufferBuilder builder = new BufferBuilder(SCRATCH, type.primitiveTopology(), type.format());
            draw.accept(sprite == null ? builder : sprite.wrap(builder));
            capture.count = 0;
            try (MeshData mesh = builder.build()) {
                if (mesh != null) {
                    ByteBuffer src = mesh.vertexBuffer();
                    int bytes = src.remaining();
                    if (capture.data == null || capture.data.capacity() < bytes) {
                        capture.data = ByteBuffer.allocateDirect(bytes * 2);
                    }
                    MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress0(capture.data), bytes);
                    capture.count = mesh.drawState().vertexCount();
                }
            }
            capture.tick = tick;
            capture.x = x;
            capture.y = y;
            capture.z = z;
        }
        if (capture.count > 0) {
            emit(capture, type.format(), x - capture.x, y - capture.y, z - capture.z, target);
        }
        return true;
    }

    /** Squared distance from the origin — shared by {@link #freezes} and {@link #render}. */
    static boolean beyondDistance(float x, float y, float z, float dist) {
        return x * x + y * y + z * z > dist * dist;
    }

    /** Per-entity bag of captures. */
    public static final class Holder {
        final List<Capture> captures = new ArrayList<>(4);

        Holder() {
        }
    }

    static final class Capture {
        Object model;
        RenderType type;
        TextureAtlasSprite sprite;
        int ordinal;
        long usedFrame = -1;
        long tick = Long.MIN_VALUE;
        ByteBuffer data;
        int count;
        float x, y, z;
    }

    /** The same model may be submitted several times per entity; the n-th call this frame reuses the n-th capture. */
    private static Capture find(Holder holder, Object model, RenderType type, TextureAtlasSprite sprite) {
        int ordinal = 0;
        for (Capture c : holder.captures) {
            if (c.model == model && c.type == type && c.sprite == sprite && c.usedFrame == frame) {
                ordinal++;
            }
        }
        for (Capture c : holder.captures) {
            if (c.model == model && c.type == type && c.sprite == sprite && c.ordinal == ordinal) {
                c.usedFrame = frame;
                return c;
            }
        }
        Capture c = new Capture();
        c.model = model;
        c.type = type;
        c.sprite = sprite;
        c.ordinal = ordinal;
        c.usedFrame = frame;
        holder.captures.add(c);
        return c;
    }

    private static void emit(Capture capture, VertexFormat format, float dx, float dy, float dz, VertexConsumer target) {
        int stride = format.getVertexSize();
        int pos = format.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME).offset();
        int size = capture.count * stride;
        if (emitBuffer.capacity() < size) {
            emitBuffer = MemoryUtil.memRealloc(emitBuffer, size * 2);
        }
        long dst = MemoryUtil.memAddress0(emitBuffer);
        MemoryUtil.memCopy(MemoryUtil.memAddress0(capture.data), dst, size);
        for (long v = dst + pos, end = dst + size; v < end; v += stride) {
            MemoryUtil.memPutFloat(v, MemoryUtil.memGetFloat(v) + dx);
            MemoryUtil.memPutFloat(v + 4, MemoryUtil.memGetFloat(v + 4) + dy);
            MemoryUtil.memPutFloat(v + 8, MemoryUtil.memGetFloat(v + 8) + dz);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            RawVertices.push(target, stack, dst, capture.count, format);
        }
    }
}
