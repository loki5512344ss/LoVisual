/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.render.iris.patch;
import com.google.common.collect.ImmutableList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
final class PatchCompileHelper {
    private PatchCompileHelper() {}
    static ShaderPatchCompiler.CompiledPatch compile(ShaderPatchCompiler.PatchProgram program, ImmutableList<String> source, ShaderPatchCompiler.CompileMode mode, Set<ShaderPatchCompiler.ShaderStage> stages) throws ShaderPatchCompiler.CompileException {
        if (mode == ShaderPatchCompiler.CompileMode.PREFLIGHT && !containsLoVisualPayload(source)) validateNoSourceExcerpt(program, source);
        PatchSourceHelper.SourceIndex index = PatchSourceHelper.SourceIndex.create(source);
        PatchSourceHelper.validateProbes(program, index);
        ArrayList<ShaderPatchCompiler.Edit> edits = new ArrayList<>(program.operations().size());
        for (ShaderPatchCompiler.Operation operation : program.operations()) {
            int anchor = PatchSourceHelper.find(index, operation.anchor());
            switch (operation.type()) {
                case INSERT_BEFORE -> edits.add(new ShaderPatchCompiler.Edit(anchor, anchor, operation.payload()));
                case INSERT_AFTER -> edits.add(new ShaderPatchCompiler.Edit(anchor + 1, anchor + 1, operation.payload()));
                case REPLACE_LINE -> edits.add(new ShaderPatchCompiler.Edit(anchor, anchor + 1, operation.payload()));
                case REMOVE_LINE -> edits.add(new ShaderPatchCompiler.Edit(anchor, anchor + 1, List.of()));
                case REPLACE_RANGE -> {
                    int end = PatchSourceHelper.find(index, operation.end());
                    if (end < anchor) throw new ShaderPatchCompiler.CompileException("Range end precedes start");
                    edits.add(new ShaderPatchCompiler.Edit(anchor, operation.includeEnd() ? end + 1 : end, operation.payload()));
                }
                case REPLACE_BETWEEN -> {
                    int end = PatchSourceHelper.find(index, operation.end());
                    if (end <= anchor) throw new ShaderPatchCompiler.CompileException("Range end does not follow start");
                    edits.add(new ShaderPatchCompiler.Edit(anchor + 1, end, operation.payload()));
                }
            }
        }
        validateNonOverlapping(edits);
        validateStageBounds(edits, source, stages);
        validateDelimiterDelta(edits, source);
        edits.sort(Comparator.comparingInt(ShaderPatchCompiler.Edit::start).thenComparingInt(ShaderPatchCompiler.Edit::end).reversed());
        return new ShaderPatchCompiler.CompiledPatch(List.copyOf(edits));
    }
    static ImmutableList<String> apply(ShaderPatchCompiler.CompiledPatch patch, ImmutableList<String> source) {
        ArrayList<String> output = new ArrayList<>(source);
        for (ShaderPatchCompiler.Edit edit : patch.edits()) {
            output.subList(edit.start(), edit.end()).clear();
            output.addAll(edit.start(), edit.payload());
        }
        return ImmutableList.copyOf(output);
    }
    static String signature(String sourceLine) {
        String normalized = normalizeLine(sourceLine);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
    static boolean containsLoVisualPayload(List<String> source) {
        for (String line : source) if (line.contains("lovisual") || line.contains("LOVISUAL_")) return true;
        return false;
    }
    static void validateNoSourceExcerpt(ShaderPatchCompiler.PatchProgram program, List<String> source) throws ShaderPatchCompiler.CompileException {
        List<String> normalizedSource = source.stream().map(PatchCompileHelper::normalizeLine).toList();
        for (ShaderPatchCompiler.Operation operation : program.operations()) {
            List<String> payload = operation.payload().stream().map(PatchCompileHelper::normalizeLine).toList();
            for (int payloadStart = 0; payloadStart + 2 < payload.size(); payloadStart++) {
                if (payload.get(payloadStart).isEmpty() || payload.get(payloadStart + 1).isEmpty() || payload.get(payloadStart + 2).isEmpty()) continue;
                for (int sourceStart = 0; sourceStart + 2 < normalizedSource.size(); sourceStart++) {
                    if (payload.get(payloadStart).equals(normalizedSource.get(sourceStart)) && payload.get(payloadStart + 1).equals(normalizedSource.get(sourceStart + 1)) && payload.get(payloadStart + 2).equals(normalizedSource.get(sourceStart + 2))) {
                        throw new ShaderPatchCompiler.CompileException("Patch payload reproduces a target source excerpt");
                    }
                }
            }
        }
    }
    static String normalizeLine(String sourceLine) {
        String source = sourceLine == null ? "" : sourceLine;
        StringBuilder normalized = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) { char c = source.charAt(i); if (!Character.isWhitespace(c)) normalized.append(c); }
        return normalized.toString();
    }
    static void validateNonOverlapping(List<ShaderPatchCompiler.Edit> edits) throws ShaderPatchCompiler.CompileException {
        ArrayList<ShaderPatchCompiler.Edit> sorted = new ArrayList<>(edits);
        sorted.sort(Comparator.comparingInt(ShaderPatchCompiler.Edit::start).thenComparingInt(ShaderPatchCompiler.Edit::end));
        int previousEnd = -1;
        for (ShaderPatchCompiler.Edit edit : sorted) {
            if (edit.start() < previousEnd) throw new ShaderPatchCompiler.CompileException("Patch operations overlap");
            previousEnd = Math.max(previousEnd, edit.end());
        }
    }
    static void validateStageBounds(List<ShaderPatchCompiler.Edit> edits, List<String> source, Set<ShaderPatchCompiler.ShaderStage> stages) throws ShaderPatchCompiler.CompileException {
        if (stages == null || stages.isEmpty() || stages.contains(ShaderPatchCompiler.ShaderStage.ANY) || !sourceHasStageBlocks(source)) return;
        for (ShaderPatchCompiler.Edit edit : edits) {
            ShaderPatchCompiler.ShaderStage stage = stageBeforeLine(source, edit.start());
            if (stage == null || !stages.contains(stage)) throw new ShaderPatchCompiler.CompileException("Patch edit escapes allowed shader stage(s): editLine=" + (edit.start() + 1) + " stage=" + (stage == null ? "none" : stage.name().toLowerCase(Locale.ROOT)) + " allowed=" + stages);
        }
    }
    static boolean sourceHasStageBlocks(List<String> source) {
        for (String line : source) {
            String stripped = PatchSourceHelper.stripLineComment(line).trim();
            if (stripped.contains("FRAGMENT_SHADER") || stripped.contains("VERTEX_SHADER") || stripped.contains("GEOMETRY_SHADER") || stripped.contains("COMPUTE_SHADER")) return true;
        }
        return false;
    }
    static ShaderPatchCompiler.ShaderStage stageBeforeLine(List<String> source, int lineIndex) {
        ArrayList<ShaderPatchCompiler.ShaderStage> stack = new ArrayList<>();
        int end = Math.min(Math.max(lineIndex, 0), source.size());
        for (int i = 0; i < end; i++) {
            String stripped = PatchSourceHelper.stripLineComment(source.get(i)).trim();
            ShaderPatchCompiler.ShaderStage begin = stageFromDirective(stripped);
            if (begin != null) stack.add(begin);
            else if (stripped.startsWith("#endif") && !stack.isEmpty()) stack.remove(stack.size() - 1);
        }
        for (int i = stack.size() - 1; i >= 0; i--) { ShaderPatchCompiler.ShaderStage stage = stack.get(i); if (stage != ShaderPatchCompiler.ShaderStage.ANY) return stage; }
        return null;
    }
    static ShaderPatchCompiler.ShaderStage stageFromDirective(String stripped) {
        if (!stripped.startsWith("#if")) return null;
        if (stripped.contains("FRAGMENT_SHADER")) return ShaderPatchCompiler.ShaderStage.FRAGMENT;
        if (stripped.contains("VERTEX_SHADER")) return ShaderPatchCompiler.ShaderStage.VERTEX;
        if (stripped.contains("GEOMETRY_SHADER")) return ShaderPatchCompiler.ShaderStage.GEOMETRY;
        if (stripped.contains("COMPUTE_SHADER")) return ShaderPatchCompiler.ShaderStage.COMPUTE;
        return null;
    }
    static void validateDelimiterDelta(List<ShaderPatchCompiler.Edit> edits, List<String> source) throws ShaderPatchCompiler.CompileException {
        for (char open : new char[]{'{', '(', '['}) {
            char close = switch (open) { case '{' -> '}'; case '(' -> ')'; case '[' -> ']'; default -> throw new IllegalStateException(); };
            int delta = 0;
            for (ShaderPatchCompiler.Edit edit : edits) {
                delta += delimiterBalance(edit.payload(), open, close);
                delta -= delimiterBalance(source.subList(edit.start(), edit.end()), open, close);
            }
            if (delta != 0) throw new ShaderPatchCompiler.CompileException("Patch changes " + open + close + " delimiter balance");
        }
    }
    static int delimiterBalance(List<String> lines, char open, char close) {
        int balance = 0;
        for (String line : lines) for (int i = 0; i < line.length(); i++) { char c = line.charAt(i); if (c == open) balance++; if (c == close) balance--; }
        return balance;
    }
}
