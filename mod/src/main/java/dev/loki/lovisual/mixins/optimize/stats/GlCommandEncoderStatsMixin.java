package dev.loki.lovisual.mixins.optimize.stats;

import dev.loki.lovisual.render.engine.optimize.profile.DrawStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts the four places {@code GlCommandEncoder} actually issues a draw —
 * {@code executeDraw}, {@code executeDraws}, {@code executeDrawIndirect} and
 * {@code executeDrawMultiple} do not call each other, so exactly one of them
 * sees every submission.
 *
 * <p>Part of Stage 3 step 1 in {@code mod/docs/optimize-layer-design.md};
 * the counter is a boolean test unless F3 is open with Optimize on.
 *
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderStatsMixin {

    @Inject(method = "executeDraw", at = @At("HEAD"))
    private void lovisual$countDraw(CallbackInfo ci) {
        DrawStats.draw();
    }

    @Inject(method = "executeDraws", at = @At("HEAD"))
    private void lovisual$countDraws(CallbackInfo ci) {
        DrawStats.draw();
    }

    @Inject(method = "executeDrawIndirect", at = @At("HEAD"))
    private void lovisual$countDrawIndirect(CallbackInfo ci) {
        DrawStats.draw();
    }

    @Inject(method = "executeDrawMultiple", at = @At("HEAD"))
    private void lovisual$countDrawMultiple(CallbackInfo ci) {
        DrawStats.draw();
    }
}
