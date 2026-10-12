package dev.loki.lovisual.mixins.optimize.entity.anim;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.lovisual.features.module.modules.misc.optimize.entity.StateEntity;
import dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze;
import dev.loki.lovisual.render.engine.optimize.parallel.ParallelModelHook;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stage 3 step 4: the AnimFreeze call site. When a far entity's model part is
 * about to render, hand the submission to {@link AnimFreeze#render} — it
 * replays the once-per-tick raw vertex capture shifted to the current
 * position; on success the vanilla {@code renderToBuffer} is skipped.
 *
 * <p>The buffer may already be sprite-wrapped; the replayed vertices go
 * through it, so Sodium's push remaps their UVs. Sheeted decals (block
 * cracks) never freeze — their pose is built elsewhere.
 */
@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererMixin {

    @Inject(method = "buildGroup", at = @At("HEAD"), cancellable = true)
    private void lovisual$parallel(FeatureFrameContext context, List<ModelFeatureRenderer.Submit<?>> submits,
                                   CallbackInfo ci) {
        if (ParallelModelHook.buildGroup((ModelFeatureRenderer) (Object) this, submits)) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "prepareModel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void lovisual$frozenAnimation(Model<?> model, PoseStack poseStack, VertexConsumer consumer,
                                          int light, int overlay, int color, Operation<Void> original,
                                          ModelFeatureRenderer.Submit<?> submit) {
        if (AnimFreeze.enabled() && submit.sheetedDecalPose() == null
                && submit.state() instanceof EntityRenderState state) {
            Entity entity = ((StateEntity) state).lovisual$entity();
            if (entity != null && AnimFreeze.render(entity, model, submit.renderType(), null,
                    poseStack.last().pose(), consumer,
                    target -> original.call(model, poseStack, target, light, overlay, color))) {
                return;
            }
        }
        original.call(model, poseStack, consumer, light, overlay, color);
    }
}
