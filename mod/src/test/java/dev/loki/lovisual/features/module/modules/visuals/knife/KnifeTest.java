package dev.loki.lovisual.features.module.modules.visuals.knife;

import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeAnimator;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeClip;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeTrack;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.ClipSource;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeSoundSink;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeVariant;
import dev.loki.lovisual.util.bedrock.anim.Clip;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class KnifeTest {
    private static final KnifeAnimator.Motion STILL = new KnifeAnimator.Motion(false, true, 0.0f);

    /** One bone "b" whose x rotation is set by whichever clip plays; the model has the clips given. */
    private static final class FakeModel implements ClipSource {
        final Map<String, KnifeClip> clips = new HashMap<>();

        FakeModel(String... names) {
            for (String name : names) {
                float angle = name.hashCode() % 90;
                String json = "{\"" + name + "\":{\"loop\":true,\"animation_length\":1,\"bones\":{\"b\":{\"rotation\":{\"0\":[" + angle
                        + ",0,0],\"1\":[" + angle + ",0,0]}}}}}";
                clips.putAll(parse(json));
            }
        }

        private static Map<String, KnifeClip> parse(String json) {
            Map<String, KnifeClip> out = new HashMap<>();
            for (Map.Entry<String, Clip> entry : Clip.parseAll(json).entrySet()) {
                out.put(entry.getKey(), new KnifeClip(entry.getValue(), Map.of("b", 0)));
            }
            return out;
        }

        @Override
        public KnifeClip clip(String name) {
            return clips.get(name);
        }

        @Override
        public int boneCount() {
            return 1;
        }

        @Override
        public void markBranches(List<String> names, boolean[] flags) {
            flags[0] = names.contains("b");
        }

        float angleOf(String name) {
            float[] pose = new float[9];
            clips.get(name).evaluate(0.0f, pose);
            return pose[0];
        }
    }

    /** Lets the draw clip play out so the idle loop is showing. */
    private static void settle(KnifeAnimator animator) {
        animator.update(0.0f, STILL);
        animator.update(2.0f, STILL);
    }

    private static KnifeAnimator animator(FakeModel model, List<KnifeVariant> variants) {
        KnifeAnimator animator = new KnifeAnimator(KnifeSoundSink.SILENT, RandomGenerator.getDefault());
        animator.bind(model, "test", variants);
        return animator;
    }

    @Test
    void drawPlaysFirstAndGivesWayToIdle() {
        FakeModel model = new FakeModel("draw", "static_idle");
        KnifeAnimator animator = animator(model, List.of());
        animator.update(0.1f, STILL);
        assertEquals(model.angleOf("draw"), animator.pose()[0], 1e-4f);
        animator.update(2.0f, STILL);
        assertEquals(model.angleOf("static_idle"), animator.pose()[0], 1e-4f);
    }

    @Test
    void primaryAttacksAlternateAndSecondaryUsesTheThird() {
        FakeModel model = new FakeModel("draw", "static_idle", "melee_1", "melee_2", "melee_3");
        KnifeAnimator animator = animator(model, List.of());
        settle(animator);
        animator.onAttack(true);
        animator.update(0.5f, STILL);
        assertEquals(model.angleOf("melee_1"), animator.pose()[0], 1e-3f);
        animator.update(2.0f, STILL);
        animator.onAttack(true);
        animator.update(0.5f, STILL);
        assertEquals(model.angleOf("melee_2"), animator.pose()[0], 1e-3f);
        animator.update(2.0f, STILL);
        animator.onAttack(false);
        animator.update(0.5f, STILL);
        assertEquals(model.angleOf("melee_3"), animator.pose()[0], 1e-3f);
    }

    @Test
    void actionsFadeInFromThePreviousPose() {
        FakeModel model = new FakeModel("draw", "static_idle", "melee_1");
        KnifeAnimator animator = animator(model, List.of());
        settle(animator);
        animator.onAttack(true);
        animator.update(0.1f, STILL);
        float idle = model.angleOf("static_idle");
        float melee = model.angleOf("melee_1");
        assertEquals(idle + (melee - idle) * 0.5f, animator.pose()[0], 1e-3f);
        animator.update(0.2f, STILL);
        assertEquals(melee, animator.pose()[0], 1e-3f);
    }

    @Test
    void variantsPickSuffixedClipsAndHideBones() {
        FakeModel model = new FakeModel("draw", "draw_angel", "static_idle", "static_idle_angel");
        KnifeAnimator animator = animator(model, List.of(new KnifeVariant("_angel", List.of("b"))));
        animator.update(0.1f, STILL);
        assertEquals(model.angleOf("draw_angel"), animator.pose()[0], 1e-4f);
        assertTrue(animator.hidden()[0]);
        assertEquals("_angel", animator.variantSuffix());
    }

    @Test
    void sprintingRunsAdditivelyAndStopsWithRunEnd() {
        FakeModel model = new FakeModel("draw", "static_idle", "run_start", "run", "run_end");
        KnifeAnimator animator = animator(model, List.of());
        settle(animator);
        float idle = animator.pose()[0];
        KnifeAnimator.Motion sprint = new KnifeAnimator.Motion(true, true, 0.0f);
        animator.update(2.0f, sprint);
        animator.update(2.0f, sprint);
        animator.update(0.5f, sprint);
        assertEquals(idle + model.angleOf("run"), animator.pose()[0], 1e-3f);
        animator.update(2.0f, STILL);
        animator.update(2.0f, STILL);
        assertEquals(idle + model.angleOf("run_end"), animator.pose()[0], 1e-3f);
    }

    @Test
    void neutralPoseHasUnitScale() {
        FakeModel model = new FakeModel("static_idle");
        KnifeAnimator animator = animator(model, List.of());
        animator.update(0.1f, STILL);
        assertEquals(1.0f, animator.pose()[6], 1e-6f);
        assertEquals(1.0f, animator.pose()[8], 1e-6f);
    }

    @Test
    void trackModesLoopStopAndHold() {
        FakeModel model = new FakeModel("c");
        KnifeClip clip = model.clip("c");
        KnifeTrack loop = new KnifeTrack(KnifeSoundSink.SILENT);
        loop.play(clip, KnifeTrack.Mode.LOOP);
        loop.advance(2.5f);
        assertTrue(loop.isActive());
        assertEquals(0.5f, loop.time(), 1e-5f);
        KnifeTrack once = new KnifeTrack(KnifeSoundSink.SILENT);
        once.play(clip, KnifeTrack.Mode.ONCE_STOP);
        once.advance(2.0f);
        assertTrue(once.isStopped());
        KnifeTrack hold = new KnifeTrack(KnifeSoundSink.SILENT);
        hold.play(clip, KnifeTrack.Mode.ONCE_HOLD);
        hold.advance(2.0f);
        assertTrue(hold.isHolding());
        assertTrue(hold.isActive());
    }

    @Test
    void soundsFireOnceInOrderAndAreStoppedWhenReplaced() {
        List<String> played = new ArrayList<>();
        List<Object> stopped = new ArrayList<>();
        KnifeSoundSink sink = new KnifeSoundSink() {
            @Override
            public Object play(String effect) {
                played.add(effect);
                return effect;
            }

            @Override
            public boolean active(Object handle) {
                return true;
            }

            @Override
            public void stop(Object handle) {
                stopped.add(handle);
            }
        };
        Clip clip = Clip.parseAll("{\"x\":{\"animation_length\":1,\"bones\":{\"b\":{}},"
                + "\"sound_effects\":{\"0.0\":{\"effect\":\"first\"},\"0.5\":{\"effect\":\"second\"}}}}").get("x");
        KnifeTrack track = new KnifeTrack(sink);
        track.play(new KnifeClip(clip, Map.of("b", 0)), KnifeTrack.Mode.ONCE_STOP);
        track.advance(0.1f);
        assertEquals(List.of("first"), played);
        track.advance(0.5f);
        assertEquals(List.of("first", "second"), played);
        track.clear();
        assertEquals(List.of("first", "second"), stopped);
    }
}
