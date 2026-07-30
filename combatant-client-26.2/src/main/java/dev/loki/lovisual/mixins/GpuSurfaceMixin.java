/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.engine.core.CombatantRenderSystem;
import dev.loki.lovisual.render.engine.profiler.TracyGpuProfiler;
import dev.loki.lovisual.render.engine.profiler.TracyProfiler;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.util.FastFps;

@Mixin(GpuSurface.class)
public abstract class GpuSurfaceMixin {
    /**
     * This is the last safe point before Minecraft copies the main render target into the
     * window surface. Combatant queued text/UI/RHI submission must be flushed here, not in
     * present(), otherwise those draws happen after the surface blit and are not visible in
     * the frame that produced them.
     */
    @Inject(method = "blitFromTexture", at = @At("HEAD"))
    private void combatant$beforeSurfaceBlit(CommandEncoder encoder, GpuTextureView textureView, CallbackInfo info) {
        CombatantRenderSystem.endRenderSubmission();
    }

    @Inject(method = "present", at = @At("HEAD"))
    private void combatant$onPresentHead(CallbackInfo info) {
        FastFps.onFrame();
    }

    @Inject(method = "present", at = @At("TAIL"))
    private void combatant$onPresentTail(CallbackInfo info) {
        CombatantRenderSystem.onFramePresented();
        if (TracyGpuProfiler.isEnabled()) {
            TracyGpuProfiler.onFrameEnd();
        }
        if (TracyProfiler.isEnabled()) {
            Renderer2D.BatchStats stats = Renderer2D.getBatchStats();
            TracyProfiler.plotUiBatch(stats.getFrameDraws(), stats.getFrameVertices());
        }
        Renderer2D.getBatchStats().onFrameStart();
        Renderer2D.invalidateWorldGlassSource();
    }
}
