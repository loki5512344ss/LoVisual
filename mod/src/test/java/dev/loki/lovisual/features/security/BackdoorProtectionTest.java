package dev.loki.lovisual.features.security;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты чистой логики BackdoorProtection без инициализации Minecraft.
 * State очищается в @BeforeEach, т.к. INSTANCE — общий синглтон.
 */
public final class BackdoorProtectionTest {

    private final BackdoorProtection service = BackdoorProtection.INSTANCE;

    @BeforeEach
    void resetState() {
        service.resetLoadedTranslations();
        service.resetVanillaKeybinds();
    }

    @Test
    void unknownKeyAllowedWhenNothingRegistered() {
        assertTrue(service.isTranslationAllowed("anything"));
    }

    @Test
    void resetLoadedTranslationsClearsRegisteredKeys() {
        service.registerTranslationKey("blocked.key", "server_pack");
        service.resetLoadedTranslations();
        assertTrue(service.isTranslationAllowed("blocked.key"));
    }

    @Test
    void remotePackTranslationBlocked() {
        service.registerTranslationKey("evil.key", "server_pack");
        assertFalse(service.isTranslationAllowed("evil.key"));
    }

    @Test
    void builtInPacksAreAllowedCaseInsensitive() {
        service.registerTranslationKey("menu.title", "vanilla");
        assertTrue(service.isTranslationAllowed("menu.title"));

        service.registerTranslationKey("accessibility.title", "high_contrast");
        assertTrue(service.isTranslationAllowed("accessibility.title"));

        service.registerTranslationKey("menu.case", "VANILLA");
        assertTrue(service.isTranslationAllowed("menu.case"));
    }

    @Test
    void registerTranslationKeyIgnoresBlankInput() {
        service.registerTranslationKey("", "server_pack");
        service.registerTranslationKey("good.key", "");
        assertTrue(service.isTranslationAllowed("good.key"));
    }

    @Test
    void keybindAllowedOnlyAfterRegistration() {
        assertFalse(service.isKeybindAllowed("key.attack"));
        service.registerVanillaKeybind("key.attack");
        assertTrue(service.isKeybindAllowed("key.attack"));
    }

    @Test
    void resetVanillaKeybindsClears() {
        service.registerVanillaKeybind("key.jump");
        service.resetVanillaKeybinds();
        assertFalse(service.isKeybindAllowed("key.jump"));
    }

    @Test
    void blankOrNullKeysAreAllowed() {
        assertTrue(service.isTranslationAllowed(null));
        assertTrue(service.isTranslationAllowed(""));
        assertTrue(service.isKeybindAllowed(null));
        assertTrue(service.isKeybindAllowed(""));
    }

    @Test
    void filterTextReplacesBlockedTranslatableWithResolvedFallback() {
        service.registerTranslationKey("evil.key", "server_pack");

        Component filtered = service.filterText(Component.translatable("evil.key"));

        FilteredTranslatableTextContent content =
                assertInstanceOf(FilteredTranslatableTextContent.class, filtered.getContents());
        assertEquals("evil.key", content.getResolved());
    }

    @Test
    void filterTextKeepsAllowedTranslatableWithNullResolved() {
        service.registerTranslationKey("menu.ok", "vanilla");

        Component filtered = service.filterText(Component.translatable("menu.ok"));

        FilteredTranslatableTextContent content =
                assertInstanceOf(FilteredTranslatableTextContent.class, filtered.getContents());
        assertNull(content.getResolved());
    }

    @Test
    void filterTextReplacesBlockedKeybindWithPlainText() {
        Component keybind = MutableComponent.create(new KeybindContents("key.attack"));

        Component filtered = service.filterText(keybind);

        assertInstanceOf(PlainTextContents.class, filtered.getContents());
        assertFalse(filtered.getContents() instanceof KeybindContents);
    }
}
