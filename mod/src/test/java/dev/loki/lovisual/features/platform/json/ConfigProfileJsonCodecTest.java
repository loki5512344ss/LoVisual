package dev.loki.lovisual.features.platform.json;

import com.google.gson.JsonObject;
import dev.loki.lovisual.config.profile.codec.ConfigProfileEntry;
import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips {@link ConfigProfileSnapshot} through {@link ConfigProfileJsonCodec},
 * covering every value type {@code ConfigValue#fromJson} accepts (booleans,
 * numbers, strings, nested lists/maps) plus meta fields, since this codec is
 * the adapter between the on-disk binary profile format and the backend's
 * JSON {@code data} field.
 */
public final class ConfigProfileJsonCodecTest {

    @Test
    void roundTripsMetaAndSimpleValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", true);
        values.put("scale", 1.5);
        values.put("label", "Hello");
        values.put("count", 42);

        ConfigProfileMeta meta = new ConfigProfileMeta("id1", "My Profile", ConfigProfileType.MODULES, "Tester", 1000L, 2000L, 1);
        ConfigProfileSnapshot snapshot = new ConfigProfileSnapshot(meta,
                List.of(new ConfigProfileEntry("module:Test", "Test", values)));

        JsonObject json = ConfigProfileJsonCodec.toJson(snapshot);
        ConfigProfileSnapshot restored = ConfigProfileJsonCodec.fromJson(json);

        assertEquals("id1", restored.meta().id());
        assertEquals("My Profile", restored.meta().name());
        assertEquals(ConfigProfileType.MODULES, restored.meta().type());
        assertEquals("Tester", restored.meta().author());
        assertEquals(1000L, restored.meta().createdAt());
        assertEquals(2000L, restored.meta().updatedAt());
        assertEquals(1, restored.entries().size());

        ConfigProfileEntry entry = restored.entries().get(0);
        assertEquals("module:Test", entry.ownerId());
        assertEquals(Boolean.TRUE, entry.values().get("enabled"));
        assertEquals(1.5, (Double) entry.values().get("scale"));
        assertEquals("Hello", entry.values().get("label"));
        assertEquals(42.0, (Double) entry.values().get("count"));
    }

    @Test
    void roundTripsNestedListsAndMaps() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("x", 1);
        nested.put("y", 2);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tags", List.of("a", "b", "c"));
        values.put("point", nested);

        ConfigProfileMeta meta = new ConfigProfileMeta("id2", "Nested", ConfigProfileType.HUD, "Tester", 1L, 2L, 1);
        ConfigProfileSnapshot snapshot = new ConfigProfileSnapshot(meta,
                List.of(new ConfigProfileEntry("hud.draggable:widget", "Widget", values)));

        ConfigProfileSnapshot restored = ConfigProfileJsonCodec.fromJson(ConfigProfileJsonCodec.toJson(snapshot));
        ConfigProfileEntry entry = restored.entries().get(0);

        Object tags = entry.values().get("tags");
        assertTrue(tags instanceof List<?>);
        assertEquals(List.of("a", "b", "c"), tags);

        Object point = entry.values().get("point");
        assertTrue(point instanceof Map<?, ?>);
        assertEquals(1.0, (Double) ((Map<?, ?>) point).get("x"));
    }

    @Test
    void missingMetaAndEntriesDecodeToEmptyDefaults() {
        ConfigProfileSnapshot restored = ConfigProfileJsonCodec.fromJson(new JsonObject());
        assertTrue(restored.entries().isEmpty());
        assertEquals(ConfigProfileType.MODULES, restored.meta().type());
    }
}
