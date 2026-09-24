package dev.loki.lovisual.render.engine.uniform;

import dev.loki.lovisual.render.engine.renderer.warp.RenderWarp;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;

import static org.lwjgl.system.MemoryUtil.memPutByte;
import static org.lwjgl.system.MemoryUtil.memPutFloat;

/**
 * Вершинные записи MeshBuilder: позиции, warp, цвета.
 */
final class MeshWrites {

    private MeshWrites() {}

    static MeshBuilder vec3(MeshBuilder m, double x, double y, double z) {
        MeshDebugChecks.vertexWrite(m, 12, "Position3");
        long p = m.verticesPtr;
        memPutFloat(p, (float) (x - m.cameraX));
        memPutFloat(p + 4, (float) y);
        memPutFloat(p + 8, (float) (z - m.cameraZ));
        m.verticesPtr += 12;
        return m;
    }

    static MeshBuilder vec2(MeshBuilder m, double x, double y) {
        MeshDebugChecks.vertexWrite(m, 8, "Position2");
        RenderWarp warp = RenderWarpStack.current();
        if (warp.active()) {
            warp.map(x, y, m.warped2);
            x = m.warped2[0];
            y = m.warped2[1];
        }
        long p = m.verticesPtr;
        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);
        m.verticesPtr += 8;
        return m;
    }

    static MeshBuilder raw2(MeshBuilder m, double x, double y) {
        MeshDebugChecks.vertexWrite(m, 8, "Raw2");
        long p = m.verticesPtr;
        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);
        m.verticesPtr += 8;
        return m;
    }

    static MeshBuilder local2(MeshBuilder m, double x, double y) {
        MeshDebugChecks.vertexWrite(m, 16, "Local");
        RenderWarp warp = RenderWarpStack.current();
        double invW = warp.active() ? warp.interpolationInvW(x, y) : 1.0;
        long p = m.verticesPtr;
        memPutFloat(p, (float) (x * invW));
        memPutFloat(p + 4, (float) (y * invW));
        memPutFloat(p + 8, (float) invW);
        memPutFloat(p + 12, 0.0f);
        m.verticesPtr += 16;
        return m;
    }

    static MeshBuilder vec4(MeshBuilder m, float x, float y, float z, float w) {
        MeshDebugChecks.vertexWrite(m, 16, "Vec4");
        long p = m.verticesPtr;
        memPutFloat(p, x);
        memPutFloat(p + 4, y);
        memPutFloat(p + 8, z);
        memPutFloat(p + 12, w);
        m.verticesPtr += 16;
        return m;
    }

    static MeshBuilder color(MeshBuilder m, int r, int g, int b, int a) {
        MeshDebugChecks.vertexWrite(m, 4, "Color");
        long p = m.verticesPtr;
        memPutByte(p, (byte) r);
        memPutByte(p + 1, (byte) g);
        memPutByte(p + 2, (byte) b);
        memPutByte(p + 3, (byte) Math.max(0, Math.min(255, (int) (a * m.alpha))));
        m.verticesPtr += 4;
        return m;
    }
}
