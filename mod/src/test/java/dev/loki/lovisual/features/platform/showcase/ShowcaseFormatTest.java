package dev.loki.lovisual.features.platform.showcase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ShowcaseFormatTest {

    private static final String ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void formatsListingLine() {
        JsonObject listing = JsonParser.parseString(
                "{\"title\":\"Clean HUD\",\"copies_count\":12,\"author\":{\"nick\":\"loki\"}}").getAsJsonObject();
        assertEquals("3. Clean HUD by loki (12 copies)", ShowcaseFormat.listingLine(3, listing));
    }

    @Test
    void singularCopyAndMissingAuthor() {
        JsonObject listing = JsonParser.parseString("{\"title\":\"X\",\"copies_count\":1}").getAsJsonObject();
        assertEquals("1. X by ? (1 copy)", ShowcaseFormat.listingLine(1, listing));
    }

    @Test
    void parsesSort() {
        assertEquals("new", ShowcaseFormat.parseSort(null));
        assertEquals("popular", ShowcaseFormat.parseSort("POPULAR"));
        assertNull(ShowcaseFormat.parseSort("oldest"));
    }

    @Test
    void resolvesNumberOrUuidButNothingElse() {
        List<String> page = List.of(ID, "22222222-2222-2222-2222-222222222222");
        assertEquals(ID, ShowcaseFormat.resolveId("1", page));
        assertNull(ShowcaseFormat.resolveId("3", page));
        assertNull(ShowcaseFormat.resolveId("0", page));
        assertEquals(ID, ShowcaseFormat.resolveId(ID, List.of()));
        assertNull(ShowcaseFormat.resolveId("../../etc", page));
        assertNull(ShowcaseFormat.resolveId(null, page));
    }
}
