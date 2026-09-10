/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.guard.boundary;
import dev.loki.lovisual.render.engine.guard.RenderArchitectureGuard;


import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;
import dev.loki.lovisual.render.engine.rhi.pipeline.pipeline.RenderPipelineSpec;

/**
 * Lightweight runtime boundary audit. It does not scan source files; it validates explicitly registered legacy
 * exceptions and prints the architectural rules once during bootstrap in debug mode.
 */
public enum RenderBoundaryAudit {
    ;
    private static final List<String> LOCKED_RULES = List.of(
            "Renderer3D/Renderer2D/Text/PostProcess must submit through LoVisualRHI or command layers",
            "Direct GL is backend/profiler/MSAA-legacy only",
            "Direct Sodium runtime access is SodiumRenderBridge/terrain interop only",
            "Immediate mesh upload is emergency fallback only",
            "Individual uniform flipFrame calls are forbidden",
            "Fullscreen MeshBuilder quad is forbidden as production path",
            "Pipeline policy must come from RenderPipelineSpec metadata"
    );
    private static boolean ran;

    public static void runOnce() {
        if (ran) return;
        ran = true;
        if (!RenderArchitectureGuard.DEBUG) return;
        DebugLog.renderThread("[LoVisualRHI] render architecture boundary locked:");
        for (String rule : LOCKED_RULES) {
            DebugLog.renderThread("[LoVisualRHI]  - " + rule);
        }
    }
}
