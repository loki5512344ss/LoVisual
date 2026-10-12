package dev.loki.lovisual.mixins.optimize.gl.raw;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stage 3 step 7: caches Sodium's {@code VertexConsumerUtils.convertOrLog}
 * result. The vanilla method is an interface instanceof plus an interface
 * call on a handful of alternating consumer classes, which keeps evicting
 * HotSpot's per-class type-check cache; an identity compare against the last
 * successful consumer skips both.
 *
 * Sodium-only: registered in {@code OPTIONAL_SODIUM_MIXINS}, never applied
 * without Sodium.
 *
 * <p>Gated on the {@code micro_opts} toggle. When the toggle is off, the
 * original method runs unchanged.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils", remap = false)
public abstract class VertexConsumerUtilsMixin {

    // last consumer that passed the check; a whole model goes to one consumer, so this hits for nearly every cuboid
    @Unique
    private static volatile Object lovisual$lastWriter;

    @WrapMethod(method = "convertOrLog")
    private static VertexBufferWriter lovisual$cachedConvert(VertexConsumer consumer, Operation<VertexBufferWriter> original) {
        if (consumer == lovisual$lastWriter && OptimizeToggles.microOpts()) {
            return (VertexBufferWriter) consumer;
        }
        VertexBufferWriter writer = original.call(consumer);
        if (writer == consumer) {
            lovisual$lastWriter = consumer;
        }
        return writer;
    }
}
