package dev.loki.lovisual.util.block.render;

import dev.loki.lovisual.config.values.mode.EnumValue;

public enum BlockOutlineShapeMode implements EnumValue.IdProvider {
    FULL_BLOCK("full_block"),
    VOXEL_SHAPE("voxel_shape");

    private final String id;

    BlockOutlineShapeMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
