package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.FunctionBindSetting;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class KeybindsData {

    private KeybindsData() {}

    static String iconFor(ModuleCategory category) {
        if (category == null) return "ellipsis";
        return switch (category) {
            case COMBAT -> "swords";
            case MOVEMENT -> "accessibility";
            case PLAYER -> "user";
            case VISUALS -> "tree-pine";
            case ACCESSORIES -> "shirt";
            default -> "ellipsis";
        };
    }

    static String cleanActionLabel(FunctionBindSetting setting) {
        if (setting == null) return "";
        String explicit = setting.getHudLabel();
        if (explicit != null && !explicit.isBlank()) return explicit;
        String actionId = setting.getActionId();
        String display = setting.getDisplayName();
        if (display == null || display.isBlank() || display.equals("Function: " + actionId)) display = actionId;
        display = display.replace("Function: ", "").replace('_', ' ').replace('-', ' ').trim();
        if (display.isEmpty()) return "";
        StringBuilder out = new StringBuilder(display.length());
        boolean cap = true;
        for (int i = 0; i < display.length(); i++) {
            char c = display.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!out.isEmpty() && out.charAt(out.length() - 1) != ' ') out.append(' ');
                cap = true;
                continue;
            }
            out.append(cap ? Character.toUpperCase(c) : c);
            cap = false;
        }
        return out.toString();
    }

    static String formatBind(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String[] parts = raw.trim().toUpperCase(Locale.ROOT).split("\\+");
        List<String> out = new ArrayList<>(parts.length);
        for (String part : parts) {
            String mapped = formatBindPart(part.trim());
            if (!mapped.isEmpty()) out.add(mapped);
        }
        return String.join("+", out);
    }

    static String formatBindPart(String value) {
        return switch (value) {
            case "LEFT_CONTROL", "RIGHT_CONTROL", "LEFT_CTRL", "RIGHT_CTRL", "CONTROL", "CTRL" -> "CTRL";
            case "LEFT_SHIFT", "RIGHT_SHIFT", "SHIFT" -> "SHIFT";
            case "LEFT_ALT", "RIGHT_ALT", "ALT" -> "ALT";
            case "LEFT_SUPER", "RIGHT_SUPER", "SUPER" -> "WIN";
            case "GRAVE_ACCENT" -> "`";
            case "COMMA" -> ",";
            case "PERIOD" -> ".";
            case "SEMICOLON" -> ";";
            case "APOSTROPHE" -> "'";
            case "SLASH" -> "/";
            case "BACKSLASH" -> "\\";
            case "MINUS" -> "-";
            case "EQUAL" -> "=";
            case "SPACE" -> "SPC";
            case "ENTER" -> "ENT";
            case "ESCAPE" -> "ESC";
            case "DELETE" -> "DEL";
            case "INSERT" -> "INS";
            case "CAPS_LOCK" -> "CAPS";
            case "LEFT_BRACKET" -> "[";
            case "RIGHT_BRACKET" -> "]";
            default -> value.startsWith("MOUSE_BUTTON_") ? "M" + value.substring("MOUSE_BUTTON_".length()) : value;
        };
    }

    static List<Keybinds.Row> collectRows(Keybinds host) {
        List<Keybinds.Row> rows = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module == null || !module.isEnabled()) continue;
            String name = module.getDisplayName();
            if (name == null || name.isBlank()) name = module.name();
            String moduleIcon = iconFor(module.getCategory());
            if (module.getKeyBindSetting() != null && module.getKeyBindSetting().getValue() != null) {
                String rawBind = module.getKeyBindSetting().getValue().get();
                if (rawBind != null && !rawBind.isBlank() && !"NONE".equalsIgnoreCase(rawBind)) {
                    rows.add(new Keybinds.Row(module.name(), name, formatBind(rawBind), moduleIcon, false));
                }
            }
            for (FunctionBindSetting action : module.getActionSettings()) {
                if (action == null || !isLocalActionVisible(host, action)) continue;
                String rawBind = action.get();
                if (rawBind == null || rawBind.isBlank() || "NONE".equalsIgnoreCase(rawBind)) continue;
                String actionLabel = cleanActionLabel(action);
                if (actionLabel.isBlank()) actionLabel = action.getActionId();
                String actionIcon = action.getHudIcon();
                if (actionIcon == null || actionIcon.isBlank()) actionIcon = moduleIcon;
                rows.add(new Keybinds.Row(module.name() + ":action:" + action.getActionId(), name + ": " + actionLabel, formatBind(rawBind), actionIcon, true));
            }
        }
        rows.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return rows;
    }

    static boolean isLocalActionVisible(Keybinds host, FunctionBindSetting action) {
        if (!host.localActions.get()) return false;
        BindMode mode = action.getMode();
        if (mode == BindMode.HOLD) return host.localActionModes.get(Keybinds.MODE_HOLD) && action.isHeldForHud();
        return host.localActionModes.get(Keybinds.MODE_PRESS) && action.isHudToggle() && action.isHudToggleActive();
    }

    static List<LinkedHashMap<String, Object>> buildPanelRows(Keybinds host, List<Keybinds.AnimatedRow> animatedRows, boolean showExampleRow) {
        List<LinkedHashMap<String, Object>> out = ScriptedListHudPanel.rows();
        if (showExampleRow) {
            LinkedHashMap<String, Object> row = ScriptedListHudPanel.row("preview", "svg", "swords",
                    List.of(ScriptedListHudPanel.textPart("KillAura", KeybindsPalette.withAlpha(host.uiText & 0x00FFFFFF, 255), 0.0f)),
                    "G", KeybindsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, 255),
                    KeybindsPalette.withAlpha(host.uiMuted & 0x00FFFFFF, 130), 1.0f);
            row.put("iconTint", ScriptedListHudPanel.hex(KeybindsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, 255)));
            out.add(row);
            return out;
        }
        for (Keybinds.AnimatedRow ar : animatedRows) {
            float anim = ar.anim();
            if (anim <= 0.01f) continue;
            Keybinds.Row r = ar.row();
            int rowAlpha = KeybindsAnimator.clamp255(Math.round(255.0f * anim));
            int rowNameColor = r.local() ? KeybindsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, rowAlpha) : KeybindsPalette.withAlpha(host.uiText & 0x00FFFFFF, rowAlpha);
            int rowRightColor = KeybindsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, rowAlpha);
            int rowDividerColor = r.local() ? KeybindsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, Math.max(42, rowAlpha - 94)) : KeybindsPalette.withAlpha(host.uiMuted & 0x00FFFFFF, Math.max(26, rowAlpha - 120));
            LinkedHashMap<String, Object> row = ScriptedListHudPanel.row(r.key(), "svg", r.icon(),
                    List.of(ScriptedListHudPanel.textPart(r.name(), rowNameColor, 0.0f)), r.bindText(), rowRightColor, rowDividerColor, anim);
            row.put("iconTint", ScriptedListHudPanel.hex(KeybindsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, rowAlpha)));
            out.add(row);
        }
        return out;
    }
}
