package dev.loki.lovisual.render.iris.patch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loki.lovisual.util.logging.DebugLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Repository that loads and indexes shader-patch manifests from JSON resources.
 * Extracted from {@link ShaderPatchEngine} to keep the facade under the file-size cap.
 * Sibling folder sharing the parent's package.
 */
final class ShaderPatchRepo {

    static final String INDEX_RESOURCE = "assets/lovisual/shaders/iris-patches/index.json";

    private ShaderPatchRepo() { }

    record Target(String manifestId, int priority, String path, String marker,
                  Set<ShaderPatchCompiler.ShaderStage> stages, ShaderPatchCompiler.PatchProgram program) { }

    record Manifest(String id, int priority, Identity identity, List<Target> targets) { }

    record Identity(String family, String profileId, String policy, Pattern packNamePattern) {
        boolean matches(String shaderPackName) {
            if (shaderPackName == null || shaderPackName.isBlank()) return false;
            if (packNamePattern != null) return packNamePattern.matcher(shaderPackName).matches();
            String n = shaderPackName.toLowerCase(Locale.ROOT);
            return n.contains(family.toLowerCase(Locale.ROOT)) || n.contains(profileId.toLowerCase(Locale.ROOT));
        }
    }

    record Selection(Map<String, List<Target>> targetsByPath, Set<String> manifestIds, boolean nameGated) { }

    record Repository(Map<String, List<Target>> targetsByPath, Map<String, Manifest> manifestsById) {

        static Repository load(List<String> extraManifestResources) {
            Map<String, List<Target>> targets = new HashMap<>();
            Map<String, Manifest> manifests = new HashMap<>();
            try {
                JsonObject index = readObject(INDEX_RESOURCE);
                requireSchema(index, INDEX_RESOURCE);
                for (JsonElement el : requireArray(index, "manifests")) addManifest(targets, manifests, el.getAsString());
                if (extraManifestResources != null) for (String r : extraManifestResources) addManifest(targets, manifests, r);
            } catch (Exception e) { DebugLog.error("[IrisPatch] Failed to load shader patch repository", e); }
            targets.replaceAll((path, list) -> list.stream().sorted(Comparator.comparingInt(Target::priority).reversed()).toList());
            return new Repository(Map.copyOf(targets), Map.copyOf(manifests));
        }

        Selection select(String shaderPackName) {
            if (shaderPackName == null || shaderPackName.isBlank()) return new Selection(Map.of(), Set.of(), true);
            HashSet<String> selected = new HashSet<>();
            for (Manifest m : manifestsById.values()) if (m.identity.matches(shaderPackName)) selected.add(m.id);
            if (selected.isEmpty()) return new Selection(Map.of(), Set.of(), true);
            Map<String, List<Target>> filtered = new HashMap<>();
            for (Map.Entry<String, List<Target>> e : targetsByPath.entrySet()) {
                List<Target> list = e.getValue().stream().filter(t -> selected.contains(t.manifestId)).toList();
                if (!list.isEmpty()) filtered.put(e.getKey(), list);
            }
            return new Selection(Map.copyOf(filtered), Set.copyOf(selected), true);
        }
    }

    // ---- JSON loading helpers ----------------------------------------------------------------

    static String normalizePath(String path) {
        String n = path.replace('\\', '/');
        return n.startsWith("/") ? n : "/" + n;
    }

    private static void addManifest(Map<String, List<Target>> targets, Map<String, Manifest> manifests, String resource)
            throws IOException, ShaderPatchCompiler.CompileException {
        Manifest manifest = loadManifest(resource);
        if (manifests.putIfAbsent(manifest.id, manifest) != null) throw new IllegalArgumentException("Duplicate shader patch manifest " + manifest.id);
        for (Target t : manifest.targets) targets.computeIfAbsent(t.path, ignored -> new ArrayList<>()).add(t);
    }

    private static Manifest loadManifest(String manifestPath) throws IOException, ShaderPatchCompiler.CompileException {
        JsonObject json = readObject(manifestPath);
        requireSchema(json, manifestPath);
        String id = requireString(json, "id");
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
        Identity identity = readIdentity(requireObject(json, "identity"));
        requireObject(json, "compatibility");
        String base = parent(manifestPath);
        ArrayList<Target> targets = new ArrayList<>();
        for (JsonElement el : requireArray(json, "targets")) {
            JsonObject tj = el.getAsJsonObject();
            String path = normalizePath(requireString(tj, "path"));
            String patchPath = resolve(base, requireString(tj, "patch"));
            ShaderPatchCompiler.PatchProgram program;
            try (InputStreamReader reader = readResource(patchPath)) { program = ShaderPatchCompiler.parse(reader); }
            targets.add(new Target(id, priority, path, requireString(tj, "marker"), readStages(tj), program));
        }
        if (targets.isEmpty()) throw new IllegalArgumentException("Shader patch manifest has no targets: " + id);
        return new Manifest(id, priority, identity, List.copyOf(targets));
    }

    private static Set<ShaderPatchCompiler.ShaderStage> readStages(JsonObject tj) throws ShaderPatchCompiler.CompileException {
        if (tj.has("stage")) return Set.of(ShaderPatchCompiler.ShaderStage.parse(tj.get("stage").getAsString()));
        if (!tj.has("stages")) return Set.of();
        HashSet<ShaderPatchCompiler.ShaderStage> stages = new HashSet<>();
        for (JsonElement el : requireArray(tj, "stages")) stages.add(ShaderPatchCompiler.ShaderStage.parse(el.getAsString()));
        return Set.copyOf(stages);
    }

    private static Identity readIdentity(JsonObject obj) {
        String family = requireString(obj, "family"), profile = requireString(obj, "profile"), policy = requireString(obj, "policy");
        Pattern namePattern = null;
        if (obj.has("packNameRegex")) {
            try { namePattern = Pattern.compile(obj.get("packNameRegex").getAsString(), Pattern.CASE_INSENSITIVE); }
            catch (PatternSyntaxException e) { throw new IllegalArgumentException("Invalid shaderpack identity regex", e); }
        }
        return new Identity(family, profile, policy, namePattern);
    }

    private static JsonObject readObject(String resource) throws IOException {
        try (InputStreamReader reader = readResource(resource)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }

    private static InputStreamReader readResource(String resource) throws IOException {
        InputStream s = ShaderPatchRepo.class.getClassLoader().getResourceAsStream(resource);
        if (s == null) throw new IOException("Missing resource " + resource);
        return new InputStreamReader(s, StandardCharsets.UTF_8);
    }

    private static void requireSchema(JsonObject obj, String resource) {
        if (!obj.has("schemaVersion") || obj.get("schemaVersion").getAsInt() != 1)
            throw new IllegalArgumentException("Unsupported shader patch manifest schema in " + resource);
    }

    private static JsonObject requireObject(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonObject()) throw new IllegalArgumentException("Missing object " + key);
        return obj.getAsJsonObject(key);
    }

    private static JsonArray requireArray(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) throw new IllegalArgumentException("Missing array " + key);
        return obj.getAsJsonArray(key);
    }

    private static String requireString(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) throw new IllegalArgumentException("Missing string " + key);
        return obj.get(key).getAsString();
    }

    private static String parent(String path) { int i = path.lastIndexOf('/'); return i < 0 ? "" : path.substring(0, i + 1); }
    private static String resolve(String base, String path) { return path.startsWith("/") ? path.substring(1) : base + path; }
}
