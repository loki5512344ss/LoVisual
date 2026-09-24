package dev.loki.lovisual.render.engine.command.world;

public record UiStatsSnapshot(long frameId,
                              int recordedCommands,
                              int shapeCommands,
                              int pathCommands,
                              int textureCommands,
                              int textCommands,
                              int itemCommands,
                              int effectCommands,
                              int compiledBatches,
                              int backendCommands) {
}
