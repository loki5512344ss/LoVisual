package dev.loki.lovisual.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every module and HUD element shown in the GUI must have an en and ru description. */
class DescriptionCoverageTest {
    private static final Path SRC = Path.of("src/main/java");
    private static final Path LANG = Path.of("src/main/resources/assets/lovisual/lang");
    private static final Pattern MODULE = Pattern.compile("@ModuleInfo\\((.*?)\\)\\s*(?:public |final |abstract )*class", Pattern.DOTALL);
    private static final Pattern HUD_INFO = Pattern.compile("@HudElementInfo\\((.*?)\\)\\s*(?:public |final |abstract )*class", Pattern.DOTALL);
    private static final Pattern ID = Pattern.compile("\\bid\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern HUD_SUPER = Pattern.compile("super\\(\"([a-z_0-9]+)\",\\s*\"[^\"]+\",\\s*(?:true|false)");
    private static final List<String> READ_ONLY_HUD_CARDS = List.of("ui_relations_color", "ui_items_color");

    @Test
    void everyModuleAndHudElementHasDescriptions() throws IOException {
        TreeSet<String> keys = new TreeSet<>();
        List<String> sources = readSources();
        for (String source : sources) {
            collect(MODULE, source, "module.%s.description", keys);
            collect(HUD_INFO, source, "hud.%s.description", keys);
            Matcher m = HUD_SUPER.matcher(source);
            while (m.find()) keys.add("hud." + m.group(1) + ".description");
        }
        READ_ONLY_HUD_CARDS.forEach(id -> keys.add("hud." + id + ".description"));
        assertTrue(keys.size() > 100, "source scan found suspiciously few ids: " + keys.size());

        for (String lang : List.of("en_us.json", "ru_ru.json")) {
            String json = Files.readString(LANG.resolve(lang));
            List<String> missing = new ArrayList<>();
            for (String key : keys) {
                if (!json.contains("\"" + key + "\"")) missing.add(key);
            }
            assertTrue(missing.isEmpty(), lang + " lacks descriptions: " + missing);
        }
    }

    private static void collect(Pattern annotation, String source, String keyFormat, TreeSet<String> out) {
        Matcher m = annotation.matcher(source);
        while (m.find()) {
            Matcher id = ID.matcher(m.group(1));
            if (id.find()) out.add(keyFormat.formatted(id.group(1)));
        }
    }

    private static List<String> readSources() throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                out.add(Files.readString(p));
            }
        }
        return out;
    }
}
