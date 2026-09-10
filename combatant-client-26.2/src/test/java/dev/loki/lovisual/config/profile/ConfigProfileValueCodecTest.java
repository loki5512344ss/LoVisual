/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.profile;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import dev.loki.lovisual.config.profile.codec.ConfigProfileValueCodec;

public final class ConfigProfileValueCodecTest {

    @Test
    void roundTripsNull() throws IOException {
        assertNull(roundTrip(null));
    }

    @Test
    void roundTripsBooleans() throws IOException {
        assertEquals(Boolean.TRUE, roundTrip(Boolean.TRUE));
        assertEquals(Boolean.FALSE, roundTrip(Boolean.FALSE));
    }

    @Test
    void roundTripsIntegersIncludingVarIntBoundaries() throws IOException {
        assertEquals(0, roundTrip(0));
        assertEquals(127, roundTrip(127));
        assertEquals(128, roundTrip(128));
        assertEquals(Integer.MAX_VALUE, roundTrip(Integer.MAX_VALUE));
        assertEquals(-1, roundTrip(-1));
    }

    @Test
    void roundTripsLongsFloatsAndDoubles() throws IOException {
        assertEquals(Long.MAX_VALUE, roundTrip(Long.MAX_VALUE));
        assertEquals(1.5f, roundTrip(1.5f));
        assertEquals(2.25d, roundTrip(2.25d));
    }

    @Test
    void narrowNumericTypesAreStoredAsDoubles() throws IOException {
        assertEquals(7.0d, roundTrip((short) 7));
        assertEquals(8.0d, roundTrip((byte) 8));
    }

    @Test
    void roundTripsUtf8Strings() throws IOException {
        assertEquals("привет ✨ мир", roundTrip("привет ✨ мир"));
        assertEquals("", roundTrip(""));
    }

    @Test
    void roundTripsListsAndMaps() throws IOException {
        List<Object> list = Arrays.asList("a", 1, Boolean.TRUE, null);
        assertEquals(list, roundTrip(list));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
        map.put("empty", null);
        map.put("nested", List.of("x"));
        assertEquals(map, roundTrip(map));
    }

    @Test
    void unknownTypesFallBackToString() throws IOException {
        Object value = new Object() {
            @Override
            public String toString() {
                return "opaque";
            }
        };
        assertEquals("opaque", roundTrip(value));
    }

    @Test
    void unknownTypeByteFailsWithIOException() {
        byte[] corrupt = new byte[]{99};
        assertThrows(IOException.class, () -> read(corrupt));
    }

    @Test
    void oversizedVarIntFailsWithIOException() {
        byte[] corrupt = new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80};
        assertThrows(IOException.class, () -> read(corrupt));
    }

    @Test
    void truncatedStringFailsWithIOException() {
        // заголовок длины 5, но самих байтов нет
        byte[] corrupt = new byte[]{5};
        assertThrows(IOException.class, () -> read(corrupt));
    }

    private static Object roundTrip(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            ConfigProfileValueCodec.writeValue(out, value);
        }
        return read(bytes.toByteArray());
    }

    private static Object read(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            return ConfigProfileValueCodec.readValue(in);
        }
    }
}
