package dev.loki.lovisual.render.engine.uniform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.render.engine.guard.LegacyRenderPath;
import dev.loki.lovisual.render.engine.guard.RenderArchitectureGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.RenderState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.system.MemoryUtil.memPutInt;

/**
 * Примитивы, views, lifecycle и legacy upload для MeshBuilder.
 */
final class MeshOps {

    private MeshOps() {}

    static Vec3 resolveRenderCameraPos() {
        if (RenderState.cameraPos != null && RenderState.cameraPos != Vec3.ZERO) {
            return RenderState.cameraPos;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            return mc.gameRenderer.mainCamera().position();
        }
        return Vec3.ZERO;
    }

    static void beginInternal(MeshBuilder m, boolean worldCameraAnchored, Vec3 cameraPos) {
        if (m.building) throw new IllegalStateException("Mesh.begin() called while already building.");
        if (m.vertices == null || m.indices == null) {
            MeshBufferGrowth.reserve(m, MeshBufferGrowth.DEFAULT_VERTEX_CAPACITY, MeshBufferGrowth.DEFAULT_INDEX_CAPACITY);
        }
        m.verticesPtr = m.verticesPtrStart;
        m.vertexI = 0;
        m.indicesCount = 0;
        m.worldCameraAnchored = worldCameraAnchored;
        m.cameraX = worldCameraAnchored ? cameraPos.x : 0.0;
        m.cameraZ = worldCameraAnchored ? cameraPos.z : 0.0;
        m.building = true;
    }

    static int next(MeshBuilder m) {
        MeshDebugChecks.completedVertex(m);
        return m.vertexI++;
    }

    static void line(MeshBuilder m, int i1, int i2) {
        MeshDebugChecks.indexWrite(m, 2, "line");
        long p = m.indicesPtr + m.indicesCount * 4L;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        m.indicesCount += 2;
    }

    static void triangle(MeshBuilder m, int i1, int i2, int i3) {
        MeshDebugChecks.indexWrite(m, 3, "triangle");
        long p = m.indicesPtr + m.indicesCount * 4L;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);
        m.indicesCount += 3;
    }

    static void quad(MeshBuilder m, int i1, int i2, int i3, int i4) {
        MeshDebugChecks.indexWrite(m, 6, "quad");
        long p = m.indicesPtr + m.indicesCount * 4L;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);
        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);
        m.indicesCount += 6;
    }

    static ByteBuffer vertexBufferView(MeshBuilder m) {
        m.validateComplete("MeshBuilder.vertexBufferView");
        ByteBuffer copy = m.vertices.duplicate().order(ByteOrder.nativeOrder());
        copy.position(0);
        copy.limit(m.getVerticesOffset());
        return copy.slice().order(ByteOrder.nativeOrder());
    }

    static ByteBuffer indexBufferView(MeshBuilder m) {
        m.validateComplete("MeshBuilder.indexBufferView");
        ByteBuffer copy = m.indices.duplicate().order(ByteOrder.nativeOrder());
        copy.position(0);
        copy.limit(m.indicesCount * Integer.BYTES);
        return copy.slice().order(ByteOrder.nativeOrder());
    }

    @Deprecated
    static GpuBuffer getVertexBuffer(MeshBuilder m) {
        RenderArchitectureGuard.requireAllowed(LegacyRenderPath.IMMEDIATE_MESH_UPLOAD, "MeshBuilder#getVertexBuffer");
        return RenderSystem.getDevice().createBuffer(
                () -> "lovisual-immediate-vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                vertexBufferView(m)
        );
    }

    @Deprecated
    static GpuBuffer getIndexBuffer(MeshBuilder m) {
        RenderArchitectureGuard.requireAllowed(LegacyRenderPath.IMMEDIATE_MESH_UPLOAD, "MeshBuilder#getIndexBuffer");
        return RenderSystem.getDevice().createBuffer(
                () -> "lovisual-immediate-indices",
                GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                indexBufferView(m)
        );
    }

    static void close(MeshBuilder m) {
        if (m.building) {
            m.building = false;
        }
        MeshBufferGrowth.freeDirect(m.vertices);
        MeshBufferGrowth.freeDirect(m.indices);
        m.vertices = null;
        m.indices = null;
        m.verticesPtrStart = 0L;
        m.verticesPtr = 0L;
        m.indicesPtr = 0L;
        m.vertexI = 0;
        m.indicesCount = 0;
        m.worldCameraAnchored = false;
        m.cameraX = 0.0;
        m.cameraZ = 0.0;
    }
}
