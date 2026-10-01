package dev.loki.lovisual.compat.voicechat;

import dev.loki.lovisual.compat.voicechat.VoiceState.MicStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class VoiceStateTest {
    private final AtomicLong now = new AtomicLong(1_000);
    private final VoiceState state = new VoiceState(now::get);
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void speakingExpiresAfterWindow() {
        state.markSpeaking(a, false);
        assertTrue(state.isSpeaking(a, 300));
        now.addAndGet(301);
        assertFalse(state.isSpeaking(a, 300));
        assertFalse(state.isSpeaking(b, 300));
    }

    @Test
    void whisperFlagFollowsLastPacket() {
        state.markSpeaking(a, true);
        assertTrue(state.isWhispering(a, 300));
        state.markSpeaking(a, false);
        assertFalse(state.isWhispering(a, 300));
    }

    @Test
    void levelFadesLinearly() {
        state.markSpeaking(a, false);
        assertEquals(1f, state.level(a, 400), 1e-6);
        now.addAndGet(100);
        assertEquals(0.75f, state.level(a, 400), 1e-6);
        now.addAndGet(400);
        assertEquals(0f, state.level(a, 400), 1e-6);
        assertEquals(0f, state.level(b, 400), 1e-6);
    }

    @Test
    void speakersAreNewestFirstAndCapped() {
        state.markSpeaking(a, false);
        now.addAndGet(50);
        state.markSpeaking(b, true);
        List<VoiceState.Speaker> all = state.speakers(500, 8);
        assertEquals(List.of(b, a), all.stream().map(VoiceState.Speaker::id).toList());
        assertTrue(all.get(0).whisper());
        assertEquals(1, state.speakers(500, 1).size());
        assertEquals(b, state.speakers(500, 1).get(0).id());
        now.addAndGet(1_000);
        assertTrue(state.speakers(500, 8).isEmpty());
    }

    @Test
    void micStatusPriority() {
        assertEquals(MicStatus.DISCONNECTED, state.micStatus(300));
        state.setConnected(true);
        assertEquals(MicStatus.IDLE, state.micStatus(300));
        state.setPushToTalk(true);
        assertEquals(MicStatus.PUSH_TO_TALK, state.micStatus(300));
        state.markSelfSpeaking(false);
        assertEquals(MicStatus.TALKING, state.micStatus(300));
        state.markSelfSpeaking(true);
        assertEquals(MicStatus.WHISPERING, state.micStatus(300));
        assertTrue(state.isSelfWhispering(300));
        state.setMuted(true);
        assertEquals(MicStatus.MUTED, state.micStatus(300));
        state.setDisabled(true);
        assertEquals(MicStatus.DISABLED, state.micStatus(300));
        state.setConnected(false);
        assertEquals(MicStatus.DISCONNECTED, state.micStatus(300));
    }

    @Test
    void disconnectAndClearDropEverything() {
        state.setConnected(true);
        state.markSpeaking(a, false);
        state.markSelfSpeaking(false);
        state.clear();
        assertFalse(state.isSpeaking(a, 300));
        assertEquals(0f, state.selfLevel(300), 1e-6);
        state.markSpeaking(a, false);
        state.setConnected(false);
        assertTrue(state.speakers(300, 8).isEmpty());
    }

    @Test
    void concurrentWritersDoNotLoseSpeakers() throws Exception {
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                for (int k = 0; k < 200; k++) state.markSpeaking(UUID.randomUUID(), k % 2 == 0);
                done.countDown();
            });
        }
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(n * 200, state.speakers(1_000, Integer.MAX_VALUE).size());
    }
}
