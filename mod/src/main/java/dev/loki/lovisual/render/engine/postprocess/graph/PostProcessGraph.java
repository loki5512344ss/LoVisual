/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess.graph;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.HistoryBufferManager;
import dev.loki.lovisual.render.engine.postprocess.graph.LegacyPostProcessGraphPass;
import dev.loki.lovisual.render.engine.postprocess.graph.PostProcessGraphPass;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessGraphResources;

public final class PostProcessGraph implements AutoCloseable {
    private final List<PostProcessGraphPass> passes = new ArrayList<>();
    private final PostProcessGraphResources resources = new PostProcessGraphResources();
    private final HistoryBufferManager history = new HistoryBufferManager();

    public void add(PostProcessGraphPass pass) {
        if (pass == null) return;
        for (PostProcessGraphPass existing : passes) {
            if (existing.getId().equals(pass.getId())) return;
        }
        passes.add(pass);
        passes.sort(Comparator
                .comparingInt(PostProcessGraphPass::priority)
                .thenComparing(PostProcessGraphPass::id));
    }

    public void addLegacy(PostProcessPass pass) {
        if (pass != null) add(new LegacyPostProcessGraphPass(pass));
    }

    public boolean hasActivePass(PostProcessPass.Phase phase, RenderFrameContext context) {
        for (PostProcessGraphPass pass : passes) {
            if (pass.phase() == phase && pass.enabled(context)) return true;
        }
        return false;
    }

    /**
     * Executes one postprocess phase as a ping-pong graph.
     * <p>
     * Flow:
     * main color -> graph source -> active passes -> final graph source -> main color
     */
    public boolean execute(PostProcessPass.Phase phase,
                           float tickDelta,
                           RenderFrameContext context,
                           LoVisualRhi rhi,
                           GraphCopy copy) {
        if (!hasActivePass(phase, context)) return false;
        try (RenderCostProfiler.Scope ignoredGraph = RenderCostProfiler.postPass("graph:", phase.toString())) {
            if (!resources.prepare(phase, tickDelta)) return false;

            GpuTextureView mainColor = resources.mainColor();
            GpuTextureView source = resources.currentSource();
            if (mainColor == null || source == null) return false;

            copy.copy(mainColor, source);
            resources.resetPingPong();

            boolean anyApplied = false;
            for (PostProcessGraphPass pass : passes) {
                if (pass.phase() != phase || !pass.enabled(context)) continue;
                boolean applied;
                try (RenderCostProfiler.Scope ignoredPass = RenderCostProfiler.postPass(pass.getId())) {
                    applied = pass.execute(context, rhi, resources);
                }
                if (applied) {
                    anyApplied = true;
                    resources.advancePingPong();
                }
            }

            if (!anyApplied) return false;
            GpuTextureView finalColor = resources.finalColor();
            if (finalColor != null && mainColor != null) {
                copy.copy(finalColor, mainColor);
            }
            return true;
        }
    }

    public HistoryBufferManager history() {
        return history;
    }

    public List<PostProcessGraphPass> passes() {
        return List.copyOf(passes);
    }

    public PostProcessGraphResources resources() {
        return resources;
    }

    @Override
    public void close() {
        resources.close();
    }

    @FunctionalInterface
    public interface GraphCopy {
        void copy(GpuTextureView src, GpuTextureView dst);
    }
}
