package dev.loki.lovisual.render.engine.renderer.ui.runtime.reconcile;

public final class UiDiffStats {
    private int mounted;
    private int reused;
    private int replaced;
    private int unmounted;

    public int mounted() {
        return mounted;
    }

    public int reused() {
        return reused;
    }

    public int replaced() {
        return replaced;
    }

    public int unmounted() {
        return unmounted;
    }

    void noteMounted() {
        mounted++;
    }

    void noteReused() {
        reused++;
    }

    void noteReplaced() {
        replaced++;
    }

    void noteUnmounted() {
        unmounted++;
    }

    public void reset() {
        mounted = 0;
        reused = 0;
        replaced = 0;
        unmounted = 0;
    }
}
