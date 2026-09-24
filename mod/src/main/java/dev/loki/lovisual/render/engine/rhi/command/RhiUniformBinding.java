package dev.loki.lovisual.render.engine.rhi.command;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

public record RhiUniformBinding(String name, GpuBufferSlice slice) {
}
