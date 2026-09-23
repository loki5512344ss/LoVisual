package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class HitWaveImpactHelper {
    private static final float WAVE_WIDTH = 2.5f;
    private static final int WAVE_MAX_PER_FRAME = 400;
    private static final float WAVE_MIN_ALPHA = 0.02f;
    private static final double WAVE_OUTLINE_EPS = 0.002;
    private static final float WAVE_LINE_WIDTH_BASE = 0.8f;
    private static final float WAVE_LINE_WIDTH_BOOST = 0.6f;
    private static final int IMPACT_RING_SEGMENTS = 24;
    private final HitEffect parent;
    private final List<WaveEffect> waves = new ArrayList<>();
    private final List<ImpactRing> impacts = new ArrayList<>();

    HitWaveImpactHelper(HitEffect parent) { this.parent = parent; }
    void clear() { waves.clear(); impacts.clear(); }
    void addWave(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) waves.add(new WaveEffect(pos, GameClock.millis()));
    }
    void addImpact(Vec3 center) { impacts.add(new ImpactRing(center, GameClock.millis())); }
    void renderWave(Renderer3D renderer) {
        if (waves.isEmpty()) return;
        Iterator<WaveEffect> it = waves.iterator();
        while (it.hasNext()) {
            WaveEffect w = it.next();
            if (w.isExpired()) { it.remove(); continue; }
            w.render(renderer);
        }
    }
    void renderImpact(Renderer3D renderer) {
        if (impacts.isEmpty()) return;
        Iterator<ImpactRing> it = impacts.iterator();
        while (it.hasNext()) {
            ImpactRing r = it.next();
            if (r.isExpired()) { it.remove(); continue; }
            r.render(renderer);
        }
    }
    private class WaveEffect {
        private final BlockPos centerPos;
        private final long startTime;
        WaveEffect(BlockPos centerPos, long startTime) { this.centerPos = centerPos; this.startTime = startTime; }
        boolean isExpired() { return GameClock.millis() - startTime > Math.max(1L, parent.waveDurationMs.get()); }
        void render(Renderer3D renderer) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || renderer == null) return;
            long durationMs = Math.max(1L, parent.waveDurationMs.get());
            int maxRadius = Math.max(1, parent.waveRadius.get());
            long elapsed = GameClock.millis() - startTime;
            float progress = Mth.clamp((float) elapsed / durationMs, 0f, 1f);
            float currentRadius = progress * maxRadius;
            float globalAlpha = (float) Math.pow(1.0f - progress, 0.6);
            float minRadSq = (currentRadius - WAVE_WIDTH) * (currentRadius - WAVE_WIDTH);
            float maxRadSq = (currentRadius + 0.5f) * (currentRadius + 0.5f);
            float prevWidth = RenderState.lineWidth;
            RenderState.lineWidth = Math.max(0.5f, WAVE_LINE_WIDTH_BASE + globalAlpha * WAVE_LINE_WIDTH_BOOST);
            try {
                boolean useDepth = parent.depthTest.get();
                RenderPipeline pipeline = useDepth ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_COLORED_LINES;
                Renderer3D.DepthMode depthMode = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
                MeshBuilder mesh = renderer.batch(pipeline, depthMode);
                if (mesh == null) return;
                int rendered = 0;
                for (int x = -maxRadius; x <= maxRadius; x++) {
                    for (int z = -maxRadius; z <= maxRadius; z++) {
                        if (rendered >= WAVE_MAX_PER_FRAME) return;
                        float distSq = x * x + z * z;
                        if (distSq < minRadSq || distSq > maxRadSq) continue;
                        BlockPos checkPos = centerPos.offset(x, 0, z);
                        BlockPos renderPos = findSurface(checkPos);
                        if (renderPos == null) continue;
                        BlockState state = mc.level.getBlockState(renderPos);
                        VoxelShape shape = state.getShape(mc.level, renderPos);
                        if (shape.isEmpty()) continue;
                        rendered++;
                        float distance = (float) Math.sqrt(distSq);
                        float localAlpha = 1.0f - Math.abs(distance - currentRadius) / WAVE_WIDTH;
                        localAlpha = Math.max(0f, Math.min(1f, localAlpha)) * globalAlpha;
                        if (localAlpha <= WAVE_MIN_ALPHA) continue;
                        int colorIndex = (int) (Math.toDegrees(Math.atan2(z, x)) + 180.0);
                        int color = HitRenderUtil.applyOpacity(parent.getColorArgb(colorIndex), localAlpha);
                        HitRenderUtil.addShapeOutline(mesh, renderPos, shape, color, WAVE_OUTLINE_EPS);
                    }
                }
            } finally { RenderState.lineWidth = prevWidth; }
        }
        private BlockPos findSurface(BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            for (int y = 2; y >= -4; y--) {
                BlockPos p = pos.above(y);
                if (!mc.level.getBlockState(p).isAir() && mc.level.getBlockState(p.above()).isAir()) return p;
            }
            return null;
        }
    }
    private class ImpactRing {
        private final Vec3 center;
        private final long startTime;
        ImpactRing(Vec3 center, long startTime) { this.center = center; this.startTime = startTime; }
        boolean isExpired() { return GameClock.millis() - startTime > Math.max(1L, parent.impactDurationMs.get()); }
        void render(Renderer3D renderer) {
            long durationMs = Math.max(1L, parent.impactDurationMs.get());
            long elapsed = GameClock.millis() - startTime;
            float progress = Mth.clamp((float) elapsed / durationMs, 0f, 1f);
            float fade = (float) Math.pow(1.0 - progress, 1.4);
            if (fade <= 0.01f) return;
            float maxRadius = Math.max(0.1f, parent.impactRadius.get());
            float radius = maxRadius * (0.15f + 0.85f * progress);
            int color = parent.getColorArgb((int) (progress * 360));
            MeshBuilder ring = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE, Renderer3D.DepthMode.MAIN);
            if (ring != null) renderRing(ring, radius, fade, color);
            MeshBuilder glow = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH, Renderer3D.DepthMode.PRE_DEPTH);
            if (glow != null) renderGlowWall(glow, radius, fade * 0.6f, color);
        }
        private void renderRing(MeshBuilder mesh, float radius, float fade, int color) {
            float inner = radius * 0.72f;
            for (int i = 0; i < IMPACT_RING_SEGMENTS; i++) {
                double a0 = (Math.PI * 2.0 / IMPACT_RING_SEGMENTS) * i;
                double a1 = (Math.PI * 2.0 / IMPACT_RING_SEGMENTS) * (i + 1);
                float ix0 = (float) (Math.cos(a0) * inner);
                float iz0 = (float) (Math.sin(a0) * inner);
                float ix1 = (float) (Math.cos(a1) * inner);
                float iz1 = (float) (Math.sin(a1) * inner);
                float ox0 = (float) (Math.cos(a0) * radius);
                float oz0 = (float) (Math.sin(a0) * radius);
                float ox1 = (float) (Math.cos(a1) * radius);
                float oz1 = (float) (Math.sin(a1) * radius);
                int edgeColor = HitRenderUtil.applyOpacity(color, fade);
                int fadeColor = HitRenderUtil.applyOpacity(color, 0f);
                mesh.ensureQuadCapacity();
                int i1 = mesh.vec3(center.x + ix0, center.y + 0.02, center.z + iz0).color(new RenderColor(fadeColor)).next();
                int i2 = mesh.vec3(center.x + ix1, center.y + 0.02, center.z + iz1).color(new RenderColor(fadeColor)).next();
                int i3 = mesh.vec3(center.x + ox1, center.y + 0.02, center.z + oz1).color(new RenderColor(edgeColor)).next();
                int i4 = mesh.vec3(center.x + ox0, center.y + 0.02, center.z + oz0).color(new RenderColor(edgeColor)).next();
                mesh.quad(i1, i2, i3, i4);
            }
        }
        private void renderGlowWall(MeshBuilder mesh, float radius, float fade, int color) {
            float height = parent.impactGlowHeight.get() * (1.0f - fade * 0.3f);
            for (int i = 0; i < IMPACT_RING_SEGMENTS; i++) {
                double a0 = (Math.PI * 2.0 / IMPACT_RING_SEGMENTS) * i;
                double a1 = (Math.PI * 2.0 / IMPACT_RING_SEGMENTS) * (i + 1);
                float x0 = (float) (Math.cos(a0) * radius);
                float z0 = (float) (Math.sin(a0) * radius);
                float x1 = (float) (Math.cos(a1) * radius);
                float z1 = (float) (Math.sin(a1) * radius);
                int baseColor = HitRenderUtil.applyOpacity(color, fade);
                int topColor = HitRenderUtil.applyOpacity(color, 0f);
                mesh.ensureQuadCapacity();
                int i1 = mesh.vec3(center.x + x0, center.y, center.z + z0).color(new RenderColor(baseColor)).next();
                int i2 = mesh.vec3(center.x + x1, center.y, center.z + z1).color(new RenderColor(baseColor)).next();
                int i3 = mesh.vec3(center.x + x1, center.y + height, center.z + z1).color(new RenderColor(topColor)).next();
                int i4 = mesh.vec3(center.x + x0, center.y + height, center.z + z0).color(new RenderColor(topColor)).next();
                mesh.quad(i1, i2, i3, i4);
            }
        }
    }
}
