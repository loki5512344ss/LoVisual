package dev.loki.lovisual.features.module.modules.visuals.knife.model;

import dev.loki.lovisual.util.logging.DebugLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Loads and bakes knife models on a background thread; {@link #get} returns null until a model is ready. */
public final class KnifeModels {
    private static final String ROOT = "/assets/lovisual/knife/";
    private static final Map<String, KnifeModel> READY = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private KnifeModels() {
    }

    public static KnifeModel get(String modelKey) {
        KnifeModel ready = READY.get(modelKey);
        if (ready != null || FAILED.contains(modelKey)) return ready;
        if (PENDING.add(modelKey)) {
            Thread loader = new Thread(() -> load(modelKey), "lovisual-knife-loader");
            loader.setDaemon(true);
            loader.start();
        }
        return null;
    }

    public static void clear() {
        READY.clear();
        FAILED.clear();
    }

    private static void load(String modelKey) {
        try {
            READY.put(modelKey, KnifeModel.bake(read(modelKey + ".geo.json"), read(modelKey + ".anim.json")));
        } catch (IOException | RuntimeException e) {
            FAILED.add(modelKey);
            DebugLog.warn("[LoVisual] Knife model '" + modelKey + "' failed to load: " + e);
        } finally {
            PENDING.remove(modelKey);
        }
    }

    private static String read(String file) throws IOException {
        try (InputStream in = KnifeModels.class.getResourceAsStream(ROOT + file)) {
            if (in == null) throw new IOException("missing resource " + file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
