/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.guard.LegacyRenderPath;
import dev.loki.lovisual.render.engine.guard.RenderArchitectureGuard;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarp;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.memPutByte;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

/**
 * Reusable CPU-side mesh builder backed by native direct buffers.
 *
 * <p>Geometry is written between {@link #begin()} and {@link #end()}, then consumed through
 * {@link #vertexBufferView()} and {@link #indexBufferView()} by the RHI dynamic-mesh upload path.
 * The builder owns only its CPU buffers; GPU buffer allocation and lifetime are managed by the renderer.</p>
 *
 * <p>ФАСАД (исключение Фазы 8): горячий путь записи атрибутов живёт здесь; рост буферов,
 * debug-валидация и слияние мешей — в package-private хелперах `uniform/mesh/`
 * (MeshBufferGrowth / MeshDebugChecks / MeshMerge, тот же package).</p>
 */
public final class MeshBuilder implements AutoCloseable {
    final VertexFormat format;
    final int vertexStride;
    final int primitiveIndicesCount;
    private final double[] warped2 = new double[2];
    public double alpha = 1.0;
    ByteBuffer vertices;
    long verticesPtrStart, verticesPtr;
    ByteBuffer indices;
    long indicesPtr;
    int vertexI;
    int indicesCount;
    boolean building;
    private boolean worldCameraAnchored;
    private double cameraX, cameraZ;

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
        if (RenderState.rendering3D) {
            beginWorld(resolveRenderCameraPos());
        } else {
            beginScreen();
        }
    }

    /**
     * Begin a 2D/screen-space mesh. No world camera anchoring is applied.
     */
    public void beginScreen() {
        beginInternal(false, Vec3.ZERO);
    }

    /**
     * Begin a world-space mesh with an explicit camera anchor.
     *
     * <p>World vertices keep full Y, but X/Z are written camera-relative to avoid
     * float precision loss. The RHI world pass then applies the matching -cameraY
     * translation immediately before writing the MeshData UBO. Do not make this
     * depend on ambient RenderState in world command paths: the caller must pass
     * the camera that belongs to the current frame.</p>
     */
    public void beginWorld(Vec3 cameraPos) {
        if (cameraPos == null) {
            throw new IllegalArgumentException("MeshBuilder.beginWorld() requires a camera position");
        }
        beginInternal(true, cameraPos);
    }

    private void beginInternal(boolean worldCameraAnchored, Vec3 cameraPos) {
        if (building) throw new IllegalStateException("Mesh.begin() called while already building.");

        if (vertices == null || indices == null) {
            reserve(MeshBufferGrowth.DEFAULT_VERTEX_CAPACITY, MeshBufferGrowth.DEFAULT_INDEX_CAPACITY);
        }

        verticesPtr = verticesPtrStart;
        vertexI = 0;
        indicesCount = 0;

        this.worldCameraAnchored = worldCameraAnchored;
        this.cameraX = worldCameraAnchored ? cameraPos.x : 0.0;
        this.cameraZ = worldCameraAnchored ? cameraPos.z : 0.0;

        building = true;
    }

    private static Vec3 resolveRenderCameraPos() {
        if (RenderState.cameraPos != null && RenderState.cameraPos != Vec3.ZERO) {
            return RenderState.cameraPos;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            return mc.gameRenderer.mainCamera().position();
        }
        return Vec3.ZERO;
    }

    public MeshBuilder vec3(double x, double y, double z) {
        MeshDebugChecks.vertexWrite(this, 12, "Position3");

        long p = verticesPtr;

        memPutFloat(p, (float) (x - cameraX));
        memPutFloat(p + 4, (float) y);
        memPutFloat(p + 8, (float) (z - cameraZ));

        verticesPtr += 12;
        return this;
    }

    public MeshBuilder vec2(double x, double y) {
        MeshDebugChecks.vertexWrite(this, 8, "Position2");

        RenderWarp warp = RenderWarpStack.current();
        if (warp.active()) {
            warp.map(x, y, warped2);
            x = warped2[0];
            y = warped2[1];
        }

        long p = verticesPtr;

        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);

        verticesPtr += 8;
        return this;
    }

    public MeshBuilder raw2(double x, double y) {
        MeshDebugChecks.vertexWrite(this, 8, "Raw2");

        long p = verticesPtr;

        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);

        verticesPtr += 8;
        return this;
    }

    public MeshBuilder local2(double x, double y) {
        MeshDebugChecks.vertexWrite(this, 16, "Local");

        RenderWarp warp = RenderWarpStack.current();
        double invW = warp.active() ? warp.interpolationInvW(x, y) : 1.0;

        long p = verticesPtr;

        memPutFloat(p, (float) (x * invW));
        memPutFloat(p + 4, (float) (y * invW));
        memPutFloat(p + 8, (float) invW);
        memPutFloat(p + 12, 0.0f);

        verticesPtr += 16;
        return this;
    }

    public MeshBuilder vec4(float x, float y, float z, float w) {
        MeshDebugChecks.vertexWrite(this, 16, "Vec4");

        long p = verticesPtr;

        memPutFloat(p, x);
        memPutFloat(p + 4, y);
        memPutFloat(p + 8, z);
        memPutFloat(p + 12, w);

        verticesPtr += 16;
        return this;
    }

    public MeshBuilder vec4(double x, double y, double z, double w) {
        return vec4((float) x, (float) y, (float) z, (float) w);
    }

    /**
     * Color as bytes 0..255
     */
    public MeshBuilder color(int r, int g, int b, int a) {
        MeshDebugChecks.vertexWrite(this, 4, "Color");

        long p = verticesPtr;

        memPutByte(p, (byte) r);
        memPutByte(p + 1, (byte) g);
        memPutByte(p + 2, (byte) b);
        memPutByte(p + 3, (byte) Math.max(0, Math.min(255, (int) (a * alpha))));

        verticesPtr += 4;
        return this;
    }

    /**
     * Color as floats 0..1
     */
    public MeshBuilder color(float r, float g, float b, float a) {
        return color(
                (int) (r * 255f),
                (int) (g * 255f),
                (int) (b * 255f),
                (int) (a * 255f)
        );
    }

    /**
     * Color from RenderColor (0..255).
     */
    public MeshBuilder color(RenderColor color) {
        return color(color.r, color.g, color.b, color.a);
    }

    public int next() {
        MeshDebugChecks.completedVertex(this);
        return vertexI++;
    }

    public void line(int i1, int i2) {
        MeshDebugChecks.indexWrite(this, 2, "line");

        long p = indicesPtr + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);

        indicesCount += 2;
    }

    public void triangle(int i1, int i2, int i3) {
        MeshDebugChecks.indexWrite(this, 3, "triangle");

        long p = indicesPtr + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);

        indicesCount += 3;
    }

    public void quad(int i1, int i2, int i3, int i4) {
        MeshDebugChecks.indexWrite(this, 6, "quad");

        long p = indicesPtr + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);

        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);

        indicesCount += 6;
    }

    public void ensureLineCapacity() {
        ensureCapacity(2, 2);
    }

    public void ensureTriCapacity() {
        ensureCapacity(3, 3);
    }

    public void ensureQuadCapacity() {
        ensureCapacity(4, 6);
    }

    public void ensureCapacity(int addVertices, int addIndices) {
        MeshBufferGrowth.ensure(this, addVertices, addIndices);
    }

    /**
     * Ensures that the CPU-side direct buffers can hold at least the requested amount of vertices/indices.
     *
     * <p>This method is intentionally separate from {@link #begin()}: render code that owns a long-lived
     * MeshBuilder can pre-warm it during initialization instead of paying ByteBuffer.allocateDirect on the
     * render-thread hot path. If the current buffers are already large enough, this is a no-op.</p>
     */
    public void reserve(int vertexCount, int indexCount) {
        MeshBufferGrowth.reserve(this, vertexCount, indexCount);
    }

    public void end() {
        validateComplete("MeshBuilder.end");
        building = false;
    }

    public boolean isBuilding() {
        return building;
    }

    /**
     * Append an already-built mesh into this builder, rebasing INT indices by the current vertex count.
     * This is used by higher level batchers (UI/text) to preserve draw order without flushing per element.
     */
    public void appendMesh(MeshBuilder source) {
        MeshMerge.append(this, source);
    }

    public ByteBuffer vertexBufferView() {
        validateComplete("MeshBuilder.vertexBufferView");
        ByteBuffer copy = vertices.duplicate().order(ByteOrder.nativeOrder());
        copy.position(0);
        copy.limit(getVerticesOffset());
        return copy.slice().order(ByteOrder.nativeOrder());
    }

    public ByteBuffer indexBufferView() {
        validateComplete("MeshBuilder.indexBufferView");
        ByteBuffer copy = indices.duplicate().order(ByteOrder.nativeOrder());
        copy.position(0);
        copy.limit(indicesCount * Integer.BYTES);
        return copy.slice().order(ByteOrder.nativeOrder());
    }

    public int getVertexBytes() {
        return getVerticesOffset();
    }

    public int getVertexStride() {
        return vertexStride;
    }

    public boolean isWorldCameraAnchored() {
        return worldCameraAnchored;
    }

    public double cameraAnchorX() {
        return cameraX;
    }

    public double cameraAnchorZ() {
        return cameraZ;
    }

    public int getIndexBytes() {
        return indicesCount * Integer.BYTES;
    }

    /**
     * Emergency compatibility upload. This path is locked after the RHI migration and is not allowed as production
     * behavior. Use LoVisualRHI.dynamicMeshes().upload(mesh) instead.
     */
    @Deprecated
    public GpuBuffer getVertexBuffer() {
        RenderArchitectureGuard.requireAllowed(LegacyRenderPath.IMMEDIATE_MESH_UPLOAD, "MeshBuilder#getVertexBuffer");
        return RenderSystem.getDevice().createBuffer(
                () -> "lovisual-immediate-vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                vertexBufferView()
        );
    }

    /**
     * Emergency compatibility upload. This path is locked after the RHI migration and is not allowed as production
     * behavior. Use LoVisualRHI.dynamicMeshes().upload(mesh) instead.
     */
    @Deprecated
    public GpuBuffer getIndexBuffer() {
        RenderArchitectureGuard.requireAllowed(LegacyRenderPath.IMMEDIATE_MESH_UPLOAD, "MeshBuilder#getIndexBuffer");
        return RenderSystem.getDevice().createBuffer(
                () -> "lovisual-immediate-indices",
                GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                indexBufferView()
        );
    }

    public com.mojang.blaze3d.IndexType getIndexType() {
        // we write ints in indices()
        return com.mojang.blaze3d.IndexType.INT;
    }

    public int getIndicesCount() {
        return indicesCount;
    }

    public int getVertexCount() {
        return vertexI;
    }

    int getVerticesOffset() {
        return (int) (verticesPtr - verticesPtrStart);
    }

    public void validateComplete(String owner) {
        MeshDebugChecks.validateComplete(this, owner);
    }

    /**
     * Releases CPU-side native buffers owned by this builder. The builder may be reused later;
     * the next begin()/reserve() call will allocate fresh buffers.
     */
    @Override
    public void close() {
        if (building) {
            building = false;
        }
        MeshBufferGrowth.freeDirect(vertices);
        MeshBufferGrowth.freeDirect(indices);
        vertices = null;
        indices = null;
        verticesPtrStart = 0L;
        verticesPtr = 0L;
        indicesPtr = 0L;
        vertexI = 0;
        indicesCount = 0;
        worldCameraAnchored = false;
        cameraX = 0.0;
        cameraZ = 0.0;
    }
}
