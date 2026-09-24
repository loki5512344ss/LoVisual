package dev.loki.lovisual.render.engine.pipeline;

import com.mojang.blaze3d.platform.CompareOp;

public enum DepthTestFunction {
    NO_DEPTH_TEST(null),
    LEQUAL_DEPTH_TEST(CompareOp.LESS_THAN_OR_EQUAL),
    GEQUAL_DEPTH_TEST(CompareOp.GREATER_THAN_OR_EQUAL);

    private final CompareOp compareOp;

    DepthTestFunction(CompareOp compareOp) {
        this.compareOp = compareOp;
    }

    public CompareOp compareOp() {
        return compareOp;
    }
}
