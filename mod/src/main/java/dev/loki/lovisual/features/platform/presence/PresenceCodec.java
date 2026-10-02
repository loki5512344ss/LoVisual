package dev.loki.lovisual.features.platform.presence;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Wire format of the GUI presence payload (see {@code backend/chat-service/PLAN.md}): 4 header bytes and up to
 * 8 cursor samples of 4 bytes. Pure and symmetric, so it is unit-tested without Minecraft.
 */
public final class PresenceCodec {

    public static final int VERSION = 1;
    public static final int MAX_SAMPLES = 8;
    public static final int NO_MODULE = 255;
    private static final int HEADER = 4;

    /** Cursor position inside the menu (0..255 on each axis), left button state and list scroll (0..255). */
    public record Sample(int x, int y, boolean pressed, int scroll) {
    }

    /**
     * @param category    menu column 0..15 (-1 is stored as 15 by {@link #encode} and decoded back to -1)
     * @param moduleIndex index of the module row under the cursor, {@link #NO_MODULE} when none
     */
    public record Gui(int category, boolean settings, boolean searching, boolean closed, int moduleIndex,
                      List<Sample> samples) {
    }

    private PresenceCodec() {
    }

    public static String encode(Gui gui) {
        int n = Math.min(MAX_SAMPLES, gui.samples().size());
        byte[] out = new byte[HEADER + n * 4];
        out[0] = VERSION;
        int cat = gui.category() < 0 ? 15 : gui.category() & 15;
        out[1] = (byte) (cat | (gui.settings() ? 16 : 0) | (gui.searching() ? 32 : 0) | (gui.closed() ? 128 : 0));
        out[2] = (byte) gui.moduleIndex();
        out[3] = (byte) n;
        for (int i = 0; i < n; i++) {
            Sample s = gui.samples().get(i);
            out[HEADER + i * 4] = (byte) clamp(s.x());
            out[HEADER + i * 4 + 1] = (byte) clamp(s.y());
            out[HEADER + i * 4 + 2] = (byte) (s.pressed() ? 1 : 0);
            out[HEADER + i * 4 + 3] = (byte) clamp(s.scroll());
        }
        return Base64.getEncoder().encodeToString(out);
    }

    /** Returns null for anything malformed or from another protocol version. */
    public static Gui decode(String base64) {
        if (base64 == null) return null;
        byte[] in;
        try {
            in = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (in.length < HEADER || in[0] != VERSION) return null;
        int n = in[3] & 0xFF;
        if (n > MAX_SAMPLES || in.length < HEADER + n * 4) return null;
        int flags = in[1] & 0xFF;
        int cat = flags & 15;
        List<Sample> samples = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int o = HEADER + i * 4;
            samples.add(new Sample(in[o] & 0xFF, in[o + 1] & 0xFF, (in[o + 2] & 1) != 0, in[o + 3] & 0xFF));
        }
        return new Gui(cat == 15 ? -1 : cat, (flags & 16) != 0, (flags & 32) != 0, (flags & 128) != 0,
                in[2] & 0xFF, samples);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
