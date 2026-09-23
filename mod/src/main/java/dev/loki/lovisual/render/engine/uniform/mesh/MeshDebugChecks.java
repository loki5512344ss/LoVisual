package dev.loki.lovisual.render.engine.uniform;

import net.fabricmc.loader.api.FabricLoader;

import static org.lwjgl.system.MemoryUtil.memGetInt;

/**
 * Debug-валидация MeshBuilder: контроль stride/индексов/буферов.
 * Вся логика гейтится константой {@link #DEBUG} и в production
 * вычищается JIT (dead code elimination).
 */
final class MeshDebugChecks {

    static final boolean DEBUG =
            FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("lovisual.render.debug");

    private MeshDebugChecks() {}

    static void hasBuffers(MeshBuilder m, String owner) {
        if (m.vertices == null || m.indices == null) {
            throw new IllegalStateException(owner + ": MeshBuilder has no CPU buffers; call begin()/reserve() before writing.");
        }
    }

    static void vertexWrite(MeshBuilder m, int bytes, String attribute) {
        if (!DEBUG) return;
        hasBuffers(m, "MeshBuilder." + attribute);
        if (!m.building) {
            throw new IllegalStateException("MeshBuilder." + attribute + " write outside begin()/end(): format=" + m.format);
        }
        if (bytes <= 0) {
            throw new IllegalArgumentException("MeshBuilder." + attribute + " invalid write size: " + bytes);
        }
        int vertexBytes = m.getVerticesOffset();
        int currentVertexBytes = vertexBytes - m.vertexI * m.vertexStride;
        if (currentVertexBytes < 0 || currentVertexBytes > m.vertexStride) {
            throw new IllegalStateException("MeshBuilder." + attribute + " cursor is not inside current vertex: vertex=" + m.vertexI
                    + ", cursorBytes=" + vertexBytes
                    + ", currentVertexBytes=" + currentVertexBytes
                    + ", stride=" + m.vertexStride
                    + ", format=" + m.format);
        }
        if (currentVertexBytes + bytes > m.vertexStride) {
            throw new IllegalStateException("MeshBuilder." + attribute + " overruns vertex stride: vertex=" + m.vertexI
                    + ", currentVertexBytes=" + currentVertexBytes
                    + ", writeBytes=" + bytes
                    + ", stride=" + m.vertexStride
                    + ", format=" + m.format);
        }
        if (vertexBytes + bytes > m.vertices.capacity()) {
            throw new IndexOutOfBoundsException("MeshBuilder." + attribute + " write exceeds vertex CPU buffer: vertex=" + m.vertexI
                    + ", cursorBytes=" + vertexBytes
                    + ", writeBytes=" + bytes
                    + ", capacity=" + m.vertices.capacity()
                    + ", format=" + m.format);
        }
    }

    static void completedVertex(MeshBuilder m) {
        if (!DEBUG) return;
        hasBuffers(m, "MeshBuilder.next");
        int vertexBytes = m.getVerticesOffset();
        int expected = (m.vertexI + 1) * m.vertexStride;
        if (vertexBytes != expected) {
            throw new IllegalStateException("MeshBuilder.next() called with incomplete/overwritten vertex: vertex=" + m.vertexI
                    + ", expectedBytes=" + expected
                    + ", actualBytes=" + vertexBytes
                    + ", stride=" + m.vertexStride
                    + ", format=" + m.format);
        }
    }

    static void indexWrite(MeshBuilder m, int addIndices, String primitive) {
        if (!DEBUG) return;
        hasBuffers(m, "MeshBuilder." + primitive);
        if (!m.building) {
            throw new IllegalStateException("MeshBuilder." + primitive + " index write outside begin()/end(): format=" + m.format);
        }
        if (addIndices <= 0) {
            throw new IllegalArgumentException("MeshBuilder." + primitive + " invalid index count: " + addIndices);
        }
        int indexBytes = m.indicesCount * Integer.BYTES;
        int writeBytes = addIndices * Integer.BYTES;
        if (indexBytes + writeBytes > m.indices.capacity()) {
            throw new IndexOutOfBoundsException("MeshBuilder." + primitive + " write exceeds index CPU buffer: indices=" + m.indicesCount
                    + ", add=" + addIndices
                    + ", capacityBytes=" + m.indices.capacity()
                    + ", format=" + m.format);
        }
    }

    static void validateIndices(MeshBuilder m, String owner) {
        if (!DEBUG || m.indicesCount <= 0) return;
        long p = m.indicesPtr;
        for (int i = 0; i < m.indicesCount; i++) {
            int index = memGetInt(p + (long) i * Integer.BYTES);
            if (index < 0 || index >= m.vertexI) {
                throw new IndexOutOfBoundsException(owner + ": MeshBuilder index references missing vertex: indexOrdinal=" + i
                        + ", index=" + index
                        + ", vertexCount=" + m.vertexI
                        + ", indicesCount=" + m.indicesCount
                        + ", format=" + m.format);
            }
        }
    }

    static void validateComplete(MeshBuilder m, String owner) {
        hasBuffers(m, owner);
        int vertexBytes = m.getVerticesOffset();
        int expectedVertexBytes = m.vertexI * m.vertexStride;
        if (vertexBytes != expectedVertexBytes) {
            throw new IllegalStateException(owner + ": MeshBuilder vertex stride mismatch: vertexCount=" + m.vertexI
                    + ", expectedVertexBytes=" + expectedVertexBytes
                    + ", actualVertexBytes=" + vertexBytes
                    + ", stride=" + m.vertexStride
                    + ", format=" + m.format);
        }
        if (vertexBytes < 0 || vertexBytes > m.vertices.capacity()) {
            throw new IndexOutOfBoundsException(owner + ": MeshBuilder vertex range outside CPU buffer: bytes="
                    + vertexBytes + ", capacity=" + m.vertices.capacity() + ", format=" + m.format);
        }
        int indexBytes = m.indicesCount * Integer.BYTES;
        if (indexBytes < 0 || indexBytes > m.indices.capacity()) {
            throw new IndexOutOfBoundsException(owner + ": MeshBuilder index range outside CPU buffer: bytes="
                    + indexBytes + ", capacity=" + m.indices.capacity() + ", format=" + m.format);
        }
        if (m.primitiveIndicesCount > 0 && m.indicesCount % m.primitiveIndicesCount != 0) {
            if (DEBUG) {
                throw new IllegalStateException(owner + ": MeshBuilder incomplete primitive payload: indices=" + m.indicesCount
                        + ", primitiveLength=" + m.primitiveIndicesCount + ", format=" + m.format);
            }
            int trimmed = (m.indicesCount / m.primitiveIndicesCount) * m.primitiveIndicesCount;
            // Trim incomplete tail to avoid Render Frame crash (e.g. 176 % 3 == 2 on TRIANGLES).
            org.slf4j.LoggerFactory.getLogger(MeshBuilder.class).warn("{}: trimming incomplete primitive payload: indices={} -> {} (primitiveLength={}, format={})", owner, m.indicesCount, trimmed, m.primitiveIndicesCount, m.format);
            m.indicesCount = trimmed;
            if (m.indicesCount == 0) return;
        }
        validateIndices(m, owner);
    }
}
