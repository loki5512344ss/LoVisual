package dev.loki.lovisual.render.engine.optimize.gl;

import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Stage 3 step 7: a {@code BufferBuilder} that accepts ready-made vertex
 * bytes pushed straight from memory — what Sodium's {@code VertexBufferWriter}
 * does, reimplemented for the vanilla builder so optimizations work without
 * Sodium.
 *
 * Implemented on {@code BufferBuilder} by {@code BufferBuilderRawMixin}.
 */
public interface RawBuffer {

    /** True when this buffer's vertex format is exactly {@code format}. */
    boolean lovisual$accepts(VertexFormat format);

    /**
     * Copy {@code count} whole vertices from {@code src} (a native address)
     * into the buffer.
     */
    void lovisual$push(long src, int count);
}
