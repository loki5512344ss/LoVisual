package dev.loki.lovisual.config.profile.service;

import dev.loki.lovisual.config.io.ConfigPaths;
import dev.loki.lovisual.util.logging.DebugLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import dev.loki.lovisual.config.profile.codec.ConfigProfileBinaryCodec;
import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;

public final class ConfigProfileStorage {
    public static final String EXTENSION = ".lvcfg";
    public static final String LEGACY_EXTENSION = ".cbcfg";

    private final Path root;
    private final ConfigProfileBinaryCodec codec = new ConfigProfileBinaryCodec();

    public ConfigProfileStorage() {
        this(ConfigPaths.profilesRoot());
    }

    public ConfigProfileStorage(Path root) {

        this.root = root;
    }

    public static String sanitizeFileName(String name) {
        if (name == null) return "config";
        String base = name.trim().toLowerCase(Locale.ROOT);
        base = stripExtension(base, EXTENSION);
        base = stripExtension(base, LEGACY_EXTENSION);
        if (base.isEmpty()) return "config";
        StringBuilder out = new StringBuilder(base.length());
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.length() == 0 ? "config" : out.toString();
    }

    private static String stripExtension(String value, String extension) {
        if (value.endsWith(extension)) return value.substring(0, value.length() - extension.length());
        return value;
    }

    public Path root() {
        return root;
    }

    public Path directory(ConfigProfileType type) {
        return root.resolve(type.folderName());
    }

    public List<ConfigProfileMeta> list(ConfigProfileType type) {
        normalizeLooseProfiles();
        List<ConfigProfileMeta> out = new ArrayList<>();
        Path dir = directory(type);
        if (!Files.isDirectory(dir)) return out;
        try (var stream = Files.list(dir)) {
            stream.filter(path -> Files.isRegularFile(path) && hasProfileExtension(path))
                    .forEach(path -> {
                        try {
                            ConfigProfileMeta meta = codec.readMeta(Files.readAllBytes(path));
                            out.add(meta);
                        } catch (Exception e) {
                            DebugLog.error("Failed to read profile meta %s", e, path.toAbsolutePath());
                        }
                    });
        } catch (IOException e) {
            DebugLog.error("Failed to list profiles %s", e, dir.toAbsolutePath());
        }
        out.sort(Comparator.comparingLong(ConfigProfileMeta::updatedAt).reversed());
        return out;
    }

    private static boolean hasProfileExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(EXTENSION) || name.endsWith(LEGACY_EXTENSION);
    }

    public ImportResult importProfileFile(Path source, boolean moveSource) throws IOException {
        if (source == null) throw new IllegalArgumentException("profile path is null");
        if (!Files.isRegularFile(source)) throw new IOException("profile is not a regular file: " + source);
        if (!hasProfileExtension(source)) {
            throw new IOException("not a " + EXTENSION + " or " + LEGACY_EXTENSION + " profile: " + source);
        }

        byte[] bytes = Files.readAllBytes(source);
        ConfigProfileMeta meta = codec.read(bytes).meta();
        Path dest = file(meta.type(), meta.getId());
        Files.createDirectories(dest.getParent());

        Path sourceAbs = source.toAbsolutePath().normalize();
        Path destAbs = dest.toAbsolutePath().normalize();
        if (!sourceAbs.equals(destAbs)) {
            if (moveSource) {
                Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.write(dest, bytes);
            }
        }
        return new ImportResult(meta, dest);
    }

    public ConfigProfileSnapshot read(ConfigProfileType type, String idOrName) throws IOException {
        Path path = resolveExisting(type, idOrName);
        return codec.read(Files.readAllBytes(path));
    }

    public ConfigProfileSnapshot read(ConfigProfileMeta meta) throws IOException {
        if (meta == null) throw new IllegalArgumentException("profile meta is null");
        return read(meta.type(), meta.getId());
    }

    public void save(ConfigProfileSnapshot snapshot) throws IOException {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is null");
        Files.createDirectories(directory(snapshot.meta().type()));
        Path path = file(snapshot.meta().type(), snapshot.meta().getId());
        Files.write(path, codec.write(snapshot));
        DebugLog.config("Config profile saved: %s", path.toAbsolutePath());
    }

    public boolean delete(ConfigProfileType type, String idOrName) throws IOException {
        boolean deleted = Files.deleteIfExists(file(type, idOrName));
        deleted |= Files.deleteIfExists(legacyFile(type, idOrName));
        return deleted;
    }

    public ConfigProfileSnapshot rename(ConfigProfileType type, String idOrName, String newName) throws IOException {
        if (newName == null || newName.isBlank()) throw new IllegalArgumentException("profile name is blank");
        ConfigProfileSnapshot old = read(type, idOrName);
        ConfigProfileMeta oldMeta = old.meta();
        ConfigProfileMeta newMeta = oldMeta.withName(newName.trim(), System.currentTimeMillis());
        Path oldPath = resolveExisting(type, oldMeta.getId());
        Path newPath = file(type, newMeta.getId());
        if (!oldMeta.getId().equals(newMeta.getId()) && Files.exists(newPath)) {
            throw new IOException("profile already exists: " + newMeta.name());
        }
        ConfigProfileSnapshot renamed = new ConfigProfileSnapshot(newMeta, old.entries());
        save(renamed);
        if (!oldPath.equals(newPath)) Files.deleteIfExists(oldPath);
        return renamed;
    }

    public boolean exists(ConfigProfileType type, String idOrName) {
        return Files.exists(file(type, idOrName)) || Files.exists(legacyFile(type, idOrName));
    }

    /** Canonical storage path — always uses the current {@link #EXTENSION}. */
    public Path file(ConfigProfileType type, String idOrName) {
        String id = sanitizeFileName(idOrName);
        if (id.endsWith(LEGACY_EXTENSION)) {
            id = stripExtension(id, LEGACY_EXTENSION) + EXTENSION;
        } else if (!id.endsWith(EXTENSION)) {
            id += EXTENSION;
        }
        return directory(type).resolve(id);
    }

    private Path legacyFile(ConfigProfileType type, String idOrName) {
        String id = sanitizeFileName(idOrName);
        if (!id.endsWith(LEGACY_EXTENSION)) id += LEGACY_EXTENSION;
        return directory(type).resolve(id);
    }

    private Path resolveExisting(ConfigProfileType type, String idOrName) {
        Path canonical = file(type, idOrName);
        if (Files.exists(canonical)) return canonical;
        Path legacy = legacyFile(type, idOrName);
        return Files.exists(legacy) ? legacy : canonical;
    }

    private void normalizeLooseProfiles() {
        if (!Files.isDirectory(root)) return;
        List<Path> candidates = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(ConfigProfileStorage::hasProfileExtension)
                    .forEach(candidates::add);
        } catch (IOException e) {
            DebugLog.error("Failed to scan loose config profiles %s", e, root.toAbsolutePath());
            return;
        }

        for (Path candidate : candidates) {
            try {
                ConfigProfileMeta meta = codec.read(Files.readAllBytes(candidate)).meta();
                Path dest = file(meta.type(), meta.getId());
                Path candidateAbs = candidate.toAbsolutePath().normalize();
                Path destAbs = dest.toAbsolutePath().normalize();
                if (candidateAbs.equals(destAbs)) continue;
                Files.createDirectories(dest.getParent());
                Files.move(candidate, dest, StandardCopyOption.REPLACE_EXISTING);
                DebugLog.config("Moved config profile %s -> %s", candidateAbs, destAbs);
            } catch (Exception e) {
                DebugLog.error("Failed to normalize config profile %s", e, candidate.toAbsolutePath());
            }
        }
    }

    public record ImportResult(ConfigProfileMeta meta, Path path) {
    }
}
