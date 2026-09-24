package dev.loki.lovisual.render.iris.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PatchParser {
    private PatchParser() {
    }

    static ShaderPatchCompiler.PatchProgram parse(Reader source) throws IOException, ShaderPatchCompiler.CompileException {
        BufferedReader reader = source instanceof BufferedReader buffered ? buffered : new BufferedReader(source);
        ArrayList<ShaderPatchCompiler.Probe> probes = new ArrayList<>();
        ArrayList<ShaderPatchCompiler.Operation> operations = new ArrayList<>();
        int schemaVersion = 1;
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(";")) {
                continue;
            }
            if (!trimmed.startsWith("@")) {
                throw new ShaderPatchCompiler.CompileException("Expected directive at line " + lineNumber);
            }

            String[] parts = trimmed.substring(1).split("\\s+");
            String directive = parts[0];
            switch (directive) {
                case "patch" -> {
                    if (parts.length != 2) {
                        throw new ShaderPatchCompiler.CompileException("Invalid patch schema directive at line " + lineNumber);
                    }
                    try {
                        schemaVersion = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        throw new ShaderPatchCompiler.CompileException("Invalid patch schema at line " + lineNumber);
                    }
                    if (schemaVersion != 1 && schemaVersion != 2) {
                        throw new ShaderPatchCompiler.CompileException("Unsupported patch schema " + schemaVersion + " at line " + lineNumber);
                    }
                }
                case "require" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_HASH, PatchParseSupport.readHash(parts, 1, lineNumber)));
                case "forbid" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_HASH, PatchParseSupport.readHash(parts, 1, lineNumber)));
                case "require_hash" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_HASH, PatchParseSupport.readHash(parts, 1, lineNumber)));
                case "forbid_hash" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_HASH, PatchParseSupport.readHash(parts, 1, lineNumber)));
                case "require_ident" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_IDENT, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "forbid_ident" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_IDENT, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "require_function" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_FUNCTION, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "forbid_function" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_FUNCTION, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "require_call" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_CALL, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "forbid_call" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_CALL, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "require_uniform" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_UNIFORM, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "forbid_uniform" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_UNIFORM, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "require_define" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.REQUIRE_DEFINE, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "forbid_define" -> probes.add(new ShaderPatchCompiler.Probe(ShaderPatchCompiler.ProbeType.FORBID_DEFINE, PatchParseSupport.readName(parts, 1, lineNumber)));
                case "insert_before", "insert_after", "replace_line", "remove_line" -> {
                    ShaderPatchCompiler.Selector selector = PatchParseSupport.readHashSelector(parts, 1, lineNumber);
                    operations.add(new ShaderPatchCompiler.Operation(
                            ShaderPatchCompiler.OperationType.valueOf(directive.toUpperCase(Locale.ROOT)),
                            selector,
                            null,
                            false,
                            directive.equals("remove_line") ? List.of() : PatchParseSupport.readPayload(reader, lineNumber)
                    ));
                }
                case "replace_range", "replace_between" -> operations.add(PatchParseSupport.readHashRangeOperation(parts, reader, lineNumber, directive));
                case "insert_zone" -> operations.add(new ShaderPatchCompiler.Operation(
                        ShaderPatchCompiler.OperationType.INSERT_BEFORE,
                        PatchParseSupport.readZoneSelector(parts, lineNumber),
                        null,
                        false,
                        PatchParseSupport.readPayload(reader, lineNumber)
                ));
                case "insert_before_hash", "insert_after_hash", "replace_line_hash", "remove_line_hash" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.HASH));
                case "insert_before_ident", "insert_after_ident", "replace_line_ident", "remove_line_ident" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.IDENT));
                case "insert_before_function", "replace_line_function", "remove_line_function" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.FUNCTION));
                case "insert_after_function_open" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.FUNCTION_OPEN));
                case "insert_before_define", "insert_after_define", "replace_line_define", "remove_line_define" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.DEFINE));
                case "insert_before_drawbuffers", "insert_after_drawbuffers", "replace_line_drawbuffers", "remove_line_drawbuffers" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.DRAWBUFFERS));
                case "insert_after_stage_begin" ->
                        operations.add(PatchParseSupport.readSingleSelectorOperation(parts, reader, lineNumber, directive, ShaderPatchCompiler.SelectorKind.STAGE_BEGIN));
                default -> throw new ShaderPatchCompiler.CompileException("Unknown directive @" + directive + " at line " + lineNumber);
            }
        }

        if (operations.isEmpty()) {
            throw new ShaderPatchCompiler.CompileException("Patch contains no operations");
        }
        return new ShaderPatchCompiler.PatchProgram(schemaVersion, List.copyOf(probes), List.copyOf(operations));
    }
}
