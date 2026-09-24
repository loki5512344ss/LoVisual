package dev.loki.lovisual.render.iris.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

final class PatchParseSupport {
    private PatchParseSupport() {
    }

    static ShaderPatchCompiler.Operation readHashRangeOperation(String[] parts, BufferedReader reader, int lineNumber, String directive) throws IOException, ShaderPatchCompiler.CompileException {
        if (parts.length < 3 || parts.length > 6) {
            throw new ShaderPatchCompiler.CompileException("Invalid " + directive + " at line " + lineNumber);
        }
        ShaderPatchCompiler.Selector start = new ShaderPatchCompiler.Selector(
                ShaderPatchCompiler.SelectorKind.HASH,
                validateHash(parts[1], lineNumber),
                readPositive(parts, 3, 1, lineNumber),
                parts.length <= 3
        );
        ShaderPatchCompiler.Selector end = new ShaderPatchCompiler.Selector(
                ShaderPatchCompiler.SelectorKind.HASH,
                validateHash(parts[2], lineNumber),
                readPositive(parts, 4, 1, lineNumber),
                parts.length <= 4
        );
        boolean includeEnd = parts.length >= 6 && Boolean.parseBoolean(parts[5]);
        return new ShaderPatchCompiler.Operation(
                ShaderPatchCompiler.OperationType.valueOf(directive.toUpperCase(Locale.ROOT)),
                start,
                end,
                includeEnd,
                readPayload(reader, lineNumber)
        );
    }

    static ShaderPatchCompiler.Operation readSingleSelectorOperation(String[] parts, BufferedReader reader, int lineNumber, String directive, ShaderPatchCompiler.SelectorKind kind) throws IOException, ShaderPatchCompiler.CompileException {
        ShaderPatchCompiler.OperationType operationType = operationType(directive);
        ShaderPatchCompiler.Selector selector = readSelector(parts, kind, lineNumber);
        return new ShaderPatchCompiler.Operation(
                operationType,
                selector,
                null,
                false,
                operationType == ShaderPatchCompiler.OperationType.REMOVE_LINE ? List.of() : readPayload(reader, lineNumber)
        );
    }

    static ShaderPatchCompiler.OperationType operationType(String directive) throws ShaderPatchCompiler.CompileException {
        if (directive.startsWith("insert_before")) return ShaderPatchCompiler.OperationType.INSERT_BEFORE;
        if (directive.startsWith("insert_after")) return ShaderPatchCompiler.OperationType.INSERT_AFTER;
        if (directive.startsWith("replace_line")) return ShaderPatchCompiler.OperationType.REPLACE_LINE;
        if (directive.startsWith("remove_line")) return ShaderPatchCompiler.OperationType.REMOVE_LINE;
        throw new ShaderPatchCompiler.CompileException("Unsupported operation directive @" + directive);
    }

    static ShaderPatchCompiler.Selector readZoneSelector(String[] parts, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (parts.length != 2) {
            throw new ShaderPatchCompiler.CompileException("Invalid insert_zone directive at line " + lineNumber);
        }
        String zone = parts[1].toLowerCase(Locale.ROOT);
        if (!zone.equals("declarations") && !zone.equals("functions")) {
            throw new ShaderPatchCompiler.CompileException("Unsupported insert_zone " + parts[1] + " at line " + lineNumber);
        }
        return new ShaderPatchCompiler.Selector(ShaderPatchCompiler.SelectorKind.ZONE, zone, 1, false);
    }

    static ShaderPatchCompiler.Selector readSelector(String[] parts, ShaderPatchCompiler.SelectorKind kind, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (kind == ShaderPatchCompiler.SelectorKind.DRAWBUFFERS) {
            if (parts.length < 1 || parts.length > 2) {
                throw new ShaderPatchCompiler.CompileException("Invalid drawbuffers selector at line " + lineNumber);
            }
            return new ShaderPatchCompiler.Selector(ShaderPatchCompiler.SelectorKind.DRAWBUFFERS, "DRAWBUFFERS", readPositive(parts, 1, 1, lineNumber), parts.length == 1);
        }
        if (parts.length < 2 || parts.length > 3) {
            throw new ShaderPatchCompiler.CompileException("Invalid selector at line " + lineNumber);
        }
        String value = kind == ShaderPatchCompiler.SelectorKind.HASH ? validateHash(parts[1], lineNumber) : validateName(parts[1], lineNumber);
        return new ShaderPatchCompiler.Selector(kind, value, readPositive(parts, 2, 1, lineNumber), parts.length == 2);
    }

    static ShaderPatchCompiler.Selector readHashSelector(String[] parts, int signatureIndex, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (parts.length < signatureIndex + 1 || parts.length > signatureIndex + 2) {
            throw new ShaderPatchCompiler.CompileException("Invalid hash anchor at line " + lineNumber);
        }
        return new ShaderPatchCompiler.Selector(
                ShaderPatchCompiler.SelectorKind.HASH,
                validateHash(parts[signatureIndex], lineNumber),
                readPositive(parts, signatureIndex + 1, 1, lineNumber),
                parts.length == signatureIndex + 1
        );
    }

    static String readHash(String[] parts, int index, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (parts.length != index + 1) {
            throw new ShaderPatchCompiler.CompileException("Invalid hash directive at line " + lineNumber);
        }
        return validateHash(parts[index], lineNumber);
    }

    static String readName(String[] parts, int index, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (parts.length != index + 1) {
            throw new ShaderPatchCompiler.CompileException("Invalid identifier directive at line " + lineNumber);
        }
        return validateName(parts[index], lineNumber);
    }

    static String validateHash(String hash, int lineNumber) throws ShaderPatchCompiler.CompileException {
        String normalized = hash.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new ShaderPatchCompiler.CompileException("Invalid structural hash at line " + lineNumber);
        }
        return normalized;
    }

    static String validateName(String name, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new ShaderPatchCompiler.CompileException("Invalid identifier '" + name + "' at line " + lineNumber);
        }
        return name;
    }

    static int readPositive(String[] parts, int index, int fallback, int lineNumber) throws ShaderPatchCompiler.CompileException {
        if (parts.length <= index) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(parts[index]);
            if (value < 1) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ShaderPatchCompiler.CompileException("Invalid occurrence at line " + lineNumber);
        }
    }

    static List<String> readPayload(BufferedReader reader, int directiveLine) throws IOException, ShaderPatchCompiler.CompileException {
        java.util.ArrayList<String> payload = new java.util.ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().equals("@end")) {
                return List.copyOf(payload);
            }
            payload.add(line);
        }
        throw new ShaderPatchCompiler.CompileException("Unterminated payload after line " + directiveLine);
    }
}
