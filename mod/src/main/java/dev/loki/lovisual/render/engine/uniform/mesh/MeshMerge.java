package dev.loki.lovisual.render.engine.uniform;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memGetInt;
import static org.lwjgl.system.MemoryUtil.memPutInt;

/**
 * Слияние собранных мешей: appendMesh с ребазой INT-индексов на текущее
 * число вершин приёмника (порядок отрисовки сохраняется без flush).
 */
final class MeshMerge {

    private MeshMerge() {}

    static void append(MeshBuilder dst, MeshBuilder source) {
        if (source == null || source.getVertexCount() <= 0 || source.getIndicesCount() <= 0) return;
        if (source.isBuilding()) source.end();
        if (!dst.building) {
            throw new IllegalStateException("appendMesh() requires destination mesh to be building.");
        }
        if (dst.vertexStride != source.vertexStride || !dst.format.equals(source.format)) {
            throw new IllegalArgumentException("Cannot append mesh with different vertex format/stride.");
        }

        int sourceVertices = source.getVertexCount();
        int sourceIndices = source.getIndicesCount();
        int baseVertex = dst.vertexI;
        dst.ensureCapacity(sourceVertices, sourceIndices);

        ByteBuffer vertexSource = source.vertexBufferView();
        long vertexBytes = source.getVertexBytes();
        memCopy(memAddress0(vertexSource), dst.verticesPtr, vertexBytes);
        dst.verticesPtr += vertexBytes;
        dst.vertexI += sourceVertices;

        ByteBuffer indexSource = source.indexBufferView();
        long indexIn = memAddress0(indexSource);
        long indexOut = dst.indicesPtr + (long) dst.indicesCount * Integer.BYTES;
        for (int i = 0; i < sourceIndices; i++) {
            memPutInt(indexOut + (long) i * Integer.BYTES, memGetInt(indexIn + (long) i * Integer.BYTES) + baseVertex);
        }
        dst.indicesCount += sourceIndices;
    }
}
