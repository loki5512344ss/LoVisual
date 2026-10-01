package dev.loki.lovisual.features.module.modules.misc.voice;

/** Pure animation of the expanding rings drawn above a talking player. */
final class VoiceIndicatorPulse {
    static final int RINGS = 2;
    private static final double PULSE_HZ = 0.9;

    private VoiceIndicatorPulse() {
    }

    /** @param radius ring radius as a fraction of the indicator size; @param alpha opacity in [0, 1] */
    record Ring(float radius, float alpha) { }

    static Ring ring(double timeSec, int index, boolean pulse) {
        double offset = (double) index / RINGS;
        double phase = pulse ? (timeSec * PULSE_HZ + offset) % 1.0 : 0.35 + 0.3 * offset;
        float p = (float) phase;
        return new Ring(0.45f + 0.75f * p, pulse ? 1.0f - p : 0.6f - 0.25f * (float) offset);
    }
}
