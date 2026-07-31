/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.pvp;

public enum PvpTabParser {
    ;

    public static PvpOverlayParser.Result parse(String raw,
                                                Iterable<String> activePatterns,
                                                Iterable<String> exitPatterns) {
        if (raw == null || raw.isBlank()) return null;

        if (PvpTextPatternUtil.matchesAny(raw, exitPatterns)) {
            return PvpOverlayParser.Result.exited();
        }

        PvpTextPatternUtil.Match match = PvpTextPatternUtil.match(raw, activePatterns);
        if (match == null) return null;

        return new PvpOverlayParser.Result(true, match.secondsLeft(), match.opponentName(), match.opponentHealth(), false);
    }
}
