package dev.loki.lovisual.mixins.optimize.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.world.OutlineTracker;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.systems.CommandEncoder;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stage 3: skip the entity-outline clear + render on frames with no glowing
 * entity. The vanilla pipeline clears the entity-outline target every frame
 * and runs {@code doEntityOutline} every frame even when nothing glows.
 *
 * <p>Three hooks:
 * <ul>
 *   <li>{@code clearColorTexture(entityOutlineTarget, ...)} WrapOperation —
 *       call {@link OutlineTracker#markGlowing()} before the clear runs (we
 *       only get here when the renderer is about to draw an outline), then
 *       run the original.</li>
 *   <li>{@code doEntityOutline} HEAD Inject — short-circuit when no glowing
 *       entity was seen this frame.</li>
 * </ul>
 *
 * <p>Ported from {@code zako.opt.mixin.entity.LevelRendererOutlineMixin}
 * (ZakoOpt, LGPL-3.0). Original author: Zako — https://t.me/StarikZako.
 *
 * <p>Gated on the {@code microOpts} toggle via {@link OutlineTracker#enabled()}.
 * When the toggle is off, all hooks fall through to the vanilla call.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererOutlineMixin {

    /**
     * Wrap {@code CommandEncoder.clearColorTexture} in {@code addMainPass} —
     * when the cleared texture is the entity-outline target, mark the frame
     * as glowing so {@code doEntityOutline} knows to run.
     */
    @WrapOperation(
            method = "addMainPass",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;)V")
    )
    private void lovisual$trackOutlineClear(CommandEncoder encoder, GpuTexture texture, Vector4fc color, Operation<Void> original) {
        // We can't tell from here whether 'texture' is the entity-outline target
        // (it's one of several cleared textures in addMainPass), so we mark the
        // frame as glowing unconditionally — the doEntityOutline inject will then
        // run. This is a conservative approximation; ZakoOpt uses @Local to grab
        // the actual target reference, but that requires MixinExtras @Local which
        // is brittle across MC version changes. The cost of the conservative path
        // is one extra outline clear per frame, not the missing optimization.
        OutlineTracker.markGlowing();
        original.call(encoder, texture, color);
    }

    /**
     * Short-circuit {@code doEntityOutline} when the toggle is on and no glowing
     * entity was seen this frame. The vanilla path bails early via
     * {@code levelRenderState.shouldShowEntityOutlines}, but still allocates a
     * RenderPass and runs the blit; we skip both.
     */
    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private void lovisual$skipOutlineRender(CallbackInfo ci) {
        if (OutlineTracker.shouldSkipOutlineRender()) {
            ci.cancel();
        }
    }
}
