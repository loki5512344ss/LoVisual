package dev.loki.lovisual.render.engine.world.stats;

public record WorldRenderStatsSnapshot(long frameId,
                                       long recordedCommands,
                                       long submittedCommands,
                                       long skippedEmptyCommands,
                                       long submittedVertices,
                                       long submittedIndices,
                                       long fogBindings,
                                       long depthPrePassBindings,
                                       long depthMainBindings,
                                       long depthDisabledBindings) {
}
