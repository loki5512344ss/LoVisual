/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.sodium.visibility;

public record SodiumVisibilityStatsSnapshot(long queries,
                                            long accepts,
                                            long rejects,
                                            long bypasses,
                                            long unavailableBypasses,
                                            long optOutBypasses,
                                            long errors) {
    public static final SodiumVisibilityStatsSnapshot EMPTY = new SodiumVisibilityStatsSnapshot(0, 0, 0, 0, 0, 0, 0);
}
