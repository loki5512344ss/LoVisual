package dev.loki.lovisual.render.engine.optimize.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-frame draw counters for the F3 overlay — step 1 of Stage 3 in
 * {@code mod/docs/optimize-layer-design.md}.
 *
 * <p>Answers the question the "additional GL optimisations" section asks
 * before anything gets turned on: how many draw calls, pipeline selections
 * and framebuffer binds a frame really costs, and which pipelines dominate
 * them. Sorting draws, batching HUD and dropping redundant FBO switches are
 * all decisions that need these three numbers first.
 *
 * <p>Counting stays off unless the Optimize module is on <em>and</em> F3 is
 * open, so on a normal frame each hook is a single boolean test.
 *
 *
 * <p>Render-thread only: counters are touched by the thread that draws the
 * frame, the published snapshot is one volatile field so the F3 read may
 * happen anywhere.
 */
public final class DrawStats {

    /** Length of one measurement window — averages are published once a second. */
    static final long WINDOW_NS = 1_000_000_000L;

    /** How many pipelines are listed in {@link Snapshot#top()}. */
    static final int TOP = 5;

    /**
     * One second of counting, already averaged per frame.
     *
     * @param draws     GL draw submissions
     * @param pipelines {@code RenderPass.setPipeline} calls — state changes
     * @param binds     {@code glBindFramebuffer} calls that reached the driver
     * @param frames    frames in the window; {@code 0} means "nothing measured yet"
     * @param top       busiest pipelines, busiest first, as {@code "<path> <per-frame avg>"}
     */
    public record Snapshot(int draws, int pipelines, int binds, int frames, List<String> top) {
        public Snapshot {
            top = List.copyOf(top);
        }
    }

    private static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, List.of());

    private static volatile Snapshot snapshot = EMPTY;

    private static boolean active;
    private static int draws;
    private static int pipelines;
    private static int binds;
    private static int frames;
    private static long windowStartNs;
    private static final Map<String, Integer> BY_PIPELINE = new HashMap<>();

    private DrawStats() {
    }

    public static void draw() {
        if (active) draws++;
    }

    /** @param location pipeline location path, already cut down to a short name */
    public static void pipeline(String location) {
        if (active) {
            pipelines++;
            BY_PIPELINE.merge(location, 1, Integer::sum);
        }
    }

    public static void bind() {
        if (active) binds++;
    }

    /**
     * Closes the frame that just finished. {@code shown} is false while F3 is
     * closed or the module is off — the first false call stops counting and
     * empties the published snapshot, the next true call starts a fresh window.
     */
    public static void endFrame(boolean shown) {
        endFrame(shown, System.nanoTime());
    }

    static void endFrame(boolean shown, long nowNs) {
        if (!shown) {
            if (active) {
                active = false;
                snapshot = EMPTY;
                BY_PIPELINE.clear();
            }
            return;
        }
        if (!active) {
            active = true;
            startWindow(nowNs);
            return;
        }
        frames++;
        if (nowNs - windowStartNs >= WINDOW_NS) {
            snapshot = publish(draws, pipelines, binds, frames, BY_PIPELINE, TOP);
            startWindow(nowNs);
        }
    }

    /**
     * Averages the window's counters over its frames and picks the busiest
     * pipelines. Pure — the part of {@link #endFrame(boolean, long)} worth a test.
     *
     * @param byPipeline pipeline location → calls in the window, unordered
     * @param topN       how many entries {@link Snapshot#top()} keeps
     */
    static Snapshot publish(int draws, int pipelines, int binds, int frames,
                            Map<String, Integer> byPipeline, int topN) {
        int perFrame = Math.max(frames, 1);
        List<String> top = new ArrayList<>(Math.min(topN, byPipeline.size()));
        byPipeline.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .forEach(e -> top.add(e.getKey() + " " + avg(e.getValue(), perFrame)));
        return new Snapshot(avg(draws, perFrame), avg(pipelines, perFrame), avg(binds, perFrame), frames, top);
    }

    private static int avg(int total, int frames) {
        return (int) Math.round(total / (double) frames);
    }

    /** Latest published window. */
    public static Snapshot snapshot() {
        return snapshot;
    }

    private static void startWindow(long nowNs) {
        draws = pipelines = binds = frames = 0;
        BY_PIPELINE.clear();
        windowStartNs = nowNs;
    }

    /** Test-only: stop counting, drop counters and the published snapshot. */
    static void reset() {
        active = false;
        snapshot = EMPTY;
        startWindow(0);
    }
}
