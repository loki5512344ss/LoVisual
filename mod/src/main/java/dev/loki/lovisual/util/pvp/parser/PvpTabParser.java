package dev.loki.lovisual.util.pvp.parser;
import dev.loki.lovisual.util.pvp.parser.PvpOverlayParser;
import dev.loki.lovisual.util.pvp.parser.PvpTextPatternUtil;

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
