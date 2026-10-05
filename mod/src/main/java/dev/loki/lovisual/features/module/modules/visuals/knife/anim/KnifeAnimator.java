package dev.loki.lovisual.features.module.modules.visuals.knife.anim;

import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.ClipSource;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeSoundSink;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeVariant;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * The view-model animation of one hand: a looping idle underneath, one-shot actions (draw, inspect, attacks) on
 * top of it, and an additive run cycle. Switching clips cross-fades from the pose that was showing.
 */
public final class KnifeAnimator {
    /** What the animator reads from the player each frame. */
    public record Motion(boolean sprinting, boolean onGround, float moveDist) {
    }

    private static final float FADE_ACTION = 0.2f;
    private static final float FADE_RUN_START = 0.2f;
    private static final float FADE_RUN_LOOP = 0.2f;
    private static final float FADE_RUN_END = 0.3f;
    private static final float RUN_CYCLE_DISTANCE = 2.0f;
    private static final int PRIMARY_ATTACK_VARIANTS = 2;
    private static final int STRIDE = KnifeClip.STRIDE;

    private enum Main {START, IDLE}

    private enum Run {IDLE, RUNNING}

    private final KnifeTrack base;
    private final KnifeTrack main;
    private final KnifeTrack movement;
    private final RandomGenerator random;

    private ClipSource model;
    private String modelKey;
    private KnifeVariant variant = KnifeVariant.DEFAULT;
    private Main mainState = Main.START;
    private Run runState = Run.IDLE;
    private boolean runCycling;
    private float runAnchor;
    private int attackIndex;

    private float[] staticPose = new float[0];
    private float[] additivePose = new float[0];
    private float[] staticFade = new float[0];
    private float[] additiveFade = new float[0];
    private float[] composed = new float[0];
    private boolean[] hidden = new boolean[0];
    private float staticFadeRemaining;
    private float staticFadeDuration;
    private float additiveFadeRemaining;
    private float additiveFadeDuration;

    public KnifeAnimator(KnifeSoundSink sounds, RandomGenerator random) {
        this.base = new KnifeTrack(sounds);
        this.main = new KnifeTrack(sounds);
        this.movement = new KnifeTrack(sounds);
        this.random = random;
    }

    public boolean isBoundTo(String key) {
        return model != null && key.equals(modelKey);
    }

    public float[] pose() {
        return composed;
    }

    public boolean[] hidden() {
        return hidden;
    }

    public String variantSuffix() {
        return variant.suffix();
    }

    public void reset() {
        model = null;
        modelKey = null;
        base.clear();
        restart();
    }

    /** Starts over with the draw clip, as after picking the knife up. */
    public void restart() {
        mainState = Main.START;
        runState = Run.IDLE;
        runCycling = false;
        attackIndex = 0;
        main.clear();
        movement.clear();
        staticFadeRemaining = 0.0f;
        additiveFadeRemaining = 0.0f;
    }

    public void bind(ClipSource source, String key, List<KnifeVariant> variants) {
        model = source;
        modelKey = key;
        int size = source.boneCount() * STRIDE;
        staticPose = new float[size];
        additivePose = new float[size];
        staticFade = new float[size];
        additiveFade = new float[size];
        composed = new float[size];
        hidden = new boolean[source.boneCount()];
        variant = variants.isEmpty() ? KnifeVariant.DEFAULT : variants.get(random.nextInt(variants.size()));
        source.markBranches(variant.hidden(), hidden);
        restart();
        base.play(source.clip(named("static_idle")), KnifeTrack.Mode.LOOP);
    }

    public void onAttack(boolean primary) {
        if (model == null || mainState != Main.IDLE) return;
        if (primary) {
            playMain(named("melee_" + (attackIndex + 1)), FADE_ACTION);
            attackIndex = (attackIndex + 1) % PRIMARY_ATTACK_VARIANTS;
        } else {
            playMain(named("melee_3"), FADE_ACTION);
        }
    }

    public void onInspect() {
        if (model == null || mainState != Main.IDLE) return;
        String alternate = named("inspect_1");
        playMain(model.hasClip(alternate) && random.nextBoolean() ? alternate : named("inspect"), FADE_ACTION);
    }

    /** Advances the clocks by {@code seconds} and recomposes the pose. */
    public void update(float seconds, Motion motion) {
        if (model == null) return;
        base.advance(seconds);
        main.advance(seconds);
        movement.advance(seconds);
        staticFadeRemaining = Math.max(0.0f, staticFadeRemaining - seconds);
        additiveFadeRemaining = Math.max(0.0f, additiveFadeRemaining - seconds);
        updateMain();
        updateMovement(motion);
        compose();
    }

    private void updateMain() {
        if (mainState != Main.START) return;
        String alternate = named("draw_1");
        playMain(model.hasClip(alternate) && random.nextBoolean() ? alternate : named("draw"), 0.0f);
        mainState = Main.IDLE;
    }

    private void updateMovement(Motion motion) {
        boolean canRun = model.hasClip(named("run_start")) && motion.sprinting() && motion.onGround() && main.isStopped();
        if (runState == Run.IDLE) {
            if (!canRun) return;
            runState = Run.RUNNING;
            runCycling = false;
            playMovement(named("run_start"), KnifeTrack.Mode.ONCE_HOLD, FADE_RUN_START);
            return;
        }
        if (!canRun) {
            runState = Run.IDLE;
            runCycling = false;
            playMovement(named("run_end"), KnifeTrack.Mode.ONCE_HOLD, FADE_RUN_END);
            return;
        }
        if (!runCycling) {
            if (!movement.isHolding()) return;
            runCycling = true;
            runAnchor = motion.moveDist();
            playMovement(named("run"), KnifeTrack.Mode.LOOP, FADE_RUN_LOOP);
        }
        float travelled = (motion.moveDist() - runAnchor) % RUN_CYCLE_DISTANCE;
        if (travelled < 0.0f) travelled += RUN_CYCLE_DISTANCE;
        movement.setProgress(travelled / RUN_CYCLE_DISTANCE);
    }

    private void playMain(String clip, float fade) {
        KnifeClip next = model.clip(clip);
        if (next == null) return;
        beginStaticFade(fade);
        main.play(next, KnifeTrack.Mode.ONCE_STOP);
    }

    private void playMovement(String clip, KnifeTrack.Mode mode, float fade) {
        KnifeClip next = model.clip(clip);
        if (next == null) {
            movement.clear();
            return;
        }
        beginAdditiveFade(fade);
        movement.play(next, mode);
    }

    private void beginStaticFade(float duration) {
        if (duration <= 0.0f) {
            staticFadeRemaining = 0.0f;
            return;
        }
        System.arraycopy(staticPose, 0, staticFade, 0, staticPose.length);
        staticFadeDuration = duration;
        staticFadeRemaining = duration;
    }

    private void beginAdditiveFade(float duration) {
        if (duration <= 0.0f) {
            additiveFadeRemaining = 0.0f;
            return;
        }
        System.arraycopy(additivePose, 0, additiveFade, 0, additivePose.length);
        additiveFadeDuration = duration;
        additiveFadeRemaining = duration;
    }

    private void compose() {
        neutral(staticPose);
        base.evaluate(staticPose);
        if (main.isActive()) main.evaluate(staticPose);
        blend(staticPose, staticFade, staticFadeRemaining, staticFadeDuration);

        neutral(additivePose);
        if (movement.isActive() || movement.isHolding()) movement.evaluate(additivePose);
        blend(additivePose, additiveFade, additiveFadeRemaining, additiveFadeDuration);

        for (int at = 0; at + STRIDE <= composed.length; at += STRIDE) {
            for (int k = 0; k < 6; k++) composed[at + k] = staticPose[at + k] + additivePose[at + k];
            for (int k = 6; k < STRIDE; k++) composed[at + k] = staticPose[at + k] * additivePose[at + k];
        }
    }

    static void neutral(float[] pose) {
        Arrays.fill(pose, 0.0f);
        for (int at = KnifeClip.SCALE; at + 3 <= pose.length; at += STRIDE) {
            pose[at] = 1.0f;
            pose[at + 1] = 1.0f;
            pose[at + 2] = 1.0f;
        }
    }

    /** Pulls {@code pose} back towards {@code from} while a fade is running. */
    static void blend(float[] pose, float[] from, float remaining, float duration) {
        if (duration <= 0.0f || remaining <= 0.0f) return;
        float t = 1.0f - remaining / duration;
        for (int i = 0; i < pose.length; i++) pose[i] = from[i] + (pose[i] - from[i]) * t;
    }

    private String named(String clip) {
        String suffixed = clip + variant.suffix();
        return !variant.suffix().isEmpty() && model != null && model.hasClip(suffixed) ? suffixed : clip;
    }
}
