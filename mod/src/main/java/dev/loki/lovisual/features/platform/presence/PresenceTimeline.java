package dev.loki.lovisual.features.platform.presence;

import java.util.ArrayDeque;

/**
 * Playback buffer of one remote player's cursor. Payloads arrive in batches (about 3 per second) carrying samples
 * spaced {@link #SAMPLE_MS} apart; the renderer plays them back {@link #DELAY_MS} late and interpolates, so the
 * remote cursor moves smoothly instead of in steps.
 */
public final class PresenceTimeline {

    public static final long SAMPLE_MS = 100L;
    public static final long DELAY_MS = 300L;
    /** A remote menu counts as gone after this long without a fresh payload. */
    public static final long STALE_MS = 2500L;
    private static final long MAX_BUFFER_MS = 1500L;
    /** The same server entry polled twice differs only by clock jitter; payloads closer than this are repeats. */
    private static final long MIN_GAP_MS = 40L;

    public record Point(float x, float y, boolean pressed, int scroll) {
    }

    private record Timed(long t, PresenceCodec.Sample sample) {
    }

    private final ArrayDeque<Timed> buffer = new ArrayDeque<>();
    private long lastSend = Long.MIN_VALUE;
    private long lastSeen = Long.MIN_VALUE;
    private PresenceCodec.Gui meta;

    /**
     * @param sendMs local-clock time the sender pushed this payload (now minus the server-reported age)
     * @return false when this payload is a repeat of one already consumed
     */
    public boolean add(long sendMs, PresenceCodec.Gui gui) {
        if (sendMs < lastSend + MIN_GAP_MS) return false;
        lastSend = sendMs;
        lastSeen = sendMs;
        meta = gui;
        int n = gui.samples().size();
        for (int i = 0; i < n; i++) {
            long t = sendMs - (long) (n - 1 - i) * SAMPLE_MS;
            if (buffer.isEmpty() || t > buffer.peekLast().t()) buffer.addLast(new Timed(t, gui.samples().get(i)));
        }
        while (!buffer.isEmpty() && sendMs - buffer.peekFirst().t() > MAX_BUFFER_MS) buffer.pollFirst();
        return true;
    }

    public PresenceCodec.Gui meta() {
        return meta;
    }

    /** True while the remote menu is open (a payload not marked closed arrived recently). */
    public boolean live(long nowMs) {
        return meta != null && !meta.closed() && nowMs - lastSeen < STALE_MS;
    }

    /** True when any payload (menu open or not) arrived recently, i.e. the player runs LoVisual. */
    public boolean present(long nowMs) {
        return meta != null && nowMs - lastSeen < STALE_MS * 4;
    }

    /** Interpolated cursor at {@code nowMs - DELAY_MS}, or null with nothing buffered. */
    public Point at(long nowMs) {
        if (buffer.isEmpty()) return null;
        long t = nowMs - DELAY_MS;
        Timed prev = null;
        for (Timed next : buffer) {
            if (next.t() >= t) {
                if (prev == null) return point(next.sample());
                float k = (float) (t - prev.t()) / Math.max(1L, next.t() - prev.t());
                return new Point(lerp(prev.sample().x(), next.sample().x(), k), lerp(prev.sample().y(), next.sample().y(), k),
                        next.sample().pressed(), next.sample().scroll());
            }
            prev = next;
        }
        return point(prev.sample());
    }

    private static Point point(PresenceCodec.Sample s) {
        return new Point(s.x(), s.y(), s.pressed(), s.scroll());
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * Math.max(0.0f, Math.min(1.0f, k));
    }
}
