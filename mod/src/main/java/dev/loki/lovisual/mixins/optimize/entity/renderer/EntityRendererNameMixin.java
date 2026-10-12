package dev.loki.lovisual.mixins.optimize.entity.renderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.entity.CachedName;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3: caches {@code Entity.getDisplayName()} per tick. The vanilla
 * implementation rebuilds a {@link Component} (team prefix + name + suffix)
 * every call; renderers call it once per frame per entity in view. Wrapping
 * the call here keeps the per-frame cost to one identity check + read,
 * instead of a full Component rebuild + style lookup.
 *
 * <p>The cache is keyed by game tick — name parts only change on tick
 * boundaries (team membership, name change). On the first call of a tick the
 * original runs and the result is stored; subsequent calls in the same tick
 * return the cached instance.
 *
 * Gated on the {@code microOpts} toggle via {@link OptimizeToggles#microOpts()}.
 * When the toggle is off, the original runs unchanged.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameMixin {

    @WrapOperation(
            method = "getNameTag",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getDisplayName()Lnet/minecraft/network/chat/Component;")
    )
    private Component lovisual$cachedName(Entity entity, Operation<Component> original) {
        if (!OptimizeToggles.microOpts()) {
            return original.call(entity);
        }
        if (!(entity instanceof CachedName cache)) {
            return original.call(entity);
        }
        long tick = entity.level().getGameTime();
        if (cache.lovisual$nameTick() != tick) {
            cache.lovisual$setName(original.call(entity), tick);
        }
        return cache.lovisual$name();
    }
}
