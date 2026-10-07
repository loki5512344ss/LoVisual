package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CosmeticKindTest {

    @Test
    void modulesMapToTheirKind() {
        assertEquals(CosmeticKind.WINGS, CosmeticKind.ofModule("wings"));
        assertEquals(CosmeticKind.HATS, CosmeticKind.ofModule("tazikhat"));
        assertEquals(CosmeticKind.WEAPONS, CosmeticKind.ofModule("knife"));
        assertNull(CosmeticKind.ofModule("totemsound"));
    }

    @Test
    void noModuleIsListedTwice() {
        Set<String> seen = new HashSet<>();
        for (CosmeticKind kind : CosmeticKind.values()) {
            for (String id : kind.moduleIds()) assertTrue(seen.add(id), id);
        }
    }
}
