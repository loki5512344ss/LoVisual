package dev.loki.lovisual.render.engine.optimize.parallel;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.loki.lovisual.features.module.modules.misc.optimize.particle.Workers;
import dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze;
import dev.loki.lovisual.render.engine.optimize.gl.RawVertices;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Stage 3 step 8: model vertices built on several threads. setupAnim writes
 * the pose into a model instance shared by every entity of that kind, so the
 * render thread poses each model and snapshots its parts, workers build the
 * vertices from the snapshots while the next models are posed, and the render
 * thread then hands them to the buffers in vanilla's order. Driven per group
 * by {@code ModelFeatureRendererMixin.buildGroup} through
 * {@link ParallelModelHook}.
 *
 * <p>Mutually exclusive with {@link AnimFreeze} (freeze replays captured
 * vertices, this builds fresh ones). Off with fewer than four cores or an
 * active Iris shader pack.
 */
public enum ParallelModels {
    ;

    public static final VertexFormat FORMAT = DefaultVertexFormat.ENTITY;
    public static final int MIN_SUBMITS = 24;
    private static final int PAGE = 1 << 20;
    private static final boolean CORES_OK = Runtime.getRuntime().availableProcessors() >= 4;

    private static final Map<ModelPart, ModelGeometry> GEOMETRY = new WeakHashMap<>();
    private static final List<Job> BUILT = new ArrayList<>();
    private static final ThreadLocal<PoseStack> STACKS = ThreadLocal.withInitial(PoseStack::new);
    private static final ThreadLocal<float[]> CORNERS = ThreadLocal.withInitial(() -> new float[24]);
    // vertex memory handed out while models are still being posed, so the total
    // is not known up front; reused every frame
    private static final List<Long> PAGES = new ArrayList<>();
    private static final List<Integer> PAGE_SIZES = new ArrayList<>();
    private static int page;
    private static int pageUsed;
    private static Workers.Stream stream;
    private static volatile boolean enabled;

    public static void configure(boolean on) {
        enabled = on;
    }

    public static void reset() {
        enabled = false;
        stream = null;
        BUILT.clear();
        page = 0;
        pageUsed = 0;
        for (int i = 0; i < PAGES.size(); i++) {
            MemoryUtil.nmemFree(PAGES.get(i));
        }
        PAGES.clear();
        PAGE_SIZES.clear();
        GEOMETRY.clear();
    }

    public static boolean enabled() {
        return enabled && CORES_OK && !AnimFreeze.enabled() && !irisShaderPack();
    }

    public static void begin() {
        BUILT.clear();
        page = 0;
        pageUsed = 0;
        stream = Workers.stream(i -> ModelBuilder.build(BUILT.get(i)));
    }

    // render thread: poses the model and queues its vertices; the caller has
    // already checked the vertex format and that nothing else (outline, block
    // cracks) has to see the same model
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Job add(Model model, Object state, PoseStack.Pose pose, int light, int overlay,
                          int color, TextureAtlasSprite sprite) {
        model.setupAnim(state);
        ModelPart root = model.root();
        ModelGeometry geometry = GEOMETRY.computeIfAbsent(root, ModelGeometry::new);
        int n = geometry.parts.length;
        Job job = new Job(geometry, pose, light, overlay,
                Integer.reverseBytes(Integer.rotateLeft(color, 8)), sprite, new float[n * ModelBuilder.FIELDS], new byte[n]);
        for (int i = 0; i < n; i++) {
            ModelPart p = i == 0 ? root : geometry.parts[i];
            ModelBuilder.snapshotPose(p, job, i);
        }
        job.vertices = ModelBuilder.countVertices(geometry, job);
        job.address = reserve(job.vertices * ModelBuilder.STRIDE);
        BUILT.add(job);
        stream.publish(BUILT.size());
        return job;
    }

    // must run even when posing failed: helpers spin until the stream is finished
    public static void end() {
        Throwable error = stream.finish(BUILT.size());
        stream = null;
        if (error != null) {
            throw new RuntimeException("lovisual parallel models", error);
        }
    }

    // false: the consumer cannot take raw vertices, the caller renders this model the vanilla way
    public static boolean emit(Job job, VertexConsumer consumer, MemoryStack stack) {
        if (job.vertices == 0) {
            return true;
        }
        if (!RawVertices.accepts(consumer, FORMAT)) {
            return false;
        }
        stack.push();
        try {
            RawVertices.push(consumer, stack, job.address, job.vertices, FORMAT);
        } finally {
            stack.pop();
        }
        return true;
    }

    static PoseStack stacks() {
        return STACKS.get();
    }

    static float[] corners() {
        return CORNERS.get();
    }

    static void setCorners(float[] at) {
        CORNERS.set(at);
    }

    private static long reserve(int bytes) {
        if (page < PAGES.size() && pageUsed + bytes > PAGE_SIZES.get(page)) {
            page++;
            pageUsed = 0;
        }
        if (page == PAGES.size()) {
            int size = Math.max(PAGE, bytes);
            PAGES.add(MemoryUtil.nmemAlloc(size));
            PAGE_SIZES.add(size);
        } else if (bytes > PAGE_SIZES.get(page)) {
            MemoryUtil.nmemFree(PAGES.get(page));
            PAGES.set(page, MemoryUtil.nmemAlloc(bytes));
            PAGE_SIZES.set(page, bytes);
        }
        long address = PAGES.get(page) + pageUsed;
        pageUsed += bytes;
        return address;
    }

    private static boolean irisShaderPack() {
        try {
            if (!FabricLoader.getInstance().isModLoaded("iris")) {
                return false;
            }
            return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    public static final class Job {
        final ModelGeometry geometry;
        final PoseStack.Pose matrix;
        final int light;
        final int overlay;
        final int color;
        final TextureAtlasSprite sprite;
        final float[] pose;
        final byte[] flags;
        int vertices;
        long address;

        Job(ModelGeometry geometry, PoseStack.Pose matrix, int light, int overlay, int color,
            TextureAtlasSprite sprite, float[] pose, byte[] flags) {
            this.geometry = geometry;
            this.matrix = matrix;
            this.light = light;
            this.overlay = overlay;
            this.color = color;
            this.sprite = sprite;
            this.pose = pose;
            this.flags = flags;
        }
    }
}
