package dev.loki.lovisual.mixins.optimize.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.entity.CachedStack;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3: hot-path helper for Sodium's entity cuboid renderer — wraps
 * {@code MemoryStack.stackPush()} so the render thread's stack is cached by
 * identity instead of being looked up via ThreadLocal on every cuboid. Sodium
 * calls this once per cuboid per frame (thousands of times for dense entity
 * scenes), so the cached lookup is a measurable win on the hot path.
 *
 * <p>Uses {@code WrapOperation} rather than {@code @Redirect} because
 * MixinExtras 0.5.4 (which Fabric Loader 0.19.3 ships with) crashes on a
 * static {@code @Redirect} here — pinned to 0.5.5 in {@code build.gradle},
 * but the convention is kept for safety. The mixin is registered with
 * {@code remap = false} because the target is a Sodium class, not a Mojang one.
 *
 * <p>Ported from {@code zako.opt.mixin.entity.SodiumEntityRendererMixin}
 * (ZakoOpt, LGPL-3.0). Original author: Zako — https://t.me/StarikZako.
 * Gated on the {@code microOpts} toggle via {@link OptimizeToggles#microOpts()}.
 * When the toggle is off, the original runs unchanged.
 *
 * <p>Registered conditionally via {@code LoVisualMixinPlugin} — only applied
 * when Sodium is loaded. See {@code lovisual.mixins.json} entry in the
 * {@code client} array.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer", remap = false)
public abstract class SodiumEntityRendererMixin {

    @WrapOperation(
            method = "renderCuboid",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;")
    )
    private static MemoryStack lovisual$cachedStack(Operation<MemoryStack> original) {
        return OptimizeToggles.microOpts() ? CachedStack.push() : original.call();
    }
}
