package dev.loki.lovisual.mixins.optimize.gl.raw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.loki.lovisual.render.engine.optimize.gl.RawBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Stage 3 step 7: what Sodium's {@code VertexBufferWriter} does for a
 * {@code BufferBuilder} — push ready-made vertex bytes straight into the
 * buffer — reimplemented on the vanilla builder so raw-vertex consumers
 * (AnimFreeze, ParallelModels) work without Sodium.
 *
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderRawMixin implements RawBuffer {

    @Shadow
    private int vertices;
    @Shadow
    @Final
    private int vertexSize;
    @Shadow
    private long vertexPointer;
    @Shadow
    @Final
    private ByteBufferBuilder buffer;
    @Shadow
    private int elementsToFill;
    @Shadow
    @Final
    private VertexFormat format;

    @Override
    public boolean lovisual$accepts(VertexFormat format) {
        return format == this.format;
    }

    @Override
    public void lovisual$push(long src, int count) {
        int length = count * vertexSize;
        long dst = buffer.reserve(length);
        MemoryUtil.memCopy(src, dst, length);
        vertices += count;
        // the pushed vertices are complete: the next addVertex must not see a half-filled one
        vertexPointer = dst + length - vertexSize;
        elementsToFill = 0;
    }
}
