package dev.loki.lovisual.render.engine.optimize.profile;

import java.util.EnumMap;
import java.util.Map;

/**
 * Section-level frame profiler: times five render subsystems
 * ({@link FrameBudget.SubSystem#ENTITIES ENTITIES}, {@link FrameBudget.SubSystem#PARTICLES PARTICLES},
 * {@link FrameBudget.SubSystem#HUD HUD}, {@link FrameBudget.SubSystem#EFFECTS EFFECTS},
 * {@link FrameBudget.SubSystem#TEXT TEXT}) per frame and exposes a stable
 * EMA-averaged snapshot for the F3 debug overlay (see {@code OptimizeDebugHudMixin}).
 *
 * <p>Use the {@link Section} try-with-resources idiom:
 * <pre>{@code
 * try (FrameBudget.Section s = FrameProfiler.section(FrameBudget.SubSystem.PARTICLES)) {
 *     // ... extract particles ...
 * }
 * }</pre>
 *
 * <p>Stage 2 of {@code mod/docs/optimize-layer-design.md}.
 *
 * <p>Thread-safety: render-thread only — the same thread that calls
 * {@link #beginFrame()} / {@link #endFrame()} is the one that opens/closes
 * sections. The snapshot is published via a single volatile field so a
 * concurrent F3-thread read always sees a consistent map.
 */
public enum FrameProfiler {
    ;

    /** EMA weight for the per-section timing — same as {@code HudWorth}'s 0.95/0.05. */
    private static final double EMA_ALPHA = 0.05;

    private static final int SAMPLE_DECAY_MS = 2000; // forget a section if idle for 2 s

    /** Read by the F3 overlay via {@link #snapshot()}. */
    public static final class Snapshot {
        private final double entityMs;
        private final double particleMs;
        private final double hudMs;
        private final double effectMs;
        private final double textMs;
        private final double frameMs;

        Snapshot(double e, double p, double h, double f, double t, double frame) {
            this.entityMs = e;
            this.particleMs = p;
            this.hudMs = h;
            this.effectMs = f;
            this.textMs = t;
            this.frameMs = frame;
        }

        public double entityMs()  { return entityMs; }
        public double particleMs(){ return particleMs; }
        public double hudMs()      { return hudMs; }
        public double effectMs()   { return effectMs; }
        public double textMs()      { return textMs; }
        public double frameMs()     { return frameMs; }
    }

    private static final EnumMap<FrameBudget.SubSystem, Double> EMA_MS = new EnumMap<>(FrameBudget.SubSystem.class);
    private static final EnumMap<FrameBudget.SubSystem, Long> LAST_SEEN_MS = new EnumMap<>(FrameBudget.SubSystem.class);

    private static volatile Snapshot snapshot = new Snapshot(0, 0, 0, 0, 0, 0);

    private static long frameStartNs;
    private static long frameEndNs;
    private static double frameMs;

    // Section state — render-thread only, so plain non-volatile fields are enough.
    private static FrameBudget.SubSystem currentSection;
    private static long sectionStartNs;
    private static long sectionAccumulatorNs;

    /** Begin a new frame — clears accumulators and records the start time. */
    public static void beginFrame() {
        frameStartNs = System.nanoTime();
        for (FrameBudget.SubSystem s : FrameBudget.SubSystem.values()) {
            // We don't clear EMA_MS here — sections that aren't seen this frame decay
            // based on LAST_SEEN_MS, see endFrame().
        }
    }

    /** End the frame — decays idle sections, publishes the new snapshot. */
    public static void endFrame() {
        frameEndNs = System.nanoTime();
        long frameNs = frameEndNs - frameStartNs;
        frameMs = frameNs / 1_000_000.0;

        long nowMs = frameEndNs / 1_000_000L;
        double e = decayed(FrameBudget.SubSystem.ENTITIES,  nowMs);
        double p = decayed(FrameBudget.SubSystem.PARTICLES, nowMs);
        double h = decayed(FrameBudget.SubSystem.HUD,       nowMs);
        double f = decayed(FrameBudget.SubSystem.EFFECTS,   nowMs);
        double t = decayed(FrameBudget.SubSystem.TEXT,      nowMs);
        snapshot = new Snapshot(e, p, h, f, t, frameMs);
    }

    private static double decayed(FrameBudget.SubSystem s, long nowMs) {
        Long last = LAST_SEEN_MS.get(s);
        if (last != null && nowMs - last > SAMPLE_DECAY_MS) {
            EMA_MS.remove(s);
            return 0;
        }
        Double v = EMA_MS.get(s);
        return v == null ? 0 : v;
    }

    /**
     * Open a section. The previous section (if any) is closed first so
     * nested {@code section(...)} calls on different subsystems accumulate
     * correctly without leaking time across sections.
     */
    public static Section section(FrameBudget.SubSystem subsystem) {
        if (currentSection != null) {
            closeSection();
        }
        currentSection = subsystem;
        sectionStartNs = System.nanoTime();
        sectionAccumulatorNs = 0;
        return new Section(subsystem);
    }

    /** Close the currently-open section — pushed into the EMA. */
    static void closeSection() {
        if (currentSection == null) return;
        long elapsedNs = System.nanoTime() - sectionStartNs + sectionAccumulatorNs;
        double elapsedMs = elapsedNs / 1_000_000.0;
        Double prev = EMA_MS.get(currentSection);
        double next = prev == null ? elapsedMs : prev * (1 - EMA_ALPHA) + elapsedMs * EMA_ALPHA;
        EMA_MS.put(currentSection, next);
        LAST_SEEN_MS.put(currentSection, System.nanoTime() / 1_000_000L);
        currentSection = null;
        sectionStartNs = 0;
        sectionAccumulatorNs = 0;
    }

    /** Suspend the current section (e.g. when calling into another subsystem). */
    static void suspend() {
        if (currentSection == null) return;
        sectionAccumulatorNs += System.nanoTime() - sectionStartNs;
        sectionStartNs = 0;
    }

    /** Resume the current section after a {@link #suspend()}. */
    static void resume() {
        if (currentSection == null) return;
        sectionStartNs = System.nanoTime();
    }

    /** Read-only snapshot for the F3 overlay. */
    public static Snapshot snapshot() {
        return snapshot;
    }

    /** Test-only: reset EMA + section state. */
    static void reset() {
        EMA_MS.clear();
        LAST_SEEN_MS.clear();
        snapshot = new Snapshot(0, 0, 0, 0, 0, 0);
        currentSection = null;
        sectionStartNs = 0;
        sectionAccumulatorNs = 0;
        frameStartNs = 0;
        frameEndNs = 0;
        frameMs = 0;
    }

    /**
     * Try-with-resources section. {@code close()} closes the current section
     * in {@link FrameProfiler}; nested usage of the same subsystem is fine
     * because each {@code close()} only ends the most recent open section.
     */
    public static final class Section implements AutoCloseable {
        private final FrameBudget.SubSystem subsystem;
        private boolean closed;

        Section(FrameBudget.SubSystem subsystem) {
            this.subsystem = subsystem;
        }

        public FrameBudget.SubSystem subsystem() {
            return subsystem;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (currentSection == subsystem) {
                closeSection();
            }
        }
    }
}
