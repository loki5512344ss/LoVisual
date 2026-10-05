package dev.loki.lovisual.util.bedrock;

import com.google.gson.JsonParser;
import dev.loki.lovisual.util.bedrock.anim.Channel;
import dev.loki.lovisual.util.bedrock.anim.Clip;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ChannelTest {
    private static float[] sample(Channel channel, float time) {
        float[] out = new float[5];
        channel.sample(time, out, 1);
        return new float[]{out[1], out[2], out[3]};
    }

    @Test
    void linearKeysBlendBetweenPostAndPre() {
        Channel c = Channel.parse(JsonParser.parseString(
                "{\"0\":[0,0,0],\"1\":{\"pre\":[10,0,0],\"post\":[20,0,0]},\"2\":[40,0,0]}"));
        assertEquals(5.0f, sample(c, 0.5f)[0], 1e-5f);
        assertEquals(10.0f, sample(c, 0.999999f)[0], 1e-3f);
        assertEquals(20.0f, sample(c, 1.0f)[0], 1e-5f);
        assertEquals(30.0f, sample(c, 1.5f)[0], 1e-5f);
        assertEquals(40.0f, sample(c, 9.0f)[0], 1e-5f);
    }

    @Test
    void catmullRomPassesThroughKeysAndBulgesBetweenThem() {
        Channel c = Channel.parse(JsonParser.parseString(
                "{\"0\":[0,0,0],\"1\":{\"post\":[10,0,0],\"lerp_mode\":\"catmullrom\"},\"2\":[10,0,0],\"3\":[0,0,0]}"));
        assertEquals(10.0f, sample(c, 1.0f)[0], 1e-4f);
        assertTrue(sample(c, 1.5f)[0] > 10.0f);
    }

    @Test
    void constantVectorsAndMissingChannels() {
        assertEquals(3.0f, sample(Channel.parse(JsonParser.parseString("[3,4,5]")), 7.0f)[0]);
        assertNull(Channel.parse(null));
        assertNull(Channel.parse(JsonParser.parseString("{}")));
    }

    @Test
    void clipsKeepNamesLengthsAndSortedSounds() {
        var clips = Clip.parseAll("""
                {"animations":{"animation.knife.draw":{"loop":false,"animation_length":0.96,
                  "bones":{"a":{"rotation":{"0":[0,0,0],"0.5":[1,2,3]}}},
                  "sound_effects":{"0.5":{"effect":"b"},"0.0":{"effect":"a"}}}}}""");
        Clip draw = clips.get("draw");
        assertEquals(0.96f, draw.length(), 1e-6f);
        assertTrue(!draw.loops());
        assertEquals(2, draw.soundCount());
        assertEquals("a", draw.soundName(0));
        assertEquals("b", draw.soundName(1));
    }
}
