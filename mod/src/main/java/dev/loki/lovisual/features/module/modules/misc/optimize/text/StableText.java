package dev.loki.lovisual.features.module.modules.misc.optimize.text;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Marker {@link FormattedCharSequence} wrapper that lets later code recognise
 * "this text won't change" by identity and skip BIDI re-shaping. The
 * {@link #mark(FormattedCharSequence)} call wraps and registers the sequence
 * in a {@link WeakHashMap}-backed set; {@link #isStable(FormattedCharSequence)}
 * checks membership.
 *
 * <p>The fast path is a plain {@code instanceof}
 * check on the wrapper; the slow path is a synchronised set lookup. LoVisual
 * keeps both — Stage 1 wires them both to {@link OptimizeToggles#microOpts()}.
 */
public final class StableText implements FormattedCharSequence {

    private static final Set<FormattedCharSequence> STABLE =
            Collections.newSetFromMap(new WeakHashMap<>());

    private final FormattedCharSequence text;

    /** Package-private — use {@link #mark(FormattedCharSequence)} to construct. */
    StableText(FormattedCharSequence text) {
        this.text = text;
    }

    /**
     * Wrap a {@link FormattedCharSequence} so {@link #isStable(FormattedCharSequence)}
     * returns {@code true} for it. The wrapper is registered in a weak set so
     * it doesn't leak when the caller drops its reference.
     */
    public static FormattedCharSequence mark(FormattedCharSequence text) {
        StableText stable = new StableText(text);
        synchronized (STABLE) {
            STABLE.add(stable);
        }
        return stable;
    }

    /**
     * True when {@code text} is known not to change (so callers can skip BIDI
     * re-shape and other per-frame work). Fast path: {@code instanceof StableText}
     * when {@code microOpts} is on; slow path: synchronised set lookup otherwise.
     */
    public static boolean isStable(FormattedCharSequence text) {
        if (OptimizeToggles.microOpts()) {
            return text instanceof StableText;
        }
        synchronized (STABLE) {
            return STABLE.contains(text);
        }
    }

    /** Test-only: number of currently-tracked stable sequences. */
    static int trackedCount() {
        synchronized (STABLE) {
            return STABLE.size();
        }
    }

    /** Test-only: clear the tracked set. */
    static void clearTracked() {
        synchronized (STABLE) {
            STABLE.clear();
        }
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return text.accept(sink);
    }
}
