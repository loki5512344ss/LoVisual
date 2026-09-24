package dev.loki.lovisual.render.engine.vertex;

import com.mojang.blaze3d.GpuFormat;

public enum LoVisualVertexFormatElements {
    ;
    public static final GpuFormat POS3 = GpuFormat.RGB32_FLOAT;
    public static final GpuFormat POS2 = GpuFormat.RG32_FLOAT;
    public static final GpuFormat TEXTURE = GpuFormat.RG32_FLOAT;
    public static final GpuFormat COLOR = GpuFormat.RGBA8_UNORM;
    public static final GpuFormat RECT = GpuFormat.RGBA32_FLOAT;
    public static final GpuFormat PARAMS = GpuFormat.RGBA32_FLOAT;
    public static final GpuFormat PARAMS2 = GpuFormat.RGBA32_FLOAT;
    public static final GpuFormat PARAMS3 = GpuFormat.RGBA32_FLOAT;
    public static final GpuFormat PARAMS4 = GpuFormat.RGBA32_FLOAT;
    public static final GpuFormat PARAMS5 = GpuFormat.RGBA32_FLOAT;
    public static final GpuFormat LOCAL = GpuFormat.RGBA32_FLOAT;

}
