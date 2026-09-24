package dev.loki.lovisual.render.engine.rhi.stats;

import dev.loki.lovisual.render.engine.guard.LegacyRenderPath;

import java.util.Map;

public record RhiStatsSnapshot(long frameId,
                               long drawCalls,
                               long renderPasses,
                               long renderPassAttachmentSwitches,
                               long fullscreenPasses,
                               long textureFastCopies,
                               long textureShaderCopies,
                               long meshUploads,
                               long uploadedVertexBytes,
                               long uploadedIndexBytes,
                               long ringWraps,
                               long ringStalls,
                               long immediateFallbackUploads,
                               long temporaryOwnedMeshes,
                               long dynamicArenaAllocations,
                               long dynamicPersistentArenaAllocations,
                               long dynamicSpillArenaAllocations,
                               long dynamicArenaReuses,
                               long dynamicArenaRetires,
                               long dynamicFenceChecks,
                               long dynamicFenceCompletions,
                               long dynamicArenaBacklogEvents,
                               long dynamicPersistentArenaBytes,
                               long dynamicSpillArenaBytes,
                               long legacyPathUses,
                               Map<LegacyRenderPath, Long> legacyPathBreakdown) {
}
