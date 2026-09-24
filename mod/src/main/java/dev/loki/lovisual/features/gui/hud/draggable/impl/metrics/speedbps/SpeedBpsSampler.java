package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import net.minecraft.client.Minecraft;
import dev.loki.lovisual.util.player.movement.PlayerSpeedUtil;

final class SpeedBpsSampler {

    private SpeedBpsSampler() {
    }

    static float currentBps() {
        Minecraft mc = Minecraft.getInstance();
        return PlayerSpeedUtil.getBps(mc != null ? mc.player : null);
    }

    static void updateSamples(SpeedBps host, float currentBps) {
        long now = System.currentTimeMillis();
        if (now - host.lastSampleMs < SpeedBps.SAMPLE_INTERVAL_MS) return;
        host.lastSampleMs = now;
        host.samples[host.sampleIndex] = currentBps;
        host.sampleIndex = (host.sampleIndex + 1) % SpeedBps.SAMPLE_COUNT;
        if (host.sampleCount < SpeedBps.SAMPLE_COUNT) host.sampleCount++;
    }

    static float averageBps(SpeedBps host, float fallback) {
        if (host.sampleCount <= 0) return fallback;
        float sum = 0f;
        for (int i = 0; i < host.sampleCount; i++) {
            sum += host.samples[i];
        }
        return sum / host.sampleCount;
    }

    static String getSpeedNumber(SpeedBps host, float speed) {
        int tenth = Math.round(speed * 10f);
        if (tenth == host.lastTenth) return host.cachedNumber;
        host.lastTenth = tenth;
        host.valueBuilder.setLength(0);
        appendOneDecimal(host.valueBuilder, tenth);
        host.cachedNumber = host.valueBuilder.toString();
        return host.cachedNumber;
    }

    static void appendOneDecimal(StringBuilder sb, int tenth) {
        if (tenth < 0) {
            sb.append('-');
            tenth = -tenth;
        }
        int intPart = tenth / 10;
        int frac = tenth - intPart * 10;
        sb.append(intPart);
        sb.append('.');
        sb.append((char) ('0' + frac));
    }
}
