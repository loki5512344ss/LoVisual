/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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
