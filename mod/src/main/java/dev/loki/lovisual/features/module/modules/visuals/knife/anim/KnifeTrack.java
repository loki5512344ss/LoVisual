package dev.loki.lovisual.features.module.modules.visuals.knife.anim;

import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeSoundSink;

import java.util.ArrayList;
import java.util.List;

/** Plays one clip at a time: looping, once and stopping, or once and holding the last pose. */
public final class KnifeTrack {
    public enum Mode {LOOP, ONCE_STOP, ONCE_HOLD}

    private final KnifeSoundSink sounds;
    private final List<Object> playing = new ArrayList<>();
    private KnifeClip clip;
    private Mode mode = Mode.ONCE_STOP;
    private float time;
    private int soundCursor;
    private boolean stopped = true;

    public KnifeTrack(KnifeSoundSink sounds) {
        this.sounds = sounds;
    }

    public void play(KnifeClip next, Mode how) {
        silence();
        clip = next;
        mode = how;
        time = 0.0f;
        soundCursor = 0;
        stopped = next == null;
    }

    public void clear() {
        play(null, Mode.ONCE_STOP);
    }

    public boolean isActive() {
        return clip != null && !stopped;
    }

    /** True once a hold-mode clip has reached its end and keeps showing the last pose. */
    public boolean isHolding() {
        return clip != null && mode == Mode.ONCE_HOLD && time >= clip.length();
    }

    public boolean isStopped() {
        return stopped || clip == null;
    }

    public float time() {
        return time;
    }

    /** Jumps to a fraction of the clip, used to tie a run cycle to the distance walked. */
    public void setProgress(float fraction) {
        if (clip != null && clip.length() > 0.0f) time = Math.max(0.0f, Math.min(fraction, 1.0f)) * clip.length();
    }

    public void evaluate(float[] pose) {
        if (clip != null) clip.evaluate(time, pose);
    }

    public void advance(float seconds) {
        if (clip == null || stopped) return;
        float length = clip.length();
        if (length <= 0.0f) {
            fire(0.0f);
            stopped = mode != Mode.ONCE_HOLD;
            return;
        }
        float next = time + seconds;
        if (next < length) {
            time = next;
            fire(next);
            return;
        }
        fire(length);
        switch (mode) {
            case LOOP -> {
                time = next % length;
                soundCursor = 0;
                fire(time);
            }
            case ONCE_HOLD -> time = length;
            case ONCE_STOP -> {
                time = length;
                stopped = true;
            }
        }
    }

    private void fire(float until) {
        if (clip == null) return;
        var source = clip.clip();
        while (soundCursor < source.soundCount() && source.soundTime(soundCursor) <= until) {
            Object handle = sounds.play(source.soundName(soundCursor));
            if (handle != null) {
                playing.removeIf(h -> !sounds.active(h));
                playing.add(handle);
            }
            soundCursor++;
        }
    }

    private void silence() {
        for (Object handle : playing) sounds.stop(handle);
        playing.clear();
    }
}
