package dev.loki.lovisual.render.engine.uniform;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Рост/аллокация CPU-буферов MeshBuilder (native direct ByteBuffer):
 * копирование данных при реаллокации, дефолтные размеры, reserve.
 */
final class MeshBufferGrowth {

    static final int DEFAULT_VERTEX_CAPACITY = Integer.getInteger("lovisual.render.mesh.defaultVertices", 4096);
    static final int DEFAULT_INDEX_CAPACITY = Integer.getInteger("lovisual.render.mesh.defaultIndices", 8192);
    private static final int MAX_BUFFER_BYTES = Integer.MAX_VALUE - 8;

    private MeshBufferGrowth() {}

    static ByteBuffer allocateDirect(int bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("MeshBuilder buffer size must be positive: " + bytes);
        }
        if (bytes > MAX_BUFFER_BYTES) {
            throw new OutOfMemoryError("MeshBuilder buffer request is too large: " + bytes);
        }
        return memAlloc(bytes);
    }

    static void freeDirect(ByteBuffer buffer) {
        if (buffer != null) {
            memFree(buffer);
        }
    }

    static void ensure(MeshBuilder m, int addVertices, int addIndices) {
        if (MeshDebugChecks.DEBUG && addIndices % m.primitiveIndicesCount != 0) {
            throw new IllegalArgumentException("Unexpected amount of indices written to MeshBuilder.");
        }

        if (m.vertices == null || m.indices == null) {
            reserve(m, Math.max(DEFAULT_VERTEX_CAPACITY, m.vertexI + addVertices),
                    Math.max(DEFAULT_INDEX_CAPACITY, m.indicesCount + addIndices));
            return;
        }

        int vNeedBytes = (m.vertexI + addVertices) * m.vertexStride;
        if (vNeedBytes > m.vertices.capacity()) {
            int offset = m.getVerticesOffset();
            int newSize = Math.max(m.vertices.capacity() * 2, m.vertices.capacity() + addVertices * m.vertexStride);

            ByteBuffer oldVertices = m.vertices;
            ByteBuffer newVertices = allocateDirect(newSize);
            memCopy(memAddress0(oldVertices), memAddress0(newVertices), offset);

            m.vertices = newVertices;
            m.verticesPtrStart = memAddress0(m.vertices);
            m.verticesPtr = m.verticesPtrStart + offset;
            freeDirect(oldVertices);
        }

        int iNeedBytes = (m.indicesCount + addIndices) * Integer.BYTES;
        if (iNeedBytes > m.indices.capacity()) {
            int newSize = Math.max(m.indices.capacity() * 2, m.indices.capacity() + addIndices * Integer.BYTES);

            ByteBuffer oldIndices = m.indices;
            ByteBuffer newIndices = allocateDirect(newSize);
            memCopy(memAddress0(oldIndices), memAddress0(newIndices), (long) m.indicesCount * 4L);

            m.indices = newIndices;
            m.indicesPtr = memAddress0(m.indices);
            freeDirect(oldIndices);
        }
    }

    /**
     * Гарантирует, что CPU-буферы вмещают как минимум запрошенное число
     * вершин/индексов. Отдельно от begin(): долгоживущие билдеры можно
     * прогревать на инициализации, а не в горячем пути.
     */
    static void reserve(MeshBuilder m, int vertexCount, int indexCount) {
        int vertexBytes = m.vertexStride * Math.max(1, vertexCount);
        int indexBytes = Math.max(1, indexCount) * Integer.BYTES;

        if (m.vertices == null || vertexBytes > m.vertices.capacity()) {
            int offset = m.vertices != null ? m.getVerticesOffset() : 0;
            ByteBuffer oldVertices = m.vertices;
            ByteBuffer newVertices = allocateDirect(vertexBytes);
            if (oldVertices != null && offset > 0) {
                memCopy(memAddress0(oldVertices), memAddress0(newVertices), offset);
            }

            m.vertices = newVertices;
            m.verticesPtrStart = memAddress0(m.vertices);
            m.verticesPtr = m.verticesPtrStart + offset;
            freeDirect(oldVertices);
        }

        if (m.indices == null || indexBytes > m.indices.capacity()) {
            long offset = m.indices != null ? (long) m.indicesCount * Integer.BYTES : 0L;
            ByteBuffer oldIndices = m.indices;
            ByteBuffer newIndices = allocateDirect(indexBytes);
            if (oldIndices != null && offset > 0L) {
                memCopy(memAddress0(oldIndices), memAddress0(newIndices), offset);
            }

            m.indices = newIndices;
            m.indicesPtr = memAddress0(m.indices);
            freeDirect(oldIndices);
        }
    }
}
