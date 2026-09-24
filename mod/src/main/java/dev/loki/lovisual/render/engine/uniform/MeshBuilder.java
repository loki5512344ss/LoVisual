package dev.loki.lovisual.render.engine.uniform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.RenderColor;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Reusable CPU-side mesh builder backed by native direct buffers.
 * Фасад ≤200: атрибуты → MeshWrites, примитивы/views/lifecycle → MeshOps,
 * рост/валидация/merge → mesh/* хелперы (тот же package).
 */
public final class MeshBuilder implements AutoCloseable {
    final VertexFormat format;
    final int vertexStride;
    final int primitiveIndicesCount;
    final double[] warped2 = new double[2];
    public double alpha = 1.0;
    ByteBuffer vertices;
    long verticesPtrStart, verticesPtr;
    ByteBuffer indices;
    long indicesPtr;
    int vertexI;
    int indicesCount;
    boolean building;
    boolean worldCameraAnchored;
    double cameraX, cameraZ;

    public MeshBuilder(RenderPipeline pipeline) {
        this(Objects.requireNonNull(pipeline.getVertexFormatBinding(0)), pipeline.getPrimitiveTopology());
    }

    public MeshBuilder(VertexFormat format, com.mojang.blaze3d.PrimitiveTopology drawMode) {
        this.format = format;
        this.vertexStride = format.getVertexSize();
        this.primitiveIndicesCount = drawMode.primitiveLength;
    }

    public MeshBuilder(VertexFormat format, com.mojang.blaze3d.PrimitiveTopology drawMode, int vertexCount, int indexCount) {
        this(format, drawMode);
        reserve(vertexCount, indexCount);
    }

    public void begin() {
        if (RenderState.rendering3D) beginWorld(MeshOps.resolveRenderCameraPos());
        else beginScreen();
    }

    public void beginScreen() { beginInternal(false, Vec3.ZERO); }

    public void beginWorld(Vec3 cameraPos) {
        if (cameraPos == null) throw new IllegalArgumentException("MeshBuilder.beginWorld() requires a camera position");
        beginInternal(true, cameraPos);
    }

    private void beginInternal(boolean worldCameraAnchored, Vec3 cameraPos) { MeshOps.beginInternal(this, worldCameraAnchored, cameraPos); }

    public MeshBuilder vec3(double x, double y, double z) { return MeshWrites.vec3(this, x, y, z); }
    public MeshBuilder vec2(double x, double y) { return MeshWrites.vec2(this, x, y); }
    public MeshBuilder raw2(double x, double y) { return MeshWrites.raw2(this, x, y); }
    public MeshBuilder local2(double x, double y) { return MeshWrites.local2(this, x, y); }
    public MeshBuilder vec4(float x, float y, float z, float w) { return MeshWrites.vec4(this, x, y, z, w); }
    public MeshBuilder vec4(double x, double y, double z, double w) { return vec4((float) x, (float) y, (float) z, (float) w); }

    public MeshBuilder color(int r, int g, int b, int a) { return MeshWrites.color(this, r, g, b, a); }
    public MeshBuilder color(float r, float g, float b, float a) { return color((int) (r * 255f), (int) (g * 255f), (int) (b * 255f), (int) (a * 255f)); }
    public MeshBuilder color(RenderColor color) { return color(color.r, color.g, color.b, color.a); }

    public int next() { return MeshOps.next(this); }
    public void line(int i1, int i2) { MeshOps.line(this, i1, i2); }
    public void triangle(int i1, int i2, int i3) { MeshOps.triangle(this, i1, i2, i3); }
    public void quad(int i1, int i2, int i3, int i4) { MeshOps.quad(this, i1, i2, i3, i4); }

    public void ensureLineCapacity() { ensureCapacity(2, 2); }
    public void ensureTriCapacity() { ensureCapacity(3, 3); }
    public void ensureQuadCapacity() { ensureCapacity(4, 6); }
    public void ensureCapacity(int addVertices, int addIndices) { MeshBufferGrowth.ensure(this, addVertices, addIndices); }
    public void reserve(int vertexCount, int indexCount) { MeshBufferGrowth.reserve(this, vertexCount, indexCount); }

    public void end() { validateComplete("MeshBuilder.end"); building = false; }
    public boolean isBuilding() { return building; }
    public void appendMesh(MeshBuilder source) { MeshMerge.append(this, source); }

    public ByteBuffer vertexBufferView() { return MeshOps.vertexBufferView(this); }
    public ByteBuffer indexBufferView() { return MeshOps.indexBufferView(this); }

    public int getVertexBytes() { return getVerticesOffset(); }
    public int getVertexStride() { return vertexStride; }
    public boolean isWorldCameraAnchored() { return worldCameraAnchored; }
    public double cameraAnchorX() { return cameraX; }
    public double cameraAnchorZ() { return cameraZ; }
    public int getIndexBytes() { return indicesCount * Integer.BYTES; }

    @Deprecated public GpuBuffer getVertexBuffer() { return MeshOps.getVertexBuffer(this); }
    @Deprecated public GpuBuffer getIndexBuffer() { return MeshOps.getIndexBuffer(this); }

    public com.mojang.blaze3d.IndexType getIndexType() { return com.mojang.blaze3d.IndexType.INT; }
    public int getIndicesCount() { return indicesCount; }
    public int getVertexCount() { return vertexI; }
    int getVerticesOffset() { return (int) (verticesPtr - verticesPtrStart); }
    public void validateComplete(String owner) { MeshDebugChecks.validateComplete(this, owner); }

    @Override public void close() { MeshOps.close(this); }
}
