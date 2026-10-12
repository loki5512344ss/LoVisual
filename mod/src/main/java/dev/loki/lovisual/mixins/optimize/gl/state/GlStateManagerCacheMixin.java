package dev.loki.lovisual.mixins.optimize.gl.state;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import dev.loki.lovisual.render.engine.optimize.gl.state.GlStateCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extended GL state cache — Stage 3 "Дополнительные GL-оптимизации" item 1 in
 * {@code mod/docs/optimize-layer-design.md}. Skips redundant driver calls that
 * vanilla's own {@code BLEND/DEPTH/CULL/SCISSOR/TEXTURES} arrays do not cover:
 * {@code _glUseProgram(int)} and {@code _glBindVertexArray(int)} reach
 * {@code GL33C} every time, with no cache.
 *
 * <p>Gated by {@link OptimizeToggles#microOpts()} — the Stage 1 "micro_opts"
 * switch already covers the TBO / RenderTypes caches, so this rides along.
 *
 * <p>Cache is invalidated on {@link GlStateManager#glDeleteProgram(int)} —
 * the driver recycles program ids, and on {@code Optimize.onDisable} /
 * resource reload via {@code GlStateCache.reset()} called from the module.
 * Under Vulkan the mixin's target class is never loaded, so no injection fires.
 */
@Mixin(GlStateManager.class)
public abstract class GlStateManagerCacheMixin {

    @Inject(method = "_glUseProgram", at = @At("HEAD"), cancellable = true)
    private static void lovisual$skipRedundantUseProgram(int program, CallbackInfo ci) {
        if (!OptimizeToggles.microOpts()) {
            GlStateCache.reset();
            return;
        }
        if (GlStateCache.programAlreadyBound(program)) {
            ci.cancel();
            return;
        }
        GlStateCache.markProgram(program);
    }

    @Inject(method = "_glBindVertexArray", at = @At("HEAD"), cancellable = true)
    private static void lovisual$skipRedundantBindVao(int vao, CallbackInfo ci) {
        if (!OptimizeToggles.microOpts()) {
            GlStateCache.reset();
            return;
        }
        if (GlStateCache.vaoAlreadyBound(vao)) {
            ci.cancel();
            return;
        }
        GlStateCache.markVao(vao);
    }

    @Inject(method = "glDeleteProgram", at = @At("HEAD"))
    private static void lovisual$invalidateOnDeleteProgram(int program, CallbackInfo ci) {
        GlStateCache.reset();
    }
}
