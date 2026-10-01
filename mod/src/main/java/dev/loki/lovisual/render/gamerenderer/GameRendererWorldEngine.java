package dev.loki.lovisual.render.gamerenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.addon.pipeline.AddonRenderPipelineManager;
import dev.loki.lovisual.api.v0.render.LoVisualRenderStage;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.profiler.bridge.TracyGpuProfiler;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.render.RenderProfiler3D;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.iris.state.IrisLoVisualFrameHooks;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * World-render engine bodies extracted from GameRendererMixin: the
 * BEFORE_TRANSLUCENT/END_MAIN world pass and the post-process world pass.
 */
public final class GameRendererWorldEngine {

    private final GameRendererWorldDebug debug = new GameRendererWorldDebug();
    private final PoseStack matrices = new PoseStack();
    private final PoseStack postMatrices = new PoseStack();
    private final Matrix4f cameraPositionScratch = new Matrix4f();
    private final Matrix4f modelViewScratch = new Matrix4f();
    private Renderer3D renderer;
    private Renderer3D depthRenderer;
    private Renderer3D postRenderer;
    private Renderer3D postDepthRenderer;

    public interface Host extends GameRendererProjectionHelper.BobInvoker {
        Minecraft lovisual$minecraft();

        Camera lovisual$mainCamera();

        GameRendererProjectionHelper lovisual$projection();

        float lovisual$spinningTime();

        float lovisual$spinningSpeed();
    }

    private record Frame(float tickDelta, float fov, Matrix4f position, Matrix4f capturedProjection,
                         boolean useCapturedMatrices) {}
    private record PhasePlan(RenderPhase phase, WorldPhase worldPhase, LoVisualRenderStage stage,
                             String moduleScope, String section, String flushScope, String flushSection,
                             boolean setupFrameState) {}

    private static final PhasePlan BEFORE_TRANSLUCENT = new PhasePlan(
            RenderPhase.WORLD_BEFORE_TRANSLUCENT, WorldPhase.BEFORE_TRANSLUCENT,
            LoVisualRenderStage.WORLD_BEFORE_TRANSLUCENT, "3d:modules_before_translucent",
            "modules_before_translucent", "3d:flush_before_translucent", "flush_before_translucent", true);
    private static final PhasePlan END_MAIN = new PhasePlan(
            RenderPhase.WORLD_AFTER_TRANSLUCENT, WorldPhase.END_MAIN,
            LoVisualRenderStage.WORLD_END_MAIN, "3d:modules_end_main",
            "modules_end_main", "3d:flush_end_main", "flush_end_main", false);
    private static final PhasePlan AFTER_POST_PROCESS = new PhasePlan(
            RenderPhase.WORLD_POST_PRE_HAND, WorldPhase.AFTER_POST_PROCESS,
            LoVisualRenderStage.WORLD_POST_PROCESS, "3d:post_modules",
            "modules", "3d:post_flush", "flush", true);

    public void renderWorld(Host host, DeltaTracker tickCounter) {
        try (ProfilerPhase.Scope phaseScope = ProfilerPhase.scope("3d:world");
             TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("3d:world")) {
            Frame frame = prepareFrame(host, tickCounter);
            RenderState.cameraLook = Vec3.directionFromRotation(RenderState.cameraPitch, RenderState.cameraYaw).normalize();
            RenderState.cameraSubmersion = host.lovisual$mainCamera().getFluidInCamera();
            RenderState.worldTranslucent = host.lovisual$projection().isWorldTranslucent(RenderState.cameraSubmersion);
            RenderState.frustum = host.lovisual$mainCamera().getCapturedFrustum();
            if (debug.clipSpaceIfNeeded()) return;

            if (renderer == null) {
                renderer = new Renderer3D(LoVisualRenderPipelines.WORLD_COLORED_LINES, LoVisualRenderPipelines.WORLD_COLORED);
            }
            if (depthRenderer == null) {
                depthRenderer = new Renderer3D(LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH, LoVisualRenderPipelines.WORLD_COLORED_DEPTH);
            }
            Matrix4fStack mv = RenderSystem.getModelViewStack();
            mv.pushMatrix();
            if (IrisLoVisualFrameHooks.isRenderingAfterIrisFinalization()) {
                mv.identity();
            }
            mv.mul(frame.position());
            matrices.pushPose();
            Matrix4f projection = buildProjection(host, frame, matrices);
            startFrame(tickCounter, frame, projection, mv);
            renderPhase(BEFORE_TRANSLUCENT, host, frame, matrices, projection, mv, renderer, depthRenderer,
                    () -> debug.primitiveIfNeeded(renderer, depthRenderer, host.lovisual$mainCamera(), projection));
            // Start a new batch for END_MAIN so overlays render above grass.
            renderer.begin();
            depthRenderer.begin();
            renderPhase(END_MAIN, host, frame, matrices, projection, mv, renderer, depthRenderer, null);
            matrices.popPose();
            mv.popMatrix();
            RenderState.rendering3D = false;
        }
    }

    public void renderPostWorld(Host host, DeltaTracker tickCounter) {
        try (ProfilerPhase.Scope phaseScope = ProfilerPhase.scope("3d:world_post");
             TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("3d:world_post")) {
            Frame frame = prepareFrame(host, tickCounter);
            if (postRenderer == null) {
                postRenderer = new Renderer3D(LoVisualRenderPipelines.WORLD_COLORED_LINES, LoVisualRenderPipelines.WORLD_COLORED);
            }
            if (postDepthRenderer == null) {
                postDepthRenderer = new Renderer3D(LoVisualRenderPipelines.WORLD_COLORED_LINES_DEPTH, LoVisualRenderPipelines.WORLD_COLORED_DEPTH);
            }
            Matrix4fStack mv = RenderSystem.getModelViewStack();
            mv.pushMatrix();
            if (IrisLoVisualFrameHooks.isRenderingAfterIrisFinalization()) {
                mv.identity();
            }
            mv.mul(frame.position());
            postMatrices.pushPose();
            Matrix4f projection = buildProjection(host, frame, postMatrices);
            startFrame(tickCounter, frame, projection, mv);
            renderPhase(AFTER_POST_PROCESS, host, frame, postMatrices, projection, mv, postRenderer, postDepthRenderer, null);
            postMatrices.popPose();
            mv.popMatrix();
            RenderState.rendering3D = false;
        }
    }

    private Frame prepareFrame(Host host, DeltaTracker tickCounter) {
        Camera camera = host.lovisual$mainCamera();
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
        Matrix4f capturedPosition = LoVisualWorldMatrices.positionMatrix();
        Matrix4f capturedProjection = LoVisualWorldMatrices.renderProjectionMatrix();
        boolean useCaptured = capturedPosition != null && capturedProjection != null;
        Matrix4f position = useCaptured
                ? capturedPosition
                : cameraPositionScratch.identity().rotation(camera.rotation().conjugate(host.lovisual$projection().conjugateScratch()));
        RenderState.rendering3D = true;
        RenderState.tickDelta = tickDelta;
        Vec3 capturedCameraPos = LoVisualWorldMatrices.cameraPosition();
        RenderState.cameraPos = capturedCameraPos != null ? capturedCameraPos : camera.position();
        RenderState.cameraRotation.set(camera.rotation());
        RenderState.cameraYaw = camera.yRot();
        RenderState.cameraPitch = camera.xRot();
        return new Frame(tickDelta, camera.getFov(), position, capturedProjection, useCaptured);
    }

    private Matrix4f buildProjection(Host host, Frame frame, PoseStack stack) {
        if (frame.useCapturedMatrices()) return frame.capturedProjection();
        return host.lovisual$projection().buildWorldProjection(host.lovisual$mainCamera(), frame.fov(), stack,
                frame.tickDelta(), host.lovisual$spinningTime(), host.lovisual$spinningSpeed(), host.lovisual$minecraft(), host);
    }

    private void startFrame(DeltaTracker tickCounter, Frame frame, Matrix4f projection, Matrix4fStack mv) {
        MeshRenderer.setProjection(projection);
        LoVisualRenderSystem.beginFrame(frame.tickDelta(), tickCounter.getGameTimeDeltaTicks(),
                tickCounter.getRealtimeDeltaTicks(), projection, modelViewScratch.set(mv));
    }

    private void renderPhase(PhasePlan plan, Host host, Frame frame, PoseStack stack, Matrix4f projection,
                             Matrix4fStack mv, Renderer3D renderer3d, Renderer3D depthRenderer3d,
                             Runnable beforeModules) {
        try (RenderPhaseScope phaseScope = LoVisualRenderSystem.phase(plan.phase())) {
            if (plan.setupFrameState()) {
                RenderState.worldProjection.set(projection);
                RenderState.frustum = new Frustum(frame.position(), projection);
                RenderState.frustum.prepare(RenderState.cameraPos.x, RenderState.cameraPos.y, RenderState.cameraPos.z);
                if (!frame.useCapturedMatrices()) {
                    mv.mul(host.lovisual$projection().inverseBobScratch().set(stack.last().pose()).invert());
                }
                renderer3d.begin();
                depthRenderer3d.begin();
            }
            if (beforeModules != null) beforeModules.run();
            try (ProfilerPhase.Scope scope = ProfilerPhase.scope(plan.moduleScope());
                 RenderProfiler3D.Section ignored = RenderProfiler3D.section(plan.section())) {
                ModuleManager.renderWorldEngine(plan.worldPhase(), renderer3d, depthRenderer3d, frame.tickDelta());
                AddonRenderPipelineManager.render3D(plan.stage(), plan.worldPhase(), renderer3d, depthRenderer3d,
                        stack, frame.tickDelta());
            }
            try (ProfilerPhase.Scope flushScope = ProfilerPhase.scope(plan.flushScope());
                 TracyGpuProfiler.Scope gpuFlush = TracyGpuProfiler.beginZone(plan.flushScope());
                 RenderProfiler3D.Section ignored = RenderProfiler3D.section(plan.flushSection())) {
                renderer3d.render(stack);
                depthRenderer3d.render(stack);
            }
        }
    }
}
