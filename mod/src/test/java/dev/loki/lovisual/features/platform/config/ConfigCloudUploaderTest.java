package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonObject;
import dev.loki.lovisual.config.profile.codec.ConfigProfileEntry;
import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ConfigCloudUploader#buildBody} wraps a snapshot in the
 * {@code {name, data}} shape configs-service expects on
 * {@code PUT /configs/{slot}}, with {@code data} being the codec's
 * {@code {meta, entries}} tree. This is the pure half of the upload path, so
 * it is tested without a running client or backend.
 */
public final class ConfigCloudUploaderTest {

    @Test
    void buildsNameAndDataEnvelope() {
        ConfigProfileMeta meta = new ConfigProfileMeta("slot1", "My Build", ConfigProfileType.MODULES, "Tester", 10L, 20L, 1);
        ConfigProfileSnapshot snapshot = new ConfigProfileSnapshot(meta,
                List.of(new ConfigProfileEntry("module:ChinaHat", "China Hat", Map.of("enabled", true))));

        JsonObject body = ConfigCloudUploader.buildBody("My Build", snapshot);

        assertEquals("My Build", body.get("name").getAsString());
        JsonObject data = body.getAsJsonObject("data");
        assertEquals("slot1", data.getAsJsonObject("meta").get("id").getAsString());
        assertEquals("My Build", data.getAsJsonObject("meta").get("name").getAsString());
        assertTrue(data.getAsJsonArray("entries").get(0).isJsonObject());
        assertEquals("module:ChinaHat",
                data.getAsJsonArray("entries").get(0).getAsJsonObject().get("ownerId").getAsString());
    }
}
