
package dev.loki.lovisual.render.iris.patch;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Locale;
import java.util.Set;
public enum ShaderPatchCompiler {
    ;
    public static PatchProgram parse(Reader source) throws IOException, CompileException {
        return PatchParser.parse(source);
    }
    public static CompiledPatch compile(PatchProgram program, ImmutableList<String> source) throws CompileException {
        return compile(program, source, CompileMode.PREFLIGHT);
    }
    public static CompiledPatch compile(PatchProgram program, ImmutableList<String> source, CompileMode mode) throws CompileException {
        return compile(program, source, mode, Set.of());
    }
    public static CompiledPatch compile(PatchProgram program, ImmutableList<String> source, CompileMode mode, Set<ShaderStage> stages) throws CompileException {
        return PatchCompileHelper.compile(program, source, mode, stages);
    }
    public static ImmutableList<String> apply(CompiledPatch patch, ImmutableList<String> source) {
        return PatchCompileHelper.apply(patch, source);
    }
    public static String signature(String sourceLine) {
        return PatchCompileHelper.signature(sourceLine);
    }
    public enum CompileMode { PREFLIGHT, APPLICATION }
    public enum ShaderStage {
        ANY, FRAGMENT, VERTEX, GEOMETRY, COMPUTE;
        public static ShaderStage parse(String value) throws CompileException {
            if (value == null || value.isBlank()) return ANY;
            try { return ShaderStage.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw new CompileException("Unsupported shader stage '" + value + "'"); }
        }
    }
    public enum ProbeType {
        REQUIRE_HASH(true, "hash"), FORBID_HASH(false, "hash"),
        REQUIRE_IDENT(true, "ident"), FORBID_IDENT(false, "ident"),
        REQUIRE_FUNCTION(true, "function"), FORBID_FUNCTION(false, "function"),
        REQUIRE_CALL(true, "call"), FORBID_CALL(false, "call"),
        REQUIRE_UNIFORM(true, "uniform"), FORBID_UNIFORM(false, "uniform"),
        REQUIRE_DEFINE(true, "define"), FORBID_DEFINE(false, "define");
        private final boolean required; private final String label;
        ProbeType(boolean required, String label) { this.required = required; this.label = label; }
        boolean required() { return required; }
        String label() { return label; }
    }
    public enum SelectorKind { HASH, IDENT, FUNCTION, FUNCTION_OPEN, DEFINE, DRAWBUFFERS, STAGE_BEGIN, ZONE }
    public enum OperationType { INSERT_BEFORE, INSERT_AFTER, REPLACE_LINE, REMOVE_LINE, REPLACE_RANGE, REPLACE_BETWEEN }
    public record PatchProgram(int schemaVersion, List<Probe> probes, List<Operation> operations) { }
    public record CompiledPatch(List<Edit> edits) { }
    public record Edit(int start, int end, List<String> payload) { }
    public record Probe(ProbeType type, String value) { }
    public record Selector(SelectorKind kind, String value, int occurrence, boolean requireUnique) { }
    public record Operation(OperationType type, Selector anchor, Selector end, boolean includeEnd, List<String> payload) { }
    public static final class CompileException extends Exception {
        public CompileException(String message) { super(message); }
    }
}
