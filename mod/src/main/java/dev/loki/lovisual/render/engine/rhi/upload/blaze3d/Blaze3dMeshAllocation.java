/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.upload.blaze3d;

import dev.loki.lovisual.render.engine.rhi.resource.GpuMeshHandle;
import dev.loki.lovisual.render.engine.rhi.resource.MeshOwnership;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.rhi.upload.blaze3d.Blaze3dMeshArena;

public record Blaze3dMeshAllocation(Blaze3dMeshArena arena,
                              int vertexOffsetBytes,
                              int indexOffsetBytes,
                              int vertexBytes,
                              int indexBytes,
                              int baseVertex) {
    static int baseVertex(int vertexOffsetBytes, int vertexStride) {
        if (vertexStride <= 0) {
            throw new IllegalArgumentException("Invalid vertex stride: " + vertexStride);
        }
        if (vertexOffsetBytes % vertexStride != 0) {
            throw new IllegalStateException("Dynamic mesh vertex offset is not stride-aligned: offset="
                    + vertexOffsetBytes + ", stride=" + vertexStride);
        }
        return vertexOffsetBytes / vertexStride;
    }

    public GpuMeshHandle toHandle(MeshBuilder mesh) {
        return GpuMeshHandle.arena(
                arena.vertexBuffer(),
                arena.indexBuffer(),
                vertexOffsetBytes,
                vertexBytes,
                mesh.getVertexStride(),
                baseVertex,
                indexOffsetBytes,
                indexBytes,
                mesh.getIndicesCount(),
                MeshOwnership.BACKEND_FRAME
        );
    }

    public void write(MeshBuilder mesh) {
        mesh.validateComplete("dynamic mesh allocation write");
        arena.writeVertices(vertexOffsetBytes, vertexBytes, mesh.vertexBufferView());
        arena.writeIndices(indexOffsetBytes, indexBytes, mesh.indexBufferView());
    }
}
