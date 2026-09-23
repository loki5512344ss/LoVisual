/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.msaa;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

@Deprecated
public enum MsaaTextureRegistry {
    ;
    private static final Int2IntOpenHashMap SAMPLES = new Int2IntOpenHashMap();

    public static void register(int glId, int samples) {
        if (glId <= 0 || samples <= 1) return;
        SAMPLES.put(glId, samples);
    }

    public static void unregister(int glId) {
        if (glId <= 0) return;
        SAMPLES.remove(glId);
    }

    public static boolean isMsaa(int glId) {
        return glId > 0 && SAMPLES.containsKey(glId);
    }

    public static int getSamples(int glId) {
        return glId > 0 ? SAMPLES.getOrDefault(glId, 1) : 1;
    }
}
