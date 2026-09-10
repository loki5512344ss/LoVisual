/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.config.values.mode.ModeValue;

public final class ModeValueTest {

    @Test
    void invalidDefaultFallsBackToFirstOption() {
        assertEquals("a", new ModeValue("m", "bogus", "a", "b", "c").get());
    }

    @Test
    void nextWrapsAroundOptions() {
        ModeValue value = new ModeValue("m", "a", "a", "b", "c");

        value.next();
        assertEquals("b", value.get());
        value.next();
        assertEquals("c", value.get());
        value.next();
        assertEquals("a", value.get());
    }

    @Test
    void addOptionTrimsAndRejectsDuplicates() {
        ModeValue value = new ModeValue("m", "a", "a", "b");

        assertTrue(value.addOption("  c  "));
        assertEquals(List.of("a", "b", "c"), value.getOptions());

        assertFalse(value.addOption("c"));
        assertFalse(value.addOption(null));
        assertFalse(value.addOption("   "));
        assertEquals(List.of("a", "b", "c"), value.getOptions());
    }

    @Test
    void removeOptionCannotRemoveCurrentValue() {
        ModeValue value = new ModeValue("m", "a", "a", "b");

        assertFalse(value.removeOption("a"));
        assertEquals(List.of("a", "b"), value.getOptions());

        assertTrue(value.removeOption(" b "));
        assertEquals(List.of("a"), value.getOptions());
    }

    @Test
    void addOptionsCountsAddedEntries() {
        ModeValue value = new ModeValue("m", "a", "a");

        assertEquals(2, value.addOptions(List.of("b", " c ", "b")));
        assertEquals(0, value.addOptions(null));
        assertEquals(0, value.addOptions(List.of()));
        assertEquals(List.of("a", "b", "c"), value.getOptions());
    }

    @Test
    void jsonRoundTripIgnoresUnknownOptions() {
        ModeValue value = new ModeValue("m", "a", "a", "b");

        value.fromJson("b");
        assertEquals("b", value.toJson());

        value.fromJson("nope");
        value.fromJson(42);
        value.fromJson(null);
        assertEquals("b", value.get());
    }

    @Test
    void optionsListIsImmutable() {
        ModeValue value = new ModeValue("m", "a", "a", "b");

        assertThrows(UnsupportedOperationException.class, () -> value.getOptions().add("c"));
    }
}
