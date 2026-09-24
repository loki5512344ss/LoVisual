package dev.loki.lovisual.render.engine.rhi.resource.manager;

public record RenderResourceStatsSnapshot(
        long frameId,
        int persistentFramebuffers,
        int temporaryFramebuffers,
        long framebufferBorrows,
        long framebufferReleases,
        long framebufferCreates,
        long framebufferResizes,
        long framebufferInvalidations,
        long textureRetirements,
        long textureCloses,
        long retirementQueued,
        long retirementClosed,
        int retirementBacklog,
        int leakedResources
) {
}
