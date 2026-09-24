package dev.loki.lovisual.render.iris.patch;

import com.google.common.collect.ImmutableList;
import dev.loki.lovisual.render.iris.patch.ShaderPatchRepo.Manifest;
import dev.loki.lovisual.render.iris.patch.ShaderPatchRepo.Repository;
import dev.loki.lovisual.render.iris.patch.ShaderPatchRepo.Target;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Higher-level orchestrator for shader-pack patches: manages the global
 * {@link Repository}, per-pack {@link Session}s, manifest registration and
 * diagnostics. JSON loading logic lives in {@link ShaderPatchRepo}.
 */
public enum ShaderPatchEngine {
    ;
    private static final CopyOnWriteArrayList<String> EXTRA_MANIFEST_RESOURCES = new CopyOnWriteArrayList<>();
    private static final AtomicReference<Repository> REPOSITORY = new AtomicReference<>(Repository.load(List.of()));
    private static final Map<String, String> DIAGNOSTICS = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> LOADING_SHADER_PACK_NAME = new ThreadLocal<>();
    private static final AtomicReference<String> GLOBAL_LOADING_SHADER_PACK_NAME = new AtomicReference<>();

    /** Legacy validation session: all repository manifests are visible. */
    public static Session newSession() { Repository r = repository(); return new Session("<all>", r.targetsByPath(), Set.copyOf(r.manifestsById().keySet()), false); }

    public static Session newSession(String shaderPackName) {
        ShaderPatchRepo.Selection sel = repository().select(shaderPackName);
        return new Session(shaderPackName, sel.targetsByPath(), sel.manifestIds(), sel.nameGated());
    }

    public static boolean registerManifestResource(String manifestResourcePath) {
        String normalized = normalizeResourcePath(manifestResourcePath);
        if (normalized.isBlank()) return false;
        if (EXTRA_MANIFEST_RESOURCES.contains(normalized)) return true;
        EXTRA_MANIFEST_RESOURCES.add(normalized);
        REPOSITORY.set(Repository.load(List.copyOf(EXTRA_MANIFEST_RESOURCES)));
        DebugLog.info("[IrisPatch] addon manifest registered: %s", normalized);
        return true;
    }

    public static void beginShaderPackLoad(String shaderPackName) {
        if (shaderPackName == null || shaderPackName.isBlank()) { LOADING_SHADER_PACK_NAME.remove(); return; }
        LOADING_SHADER_PACK_NAME.set(shaderPackName);
        GLOBAL_LOADING_SHADER_PACK_NAME.set(shaderPackName);
        DebugLog.info("[IrisPatch] shaderpack load begin: '%s'", shaderPackName);
    }

    public static void endShaderPackLoad(String shaderPackName) {
        String active = LOADING_SHADER_PACK_NAME.get();
        if (active != null && (shaderPackName == null || active.equals(shaderPackName))) LOADING_SHADER_PACK_NAME.remove();
        String global = GLOBAL_LOADING_SHADER_PACK_NAME.get();
        if (global != null && (shaderPackName == null || global.equals(shaderPackName))) GLOBAL_LOADING_SHADER_PACK_NAME.compareAndSet(global, null);
        DebugLog.info("[IrisPatch] shaderpack load end: '%s'", shaderPackName == null ? "" : shaderPackName);
    }

    public static String loadingShaderPackName() {
        String t = LOADING_SHADER_PACK_NAME.get();
        return (t != null && !t.isBlank()) ? t : GLOBAL_LOADING_SHADER_PACK_NAME.get();
    }

    public static List<String> diagnostics() {
        return DIAGNOSTICS.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + ": " + e.getValue()).toList();
    }

    public static List<String> targetPaths() { return repository().targetsByPath().keySet().stream().sorted().toList(); }
    public static List<String> targetPaths(String shaderPackName) { return repository().select(shaderPackName).targetsByPath().keySet().stream().sorted().toList(); }
    
    private static Repository repository() { return REPOSITORY.get(); }
    private static void setDiagnostic(Target target, String value) { DIAGNOSTICS.put(target.manifestId() + " " + target.path(), value); }
    private static boolean contains(List<String> lines, String marker) { return marker != null && !marker.isBlank() && lines.stream().anyMatch(l -> l.contains(marker)); }

    private static String normalizeResourcePath(String path) {
        if (path == null || path.isBlank()) return "";
        String n = path.trim().replace('\\', '/');
        return n.startsWith("/") ? n.substring(1) : n;
    }

    @FunctionalInterface
    public interface SourceProvider { ImmutableList<String> load(String path); }

    /** Per-shaderpack patch session; holds a filtered view of the repository and applies patches to GLSL source. */
    public static final class Session {
        private final String shaderPackName;
        private final Map<String, List<Target>> targetsByPath;
        private final Set<String> activeManifestIds;
        private final Map<String, Boolean> manifestApplicability = new ConcurrentHashMap<>();
        private final ThreadLocal<Boolean> preflighting = ThreadLocal.withInitial(() -> false);
        private final boolean nameGated;
        private boolean announced;

        private Session(String shaderPackName, Map<String, List<Target>> targetsByPath, Set<String> activeManifestIds, boolean nameGated) {
            this.shaderPackName = shaderPackName == null ? "" : shaderPackName;
            this.targetsByPath = targetsByPath;
            this.activeManifestIds = activeManifestIds;
            this.nameGated = nameGated;
        }

        public boolean isActive() { return !targetsByPath.isEmpty(); }

        public ImmutableList<String> patch(String path, ImmutableList<String> source, SourceProvider sp) {
            if (path == null || source == null || preflighting.get()) return source;
            announceOnce();
            List<Target> candidates = targetsByPath.get(ShaderPatchRepo.normalizePath(path));
            if (candidates == null || candidates.isEmpty()) return source;
            for (Target target : candidates) {
                if (contains(source, target.marker())) { setDiagnostic(target, "already applied"); return source; }
                if (!manifestApplicability.computeIfAbsent(target.manifestId(), id -> preflight(id, sp))) continue;
                try {
                    ShaderPatchCompiler.CompiledPatch compiled = ShaderPatchCompiler.compile(target.program(), source, ShaderPatchCompiler.CompileMode.APPLICATION, target.stages());
                    setDiagnostic(target, "applied edits=" + compiled.edits().size());
                    DebugLog.info("[IrisPatch] %s %s applied edits=%d", target.manifestId(), target.path(), compiled.edits().size());
                    return ShaderPatchCompiler.apply(compiled, source);
                } catch (ShaderPatchCompiler.CompileException e) { setDiagnostic(target, "rejected: " + e.getMessage()); }
            }
            return source;
        }

        private void announceOnce() {
            if (announced) return;
            announced = true;
            DebugLog.info("[IrisPatch] session %s: shaderPack='%s' nameGated=%s manifests=%d paths=%d",
                    targetsByPath.isEmpty() ? "inactive" : "active", shaderPackName, nameGated, activeManifestIds.size(), targetsByPath.size());
        }

        private boolean preflight(String manifestId, SourceProvider sp) {
            Manifest manifest = repository().manifestsById().get(manifestId);
            if (manifest == null || !activeManifestIds.contains(manifestId)) return false;
            preflighting.set(true);
            try {
                for (Target target : manifest.targets()) {
                    ImmutableList<String> source = sp.load(target.path());
                    if (source == null) { setDiagnostic(target, "preflight rejected: source unavailable"); return false; }
                    try { ShaderPatchCompiler.compile(target.program(), source, ShaderPatchCompiler.CompileMode.PREFLIGHT, target.stages()); }
                    catch (ShaderPatchCompiler.CompileException e) { setDiagnostic(target, "preflight rejected: " + e.getMessage()); return false; }
                }
                return true;
            } finally { preflighting.set(false); }
        }
    }
}
