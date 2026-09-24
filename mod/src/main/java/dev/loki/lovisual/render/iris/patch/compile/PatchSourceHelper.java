
package dev.loki.lovisual.render.iris.patch;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
final class PatchSourceHelper {
    static final Pattern GLOBAL_DECLARATION = Pattern.compile("^(?:flat\\s+|smooth\\s+|noperspective\\s+|centroid\\s+)*(?:in|out|uniform|varying)\\b.*;");
    private PatchSourceHelper() {}
    static void validateProbes(ShaderPatchCompiler.PatchProgram program, SourceIndex index) throws ShaderPatchCompiler.CompileException {
        ArrayList<String> missing = new ArrayList<>();
        ArrayList<String> forbidden = new ArrayList<>();
        for (ShaderPatchCompiler.Probe probe : program.probes()) {
            boolean present = switch (probe.type()) {
                case REQUIRE_HASH, FORBID_HASH -> index.availableHashes().contains(probe.value());
                case REQUIRE_IDENT, FORBID_IDENT -> hasIdentifier(index.source(), probe.value());
                case REQUIRE_FUNCTION, FORBID_FUNCTION -> findFunctionStart(index.source(), probe.value(), 1, false) >= 0;
                case REQUIRE_CALL, FORBID_CALL -> hasCall(index.source(), probe.value());
                case REQUIRE_UNIFORM, FORBID_UNIFORM -> hasUniform(index.source(), probe.value());
                case REQUIRE_DEFINE, FORBID_DEFINE -> hasDefine(index.source(), probe.value());
            };
            if (probe.type().required() && !present) missing.add(probe.type().label() + " " + probe.value());
            else if (!probe.type().required() && present) forbidden.add(probe.type().label() + " " + probe.value());
        }
        if (!missing.isEmpty()) throw new ShaderPatchCompiler.CompileException("Required structural probes are absent: " + String.join(", ", missing));
        if (!forbidden.isEmpty()) throw new ShaderPatchCompiler.CompileException("Forbidden structural probes are present: " + String.join(", ", forbidden));
    }
    static int find(SourceIndex index, ShaderPatchCompiler.Selector selector) throws ShaderPatchCompiler.CompileException {
        return switch (selector.kind()) {
            case HASH -> findHash(index.hashes(), selector);
            case IDENT -> findLine(index.source(), selector, line -> hasIdentifier(line, selector.value()));
            case FUNCTION -> findFunctionStartChecked(index.source(), selector);
            case FUNCTION_OPEN -> findFunctionOpenChecked(index.source(), selector);
            case DEFINE -> findLine(index.source(), selector, line -> isDefine(line, selector.value()));
            case DRAWBUFFERS -> findLine(index.source(), selector, line -> line.contains("DRAWBUFFERS"));
            case STAGE_BEGIN -> findLine(index.source(), selector, line -> isStageBegin(line, selector.value()));
            case ZONE -> findZone(index.source(), selector.value());
        };
    }
    static int findHash(List<String> hashes, ShaderPatchCompiler.Selector selector) throws ShaderPatchCompiler.CompileException {
        int occurrence = 0; int result = -1;
        for (int i = 0; i < hashes.size(); i++) {
            if (!hashes.get(i).equals(selector.value())) continue;
            occurrence++;
            if (occurrence == selector.occurrence()) result = i;
        }
        if (result < 0) throw new ShaderPatchCompiler.CompileException("Anchor not found: hash " + selector.value() + "#" + selector.occurrence());
        if (selector.requireUnique() && occurrence != 1) throw new ShaderPatchCompiler.CompileException("Anchor is ambiguous; specify occurrence: hash " + selector.value());
        return result;
    }
    static int findLine(List<String> source, ShaderPatchCompiler.Selector selector, LinePredicate predicate) throws ShaderPatchCompiler.CompileException {
        int occurrence = 0; int result = -1;
        for (int i = 0; i < source.size(); i++) {
            if (!predicate.matches(source.get(i))) continue;
            occurrence++;
            if (occurrence == selector.occurrence()) result = i;
        }
        if (result < 0) throw new ShaderPatchCompiler.CompileException("Anchor not found: " + selector.kind().name().toLowerCase(Locale.ROOT) + " " + selector.value() + "#" + selector.occurrence());
        if (selector.requireUnique() && occurrence != 1) throw new ShaderPatchCompiler.CompileException("Anchor is ambiguous; specify occurrence: " + selector.kind().name().toLowerCase(Locale.ROOT) + " " + selector.value());
        return result;
    }
    static int findFunctionStartChecked(List<String> source, ShaderPatchCompiler.Selector selector) throws ShaderPatchCompiler.CompileException {
        int index = findFunctionStart(source, selector.value(), selector.occurrence(), selector.requireUnique());
        if (index < 0) throw new ShaderPatchCompiler.CompileException("Anchor not found: function " + selector.value() + "#" + selector.occurrence());
        return index;
    }
    static int findFunctionOpenChecked(List<String> source, ShaderPatchCompiler.Selector selector) throws ShaderPatchCompiler.CompileException {
        int start = findFunctionStartChecked(source, new ShaderPatchCompiler.Selector(ShaderPatchCompiler.SelectorKind.FUNCTION, selector.value(), selector.occurrence(), selector.requireUnique()));
        for (int i = start; i < source.size(); i++) {
            String line = stripLineComment(source.get(i));
            int brace = line.indexOf('{'); int semicolon = line.indexOf(';');
            if (brace >= 0 && (semicolon < 0 || brace < semicolon)) return i;
            if (semicolon >= 0) break;
        }
        throw new ShaderPatchCompiler.CompileException("Function opening brace not found: " + selector.value() + "#" + selector.occurrence());
    }
    static int findFunctionStart(List<String> source, String functionName, int requestedOccurrence, boolean requireUnique) {
        int occurrence = 0; int result = -1;
        for (int i = 0; i < source.size(); i++) {
            if (!looksLikeFunctionStart(source.get(i), functionName)) continue;
            occurrence++;
            if (occurrence == requestedOccurrence) result = i;
        }
        if (result < 0) return -1;
        if (requireUnique && occurrence != 1) return -2;
        return result;
    }
    static int findZone(List<String> source, String zone) {
        if (zone.equals("declarations")) {
            for (int i = 0; i < source.size(); i++) {
                String trimmed = source.get(i).trim();
                if (GLOBAL_DECLARATION.matcher(trimmed).matches()) return i;
            }
        }
        for (int i = 0; i < source.size(); i++) if (looksLikeAnyFunctionStart(source.get(i))) return i;
        return 0;
    }
    static boolean isStageBegin(String line, String stageMacro) {
        String stripped = stripLineComment(line).trim();
        return stripped.equals("#ifdef " + stageMacro) || stripped.equals("#if defined " + stageMacro) || stripped.equals("#if defined(" + stageMacro + ")");
    }
    static boolean looksLikeAnyFunctionStart(String line) {
        String stripped = stripLineComment(line).trim();
        if (stripped.isEmpty() || stripped.startsWith("#")) return false;
        return stripped.matches(".*\\b[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*") && !stripped.endsWith(";");
    }
    static boolean looksLikeFunctionStart(String line, String functionName) {
        String stripped = stripLineComment(line).trim();
        if (stripped.isEmpty() || stripped.startsWith("#")) return false;
        if (!hasIdentifier(stripped, functionName)) return false;
        int nameIndex = stripped.indexOf(functionName);
        int paren = stripped.indexOf('(', nameIndex + functionName.length());
        if (paren < 0) return false;
        int semicolon = stripped.indexOf(';');
        return semicolon < 0 || semicolon > paren;
    }
    static boolean hasCall(List<String> source, String name) {
        String needle = name + "(";
        for (String line : source) { String stripped = stripLineComment(line); if (stripped.contains(needle)) return true; }
        return false;
    }
    static boolean hasUniform(List<String> source, String name) {
        for (String line : source) { String stripped = stripLineComment(line).trim(); if (stripped.startsWith("uniform ") && hasIdentifier(stripped, name)) return true; }
        return false;
    }
    static boolean hasDefine(List<String> source, String name) {
        for (String line : source) if (isDefine(line, name)) return true;
        return false;
    }
    static boolean isDefine(String line, String name) {
        String stripped = stripLineComment(line).trim();
        if (!stripped.startsWith("#define")) return false;
        String rest = stripped.substring("#define".length()).trim();
        if (rest.startsWith(name)) return rest.length() == name.length() || !isIdentifierPart(rest.charAt(name.length()));
        return false;
    }
    static boolean hasIdentifier(List<String> source, String identifier) {
        for (String line : source) if (hasIdentifier(line, identifier)) return true;
        return false;
    }
    static boolean hasIdentifier(String line, String identifier) {
        String stripped = stripLineComment(line);
        int from = 0;
        while (from < stripped.length()) {
            int index = stripped.indexOf(identifier, from);
            if (index < 0) return false;
            int before = index - 1; int after = index + identifier.length();
            boolean validBefore = before < 0 || !isIdentifierPart(stripped.charAt(before));
            boolean validAfter = after >= stripped.length() || !isIdentifierPart(stripped.charAt(after));
            if (validBefore && validAfter) return true;
            from = index + identifier.length();
        }
        return false;
    }
    static boolean isIdentifierPart(char character) { return Character.isLetterOrDigit(character) || character == '_'; }
    static String stripLineComment(String line) { int comment = line.indexOf("//"); return comment >= 0 ? line.substring(0, comment) : line; }
    interface LinePredicate { boolean matches(String line); }
    record SourceIndex(ImmutableList<String> source, List<String> hashes, Set<String> availableHashes) {
        static SourceIndex create(ImmutableList<String> source) {
            ArrayList<String> hashes = new ArrayList<>(source.size());
            Set<String> available = new HashSet<>(source.size());
            for (String line : source) { String hash = ShaderPatchCompiler.signature(line); hashes.add(hash); available.add(hash); }
            return new SourceIndex(source, List.copyOf(hashes), Set.copyOf(available));
        }
    }
}
