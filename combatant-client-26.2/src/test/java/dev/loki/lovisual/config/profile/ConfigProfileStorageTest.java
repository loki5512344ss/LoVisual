/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты ConfigProfileStorage: канонический формат .lvcfg и обратная
 * совместимость с легаси-профилями .cbcfg (чтение, листинг, импорт, удаление).
 * Чистая файловая логика без инициализации Minecraft.
 */
public final class ConfigProfileStorageTest {

    @TempDir
    Path tempDir;

    private ConfigProfileStorage storage() {
        return new ConfigProfileStorage(tempDir);
    }

    private static ConfigProfileSnapshot snapshot(String name) {
        ConfigProfileMeta meta = new ConfigProfileMeta(null, name, ConfigProfileType.MODULES, "loki5512344", 1L, 2L, 0);
        return new ConfigProfileSnapshot(meta, List.of(new ConfigProfileEntry("module.a", "Module A", Map.of("enabled", true))));
    }

    @Test
    void saveWritesCanonicalLvcfgFile() throws IOException {
        ConfigProfileStorage storage = storage();
        ConfigProfileSnapshot saved = snapshot("alpha");
        storage.save(saved);

        Path canonical = tempDir.resolve("modules").resolve("alpha.lvcfg");
        assertTrue(Files.exists(canonical));

        ConfigProfileSnapshot read = storage.read(ConfigProfileType.MODULES, "alpha");
        assertEquals("alpha", read.meta().name());
    }

    @Test
    void readFallsBackToLegacyCbcfg() throws IOException {
        ConfigProfileStorage storage = storage();
        storage.save(snapshot("legacy"));
        Path canonical = tempDir.resolve("modules").resolve("legacy.lvcfg");
        Files.move(canonical, tempDir.resolve("modules").resolve("legacy.cbcfg"));

        assertTrue(storage.exists(ConfigProfileType.MODULES, "legacy"));
        ConfigProfileSnapshot read = storage.read(ConfigProfileType.MODULES, "legacy");
        assertEquals("legacy", read.meta().name());
    }

    @Test
    void listIncludesLegacyProfiles() throws IOException {
        ConfigProfileStorage storage = storage();
        storage.save(snapshot("fresh"));
        storage.save(snapshot("old"));
        Files.move(
                tempDir.resolve("modules").resolve("old.lvcfg"),
                tempDir.resolve("modules").resolve("old.cbcfg"),
                StandardCopyOption.REPLACE_EXISTING
        );

        List<String> names = storage.list(ConfigProfileType.MODULES).stream().map(ConfigProfileMeta::name).toList();
        assertTrue(names.contains("fresh"));
        assertTrue(names.contains("old"));
    }

    @Test
    void listMigratesLegacyFileToCanonical() throws IOException {
        ConfigProfileStorage storage = storage();
        storage.save(snapshot("migrate"));
        Path legacy = tempDir.resolve("modules").resolve("migrate.cbcfg");
        Files.move(tempDir.resolve("modules").resolve("migrate.lvcfg"), legacy);

        storage.list(ConfigProfileType.MODULES);

        assertFalse(Files.exists(legacy));
        assertTrue(Files.exists(tempDir.resolve("modules").resolve("migrate.lvcfg")));
    }

    @Test
    void deleteRemovesBothVariants() throws IOException {
        ConfigProfileStorage storage = storage();
        storage.save(snapshot("doomed"));
        Files.copy(
                tempDir.resolve("modules").resolve("doomed.lvcfg"),
                tempDir.resolve("modules").resolve("doomed.cbcfg")
        );

        assertTrue(storage.delete(ConfigProfileType.MODULES, "doomed"));
        assertFalse(Files.exists(tempDir.resolve("modules").resolve("doomed.lvcfg")));
        assertFalse(Files.exists(tempDir.resolve("modules").resolve("doomed.cbcfg")));
    }

    @Test
    void importLegacyFileConvertsToCanonical() throws IOException {
        ConfigProfileStorage storage = storage();
        storage.save(snapshot("importme"));
        byte[] bytes = Files.readAllBytes(tempDir.resolve("modules").resolve("importme.lvcfg"));

        Path source = tempDir.resolve("importme.cbcfg");
        Files.write(source, bytes);

        ConfigProfileStorage.ImportResult result = storage.importProfileFile(source, true);
        assertEquals("importme", result.meta().getId());
        assertTrue(Files.exists(tempDir.resolve("modules").resolve("importme.lvcfg")));
        assertFalse(Files.exists(source));
    }

    @Test
    void importRejectsForeignExtension() throws IOException {
        Path source = tempDir.resolve("notes.txt");
        Files.writeString(source, "not a profile");
        assertThrows(IOException.class, () -> storage().importProfileFile(source, false));
    }

    @Test
    void sanitizeFileNameStripsBothExtensions() {
        assertEquals("alpha", ConfigProfileStorage.sanitizeFileName("alpha.lvcfg"));
        assertEquals("alpha", ConfigProfileStorage.sanitizeFileName("alpha.cbcfg"));
        assertEquals("alpha", ConfigProfileStorage.sanitizeFileName("Alpha"));
        assertEquals("config", ConfigProfileStorage.sanitizeFileName(".lvcfg"));
    }

    @Test
    void fileNormalizesLegacyIdToCanonical() {
        ConfigProfileStorage storage = storage();
        Path resolved = storage.file(ConfigProfileType.MODULES, "something.cbcfg");
        assertEquals(tempDir.resolve("modules").resolve("something.lvcfg"), resolved);
    }
}
