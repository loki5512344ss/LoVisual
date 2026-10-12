package dev.loki.lovisual.render.engine.optimize.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.system.MemoryStack;

/**
 * Stage 3 step 7: dispatches ready vertex bytes into a consumer — through
 * Sodium's {@code VertexBufferWriter} when Sodium is installed, else into a
 * vanilla {@code BufferBuilder} via {@link RawBuffer}.
 *
 *
 * <p>Optional-dependency pattern: when Sodium is absent the {@code SODIUM}
 * flag short-circuits before {@link SodiumVertices} is ever touched, so the
 * Sodium API classes are never loaded.
 */
public enum RawVertices {
    ;

    private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");

    /** True when this consumer can receive raw vertices in {@code format}. */
    public static boolean accepts(VertexConsumer consumer, VertexFormat format) {
        return SODIUM
                ? SodiumVertices.accepts(consumer)
                : consumer instanceof RawBuffer raw && raw.lovisual$accepts(format);
    }

    /** Push {@code count} whole vertices from native address {@code src}. */
    public static void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
        if (SODIUM) {
            SodiumVertices.push(consumer, stack, src, count, format);
        } else {
            ((RawBuffer) consumer).lovisual$push(src, count);
        }
    }
}
