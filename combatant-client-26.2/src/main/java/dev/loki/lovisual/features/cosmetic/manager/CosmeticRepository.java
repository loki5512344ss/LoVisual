package dev.loki.lovisual.features.cosmetic.manager;

import dev.loki.lovisual.config.io.ConfigPaths;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import dev.loki.lovisual.features.cosmetic.animation.CosmeticType;
import dev.loki.lovisual.features.cosmetic.model.CosmeticModel;
import dev.loki.lovisual.features.cosmetic.model.CosmeticSlot;

/**
 * Scans {@code config/lovisual/cosmetics/&lt;type&gt;/&lt;name&gt;} (and the flat root as a
 * fallback) for user-provided cosmetic folders (avatar.json + .bbmodel) and keeps the
 * loaded catalog in memory.
 */
public final class CosmeticRepository {
    private static final CosmeticRepository INSTANCE = new CosmeticRepository();

    private final Map<String, CosmeticModel> catalog = new LinkedHashMap<>();

    public static CosmeticRepository get() {
        return INSTANCE;
    }

    public Path root() {
        return ConfigPaths.root().resolve("cosmetics");
    }

    public void reload() {
        catalog.clear();
        discoverUserFolder(root());
    }

    public List<CosmeticModel> all() {
        return new ArrayList<>(catalog.values());
    }

    public List<CosmeticModel> byType(CosmeticType type) {
        return catalog.values().stream().filter(m -> m.getType() == type).toList();
    }

    public List<CosmeticModel> bySlot(CosmeticSlot slot) {
        return catalog.values().stream().filter(m -> m.getSlot() == slot).toList();
    }

    public CosmeticModel get(String id) {
        return catalog.get(id);
    }

    private void register(CosmeticModel model) {
        if (model != null && !catalog.containsKey(model.getId())) {
            catalog.put(model.getId(), model);
        }
    }

    private void discoverUserFolder(Path root) {
        if (!Files.isDirectory(root)) {
            try {
                Files.createDirectories(root);
                for (CosmeticType type : CosmeticType.values()) {
                    Files.createDirectories(root.resolve(type.getId()));
                }
            } catch (Exception ignored) {
            }
            return;
        }

        for (CosmeticType type : CosmeticType.values()) {
            Path typeDir = root.resolve(type.getId());
            scanDirectory(typeDir, type);
        }
        scanDirectory(root, null);
    }

    private void scanDirectory(Path dir, CosmeticType forcedType) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isDirectory).forEach(sub -> {
                File folder = sub.toFile();
                if (new File(folder, "avatar.json").exists() || hasBbmodel(folder)) {
                    CosmeticModel model = CosmeticModel.loadExternal(folder, "user");
                    if (model != null) {
                        if (forcedType != null && model.getType() == CosmeticType.MODEL) {
                            model = model.withType(forcedType);
                        }
                        register(model);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static boolean hasBbmodel(File folder) {
        return new File(folder, "model.bbmodel").exists()
                || new File(folder, folder.getName() + ".bbmodel").exists();
    }
}
