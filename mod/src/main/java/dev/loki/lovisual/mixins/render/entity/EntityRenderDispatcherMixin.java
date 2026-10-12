package dev.loki.lovisual.mixins.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.optimize.Optimize;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void lovisual$cullEntity(E entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        Optimize optimize = Modules.get(Optimize.class);
        if (optimize == null || !optimize.isEntityCullingEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) return;

        double maxDistance = optimize.entityCullDistance();
        if (entity.distanceToSqr(camX, camY, camZ) > maxDistance * maxDistance) {
            cir.setReturnValue(false);
            return;
        }

        if (!frustum.isVisible(entity.getBoundingBox())) {
            cir.setReturnValue(false);
        }
    }
}
