package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Поиск исходников скриптов: {@code config/lovisual/holdmyitems/<name>.lua}, иначе встроенный пример
 * из ресурсов мода (только для основных скриптов; аддоны бывают лишь в конфиге).
 */
public final class ScriptFiles {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptFiles.class);

    private ScriptFiles() {
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("lovisual").resolve("holdmyitems");
    }

    /** Метка изменения: mtime файла, 0 для встроенного примера, -1 если источника нет. */
    public static long stamp(String name, boolean builtinAllowed) {
        Path file = dir().resolve(name + ".lua");
        try {
            if (Files.isRegularFile(file)) return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return -1L;
        }
        return builtinAllowed && ScriptFiles.class.getResource(resource(name)) != null ? 0L : -1L;
    }

    public static String read(String name) {
        Path file = dir().resolve(name + ".lua");
        try {
            if (Files.isRegularFile(file)) return Files.readString(file, StandardCharsets.UTF_8);
            try (InputStream in = ScriptFiles.class.getResourceAsStream(resource(name))) {
                return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOG.warn("Cannot read HoldMyItems script {}: {}", name, e.getMessage());
            return null;
        }
    }

    private static String resource(String name) {
        return "/assets/lovisual/holdmyitems/" + name + ".lua";
    }
}
