/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

final class TpsFormat {

    private TpsFormat() {
    }

    static void updateTpsStrings(Tps host, float tps, float extraTps, boolean extraEnabled) {
        int tenth = Math.round(tps * 10f);
        int hundredth = Math.round(extraTps * 100f);
        if (extraEnabled == host.lastExtra
                && tenth == host.lastTpsTenth
                && (!extraEnabled || hundredth == host.lastExtraHundredth)) {
            return;
        }

        host.lastExtra = extraEnabled;
        host.lastTpsTenth = tenth;
        host.lastExtraHundredth = hundredth;

        host.tpsBuilder.setLength(0);
        appendOneDecimal(host.tpsBuilder, tenth);
        host.cachedValue = host.tpsBuilder.toString();

        if (extraEnabled) {
            host.extraBuilder.setLength(0);
            host.extraBuilder.append('[');
            appendTwoDecimals(host.extraBuilder, hundredth);
            host.extraBuilder.append(']');
            host.cachedExtra = host.extraBuilder.toString();
        } else {
            host.cachedExtra = "";
        }
    }

    private static void appendOneDecimal(StringBuilder sb, int tenth) {
        if (tenth < 0) {
            sb.append('-');
            tenth = -tenth;
        }
        int intPart = tenth / 10;
        int frac = tenth - intPart * 10;
        sb.append(intPart).append('.').append((char) ('0' + frac));
    }

    private static void appendTwoDecimals(StringBuilder sb, int hundredth) {
        if (hundredth < 0) {
            sb.append('-');
            hundredth = -hundredth;
        }
        int intPart = hundredth / 100;
        int frac = hundredth - intPart * 100;
        sb.append(intPart)
                .append('.')
                .append((char) ('0' + (frac / 10)))
                .append((char) ('0' + (frac % 10)));
    }
}
