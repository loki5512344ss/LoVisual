package dev.loki.lovisual.config.io;

import dev.loki.lovisual.util.logging.DebugLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class ConfigSaveScheduler {
    private static final long SAVE_DEBOUNCE_MS = 220L;
    private static final ScheduledExecutorService SAVE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "LoVisual-ConfigSaver");
                t.setDaemon(true);
                return t;
            });
    private static final Object SAVE_LOCK = new Object();
    private static final Map<Path, String> PENDING_TEXT = new HashMap<>();
    private static final Map<Path, ScheduledFuture<?>> PENDING_TASKS = new HashMap<>();
    private static final Map<Path, Long> PENDING_TOKENS = new HashMap<>();
    private static long SAVE_TOKEN_SEQ = 0L;

    private ConfigSaveScheduler() {}

    static void schedule(PendingWrite write) {
        if (write == null) return;
        long token;
        synchronized (SAVE_LOCK) {
            token = ++SAVE_TOKEN_SEQ;
            PENDING_TEXT.put(write.path(), write.text());
            PENDING_TOKENS.put(write.path(), token);
            ScheduledFuture<?> prev = PENDING_TASKS.get(write.path());
            if (prev != null) prev.cancel(false);
            ScheduledFuture<?> task = SAVE_EXECUTOR.schedule(
                    () -> flushPending(write.path(), token),
                    SAVE_DEBOUNCE_MS,
                    TimeUnit.MILLISECONDS
            );
            PENDING_TASKS.put(write.path(), task);
        }
    }

    static void saveNow(PendingWrite write) {
        if (write == null) return;
        clearPending(write.path());
        writeConfig(write.path(), write.text());
    }

    private static void flushPending(Path path, long token) {
        String text;
        synchronized (SAVE_LOCK) {
            Long current = PENDING_TOKENS.get(path);
            if (current == null || current != token) return;
            text = PENDING_TEXT.remove(path);
            PENDING_TOKENS.remove(path);
            PENDING_TASKS.remove(path);
        }
        if (text != null) {
            writeConfig(path, text);
        }
    }

    static void clearPending(Path path) {
        synchronized (SAVE_LOCK) {
            ScheduledFuture<?> prev = PENDING_TASKS.remove(path);
            if (prev != null) prev.cancel(false);
            PENDING_TEXT.remove(path);
            PENDING_TOKENS.remove(path);
        }
    }

    static void writeConfig(Path out, String text) {
        try {
            Path parent = out.getParent();
            Path base = ConfigFilePaths.rootDir();
            Files.createDirectories(parent != null ? parent : base);
            Files.writeString(out, text);
            DebugLog.config("Config saved: %s", out.toAbsolutePath());
        } catch (IOException e) {
            DebugLog.error("Failed to save config %s", e, out.toAbsolutePath());
        }
    }

    record PendingWrite(Path path, String text) {}
}
