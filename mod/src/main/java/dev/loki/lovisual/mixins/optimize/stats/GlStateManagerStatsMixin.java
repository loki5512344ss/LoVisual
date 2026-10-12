package dev.loki.lovisual.mixins.optimize.stats;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.loki.lovisual.render.engine.optimize.profile.DrawStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts framebuffer binds that actually reach the driver: the injection sits
 * on the {@code glBindFramebuffer} calls inside {@code _glBindFramebuffer}, so
 * binds GlStateManager's read/write cache absorbs are not counted. A target of
 * {@code GL_FRAMEBUFFER} runs both branches and is counted twice — two GL calls
 * really happen.
 *
 * <p>Part of Stage 3 step 1 in {@code mod/docs/optimize-layer-design.md}.
 *
 */
@Mixin(GlStateManager.class)
public abstract class GlStateManagerStatsMixin {

    @Inject(
            method = "_glBindFramebuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL33C;glBindFramebuffer(II)V"
            )
    )
    private static void lovisual$countBind(int target, int framebuffer, CallbackInfo ci) {
        DrawStats.bind();
    }
}
