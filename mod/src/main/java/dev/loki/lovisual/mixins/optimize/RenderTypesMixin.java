package dev.loki.lovisual.mixins.optimize;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.gl.RenderBackendOps;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiFunction;

/**
 * Stage 3b: cache {@code RenderTypes.entityTranslucent(id, flag=true)} by
 * identifier — vanilla uses {@code Util.memoize} keyed on {@code Pair(id, flag)},
 * which {@code Objects.hash}-es the pair on every call, per entity per frame.
 * When the flag is {@code true} (the common case — translucent skins, held
 * items, etc.), the result depends only on the identifier, so we cache it
 * directly in a {@code ConcurrentHashMap}.
 *
 * <p>Ported from {@code zako.opt.mixin.gl.RenderTypesMixin} (ZakoOpt, LGPL-3.0).
 * Original author: Zako — https://t.me/StarikZako. The original cache logic
 * is wrapped in {@link RenderBackendOps}; LoVisual's GL implementation is
 * {@code GlRenderBackendOps}, which gates on {@code microOpts} (Stage 1 toggle).
 *
 * <p>When the toggle is off, the WrapOperation calls the original — vanilla
 * behavior preserved. When the toggle is on and the flag is {@code true},
 * the cache is consulted; on a miss, the original is called and the result
 * is stored.
 */
@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {

    @WrapOperation(
            method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private static Object lovisual$cacheTranslucent(
            BiFunction<Object, Object, Object> memo, Object id, Object flag, Operation<Object> original) {
        RenderBackendOps backend = RenderBackendOps.current();
        if (backend == null || !backend.renderTypesCacheSupported()) {
            return original.call(memo, id, flag);
        }
        // Only cache when the flag is true — the false case depends on the flag too.
        if (!(Boolean) flag) {
            return original.call(memo, id, flag);
        }
        Identifier mcId = (Identifier) id;
        RenderBackendOps.Identifier cacheKey = new RenderBackendOps.Identifier(mcId.getNamespace(), mcId.getPath());
        Object cached = backend.renderTypeFor(cacheKey);
        if (cached != null) {
            return cached;
        }
        Object result = original.call(memo, id, flag);
        if (result instanceof RenderType) {
            backend.cacheRenderType(cacheKey, result);
        }
        return result;
    }
}
