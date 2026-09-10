/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.profile;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.config.profile.codec.ConfigProfileEntry;
import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.diff.ConfigProfileDiff;
import dev.loki.lovisual.config.profile.diff.ConfigProfileDiffEngine;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;

public final class ConfigProfileDiffEngineTest {

    private final ConfigProfileDiffEngine engine = new ConfigProfileDiffEngine();

    @Test
    void identicalSnapshotsProduceEmptyDiff() {
        ConfigProfileSnapshot current = snapshot(entry("mod.a", "Mod A", "x", 1));
        ConfigProfileSnapshot selected = snapshot(entry("mod.a", "Mod A", "x", 1));

        assertTrue(engine.compare(current, selected).isEmpty());
    }

    @Test
    void changedValueIsReported() {
        ConfigProfileDiff diff = engine.compare(
                snapshot(entry("mod.a", "Mod A", "x", 1)),
                snapshot(entry("mod.a", "Mod A", "x", 2)));

        List<ConfigProfileDiff.Value> values = diff.entries().get(0).values();
        assertEquals(1, values.size());
        assertEquals("x", values.get(0).key());
        assertEquals(1, values.get(0).currentValue());
        assertEquals(2, values.get(0).selectedValue());
        assertEquals(ConfigProfileDiff.Kind.CHANGED, values.get(0).kind());
    }

    @Test
    void addedAndRemovedValuesAreReported() {
        ConfigProfileDiff diff = engine.compare(
                snapshot(entry("mod.a", "Mod A", "keep", 1, "gone", 2)),
                snapshot(entry("mod.a", "Mod A", "keep", 1, "added", 3)));

        Map<String, ConfigProfileDiff.Kind> kinds = diff.entries().get(0).values().stream()
                .collect(Collectors.toMap(ConfigProfileDiff.Value::key, ConfigProfileDiff.Value::kind));

        assertEquals(Set.of("gone", "added"), kinds.keySet());
        assertEquals(ConfigProfileDiff.Kind.REMOVED, kinds.get("gone"));
        assertEquals(ConfigProfileDiff.Kind.ADDED, kinds.get("added"));
    }

    @Test
    void ownerOnlyInSelectedBecomesAddedEntry() {
        ConfigProfileDiff diff = engine.compare(
                snapshot(),
                snapshot(entry("mod.b", "Mod B", "x", 1)));

        ConfigProfileDiff.Entry entry = diff.entries().get(0);
        assertEquals("mod.b", entry.ownerId());
        assertEquals("Mod B", entry.displayName());
        assertEquals(List.of("x"), entry.values().stream()
                .map(ConfigProfileDiff.Value::key).toList());
        assertEquals(ConfigProfileDiff.Kind.ADDED, entry.values().get(0).kind());
        assertNull(entry.values().get(0).currentValue());
    }

    @Test
    void ownerOnlyInCurrentKeepsCurrentDisplayName() {
        ConfigProfileDiff diff = engine.compare(
                snapshot(entry("mod.a", "Current Name", "x", 1)),
                snapshot());

        ConfigProfileDiff.Entry entry = diff.entries().get(0);
        assertEquals("mod.a", entry.ownerId());
        assertEquals("Current Name", entry.displayName());
        assertEquals(ConfigProfileDiff.Kind.REMOVED, entry.values().get(0).kind());
        assertNull(entry.values().get(0).selectedValue());
    }

    @Test
    void nullSnapshotsYieldEmptyModulesDiff() {
        ConfigProfileSnapshot snapshot = snapshot(entry("mod.a", "Mod A", "x", 1));

        assertTrue(engine.compare(null, snapshot).isEmpty());
        assertTrue(engine.compare(snapshot, null).isEmpty());
        assertEquals(ConfigProfileType.MODULES, engine.compare(null, snapshot).type());
    }

    @Test
    void valueChangeCountSumsAllChangedValues() {
        ConfigProfileDiff diff = engine.compare(
                snapshot(entry("mod.a", "A", "x", 1, "y", 2), entry("mod.b", "B", "z", 3)),
                snapshot(entry("mod.a", "A", "x", 9), entry("mod.b", "B", "z", 3)));

        // mod.a: x changed, y removed; mod.b identical -> no entry
        assertEquals(2, diff.valueChangeCount());
    }

    private static ConfigProfileSnapshot snapshot(ConfigProfileEntry... entries) {
        ConfigProfileMeta meta = new ConfigProfileMeta(
                "id", "Profile", ConfigProfileType.MODULES, "author", 0L, 0L, 0);
        return new ConfigProfileSnapshot(meta, List.of(entries));
    }

    private static ConfigProfileEntry entry(String owner, String displayName, Object... keyValuePairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            values.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return new ConfigProfileEntry(owner, displayName, values);
    }
}
