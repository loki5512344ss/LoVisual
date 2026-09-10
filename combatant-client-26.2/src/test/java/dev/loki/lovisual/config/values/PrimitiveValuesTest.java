/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.values;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.primitive.StringValue;

public final class PrimitiveValuesTest {

    @Test
    void numberClampsDefaultToRange() {
        assertEquals(3, new NumberValue<>("n", 5, 0, 3).get());
        assertEquals(0, new NumberValue<>("n", -5, 0, 3).get());
    }

    @Test
    void numberPreservesDeclaredType() {
        assertInstanceOf(Double.class, new NumberValue<>("n", 1.5, 0.0, 3.0).get());
        assertInstanceOf(Float.class, new NumberValue<>("n", 0.5f, 0.0f, 3.0f).get());
        assertInstanceOf(Long.class, new NumberValue<>("n", 2L, 0L, 3L).get());
        assertInstanceOf(Integer.class, new NumberValue<>("n", 2, 0, 3).get());
    }

    @Test
    void numberToJsonRoundsFractionsToTwoDecimals() {
        assertEquals(1.23, new NumberValue<>("n", 1.23456, 0.0, 5.0).toJson());
        assertEquals(7.0, new NumberValue<>("n", 7, 0, 9).toJson());
    }

    @Test
    void numberFromJsonClampsAndIgnoresForeignTypes() {
        NumberValue<Integer> value = new NumberValue<>("n", 1, 0, 3);

        value.fromJson(99);
        assertEquals(3, value.get());

        value.fromJson("99");
        value.fromJson(null);
        assertEquals(3, value.get());
    }

    @Test
    void numberExposesBounds() {
        NumberValue<Integer> value = new NumberValue<>("n", 1, 0, 3);
        assertEquals(0, value.getMin());
        assertEquals(3, value.getMax());
    }

    @Test
    void booleanRoundTripIgnoresForeignTypes() {
        BooleanValue value = new BooleanValue("b", true);

        assertEquals(true, value.toJson());
        value.fromJson(false);
        assertFalse(value.get());

        value.fromJson("false");
        value.fromJson(null);
        assertFalse(value.get());
    }

    @Test
    void stringRoundTripIgnoresForeignTypes() {
        StringValue value = new StringValue("s", "default");

        assertEquals("default", value.toJson());
        value.fromJson("next");
        assertEquals("next", value.get());

        value.fromJson(42);
        assertEquals("next", value.get());
    }

    @Test
    void setDefendsAgainstExternalMutation() {
        SetValue value = new SetValue("set", Set.of("a", "b"));

        Set<String> input = new LinkedHashSet<>();
        input.add("x");
        value.set(input);
        input.add("y");

        assertEquals(Set.of("x"), value.get());

        value.set(null);
        assertTrue(value.get().isEmpty());
    }

    @Test
    void setFromJsonFiltersNonStrings() {
        SetValue value = new SetValue("set");

        List<Object> json = new ArrayList<>();
        json.add("a");
        json.add(1);
        json.add(null);
        json.add("b");
        value.fromJson(json);

        assertEquals(new LinkedHashSet<>(List.of("a", "b")), value.get());
        assertEquals(List.of("a", "b"), value.toJson());
    }

    @Test
    void setFromJsonNonListKeepsCurrent() {
        SetValue value = new SetValue("set", Set.of("keep"));
        value.fromJson("nope");
        assertEquals(Set.of("keep"), value.get());
    }

    @Test
    void itemIdSetNormalizesNamespace() {
        ItemIdSetValue value = new ItemIdSetValue("ids", Set.of("STONE", "minecraft:Dirt", "  ", ""));

        assertEquals(new LinkedHashSet<>(List.of("minecraft:stone", "minecraft:dirt")), value.get());
    }

    @Test
    void itemIdSetAddsDefaultNamespace() {
        ItemIdSetValue value = new ItemIdSetValue("ids");
        value.set(Set.of("diamond_sword"));

        assertEquals(Set.of("minecraft:diamond_sword"), value.get());
    }

    @Test
    void booleanMapSeedsDefaultsAndDefaultsUnknownToFalse() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put("alpha", true);
        defaults.put("beta", false);
        BooleanMapValue value = new BooleanMapValue("map", defaults);

        assertTrue(value.get("alpha"));
        assertFalse(value.get("beta"));
        assertFalse(value.get("unknown"));
    }

    @Test
    void booleanMapFromJsonAppliesOnlyKnownKeys() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put("alpha", true);
        BooleanMapValue value = new BooleanMapValue("map", defaults);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("alpha", false);
        json.put("unknown", true);
        json.put("num", 5);
        value.fromJson(json);

        assertFalse(value.get("alpha"));
        assertFalse(value.getAll().containsKey("unknown"));
        assertEquals(Map.of("alpha", false), value.toJson());
    }

    @Test
    void booleanMapFromJsonNonMapResetsToDefaults() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put("alpha", true);
        BooleanMapValue value = new BooleanMapValue("map", defaults);

        value.set("alpha", false);
        value.fromJson("garbage");

        assertTrue(value.get("alpha"));
    }
}
