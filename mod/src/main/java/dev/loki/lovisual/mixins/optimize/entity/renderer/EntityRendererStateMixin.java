package dev.loki.lovisual.mixins.optimize.entity.renderer;

import com.llamalad7.mixinextras.sugar.Local;
import dev.loki.lovisual.features.module.modules.misc.optimize.entity.StateEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stage 3: captures the source {@link Entity} on every freshly-built
 * {@link EntityRenderState}. Vanilla's {@code createRenderState(entity, partial)}
 * returns a state without back-reference; this inject at RETURN stores the
 * entity on the state via the {@link StateEntity} contract so downstream
 * renderers (model feature renderer, item feature renderer, future LOD mixins)
 * can reach the live entity.
 *
 * <p>Ported from {@code zako.opt.mixin.entity.EntityRendererStateMixin}
 * (ZakoOpt, LGPL-3.0). Original author: Zako — https://t.me/StarikZako.
 * The original used {@code @ModifyReturnValue}; LoVisual uses {@code @Inject}
 * at RETURN with {@code CallbackInfoReturnable} so the mixin stays simple and
 * doesn't rely on the (more fragile across versions) MixinExtras return-value
 * transformer — the captured state is set as a side effect, not returned.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererStateMixin {

    @Inject(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private void lovisual$rememberEntity(Entity entity, float partialTick,
                                         CallbackInfoReturnable<EntityRenderState> cir,
                                         @Local(argsOnly = true) Entity localEntity) {
        EntityRenderState state = cir.getReturnValue();
        if (state instanceof StateEntity stateEntity) {
            stateEntity.lovisual$entity(localEntity);
        }
    }
}
