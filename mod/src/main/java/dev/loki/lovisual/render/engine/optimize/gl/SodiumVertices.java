package dev.loki.lovisual.render.engine.optimize.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;

/**
 * Stage 3 step 7: the only class touching Sodium's {@code VertexBufferWriter}
 * API — kept separate so it is loaded only when Sodium is installed
 * ({@link RawVertices} never references it on the vanilla path).
 *
 */
final class SodiumVertices {

    // tryOf is an interface instanceof plus an interface call, which thrashes HotSpot's
    // per-class type check cache when a handful of consumer classes alternate; most calls
    // ask about the same buffer as the last one
    private static Object lastWriter;

    private SodiumVertices() {
    }

    static boolean accepts(VertexConsumer consumer) {
        if (consumer == lastWriter) {
            return true;
        }
        if (VertexBufferWriter.tryOf(consumer) == null) {
            return false;
        }
        lastWriter = consumer;
        return true;
    }

    static void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
        (consumer == lastWriter
                ? (VertexBufferWriter) consumer
                : VertexBufferWriter.of(consumer)).push(stack, src, count, format);
    }
}
