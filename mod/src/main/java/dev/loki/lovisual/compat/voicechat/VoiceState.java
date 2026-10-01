package dev.loki.lovisual.compat.voicechat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Thread-safe snapshot of who is talking in Simple Voice Chat. Written from the voice
 * threads by the plugin, read from the render thread by the module and the HUD. Holds no
 * Simple Voice Chat or Minecraft types, so it stays testable and safe to load without the mod.
 */
public final class VoiceState {

    /** State of the local microphone, ordered by display priority. */
    public enum MicStatus { DISCONNECTED, DISABLED, MUTED, WHISPERING, TALKING, PUSH_TO_TALK, IDLE }

    /** One remote speaker; {@code lastMs} is when audio from them was last received. */
    public record Speaker(UUID id, boolean whisper, long lastMs) { }

    private record Voice(boolean whisper, long lastMs) { }

    private static final Comparator<Speaker> NEWEST_FIRST = Comparator.comparingLong(Speaker::lastMs).reversed();

    private final LongSupplier clock;
    private final Map<UUID, Voice> speakers = new ConcurrentHashMap<>();
    private volatile Voice self = new Voice(false, Long.MIN_VALUE);
    private volatile boolean connected;
    private volatile boolean muted;
    private volatile boolean disabled;
    private volatile boolean pushToTalk;

    public VoiceState(LongSupplier clock) {
        this.clock = clock;
    }

    public void markSpeaking(UUID id, boolean whisper) {
        if (id != null) speakers.put(id, new Voice(whisper, clock.getAsLong()));
    }

    public void markSelfSpeaking(boolean whisper) {
        self = new Voice(whisper, clock.getAsLong());
    }

    public void setConnected(boolean value) {
        connected = value;
        if (!value) speakers.clear();
    }

    public void setMuted(boolean value) { muted = value; }

    public void setDisabled(boolean value) { disabled = value; }

    public void setPushToTalk(boolean value) { pushToTalk = value; }

    public boolean isConnected() { return connected; }

    public void clear() {
        speakers.clear();
        self = new Voice(false, Long.MIN_VALUE);
    }

    public boolean isSpeaking(UUID id, long windowMs) {
        Voice v = speakers.get(id);
        return v != null && age(v) <= windowMs;
    }

    public boolean isWhispering(UUID id, long windowMs) {
        Voice v = speakers.get(id);
        return v != null && v.whisper() && age(v) <= windowMs;
    }

    /** Fade factor: 1 while audio is arriving, falling linearly to 0 over {@code fadeMs} after it stops. */
    public float level(UUID id, long fadeMs) {
        Voice v = speakers.get(id);
        return v == null ? 0f : fade(age(v), fadeMs);
    }

    public float selfLevel(long fadeMs) {
        return fade(age(self), fadeMs);
    }

    public boolean isSelfWhispering(long windowMs) {
        Voice v = self;
        return v.whisper() && age(v) <= windowMs;
    }

    /** Remote speakers heard within {@code windowMs}, newest first; stale entries are dropped. */
    public List<Speaker> speakers(long windowMs, int max) {
        List<Speaker> out = new ArrayList<>();
        speakers.entrySet().removeIf(e -> age(e.getValue()) > Math.max(windowMs * 4, 10_000L));
        for (Map.Entry<UUID, Voice> e : speakers.entrySet()) {
            if (age(e.getValue()) <= windowMs) {
                out.add(new Speaker(e.getKey(), e.getValue().whisper(), e.getValue().lastMs()));
            }
        }
        out.sort(NEWEST_FIRST);
        return out.size() > max ? new ArrayList<>(out.subList(0, Math.max(0, max))) : out;
    }

    public MicStatus micStatus(long windowMs) {
        if (!connected) return MicStatus.DISCONNECTED;
        if (disabled) return MicStatus.DISABLED;
        if (muted) return MicStatus.MUTED;
        if (age(self) <= windowMs) return self.whisper() ? MicStatus.WHISPERING : MicStatus.TALKING;
        return pushToTalk ? MicStatus.PUSH_TO_TALK : MicStatus.IDLE;
    }

    private long age(Voice v) {
        return v.lastMs() == Long.MIN_VALUE ? Long.MAX_VALUE : clock.getAsLong() - v.lastMs();
    }

    private static float fade(long ageMs, long fadeMs) {
        if (ageMs == Long.MAX_VALUE || ageMs > fadeMs) return 0f;
        return fadeMs <= 0 ? 1f : 1f - (float) ageMs / fadeMs;
    }
}
