package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CloudSlotsTest {

    @Test
    void formatsPublishedSlot() {
        JsonObject slot = JsonParser.parseString(
                "{\"slot\":2,\"name\":\"My Build\",\"share_code\":\"ABCD2345\",\"published\":true}").getAsJsonObject();
        assertEquals("2. My Build [published] code ABCD2345", CloudSlots.slotLine(slot));
    }

    @Test
    void formatsPrivateSlot() {
        JsonObject slot = JsonParser.parseString(
                "{\"slot\":1,\"name\":\"A\",\"share_code\":\"XYZ\",\"published\":false}").getAsJsonObject();
        assertEquals("1. A code XYZ", CloudSlots.slotLine(slot));
    }
}
