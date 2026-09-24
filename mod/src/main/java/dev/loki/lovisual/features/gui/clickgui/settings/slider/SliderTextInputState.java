package dev.loki.lovisual.features.gui.clickgui.settings.slider;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderSetting;

public final class SliderTextInputState {
    private SliderTextInputState() {
    }

    static String sanitizeSliderText(String text, boolean integer) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        boolean dot = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c == '-' && out.length() == 0) {
                out.append('-');
            } else if (!integer && (c == '.' || c == ',') && !dot) {
                out.append('.');
                dot = true;
            }
        }
        return out.toString();
    }

    static void selectAllSliderText(SliderSetting.UiState ui) {
        ui.editSelection.begin(0, 0);
        ui.editSelection.updateCaret(0, ui.editBuffer == null ? 0 : ui.editBuffer.length());
        ui.editCursor = ui.editBuffer == null ? 0 : ui.editBuffer.length();
    }

    static boolean hasSliderSelection(SliderSetting.UiState ui) {
        return ui.editSelection.hasRange();
    }

    static int sliderSelStart(SliderSetting.UiState ui) {
        return hasSliderSelection(ui) ? Math.max(0, Math.min(ui.editSelection.start(), ui.editBuffer.length())) : -1;
    }

    static int sliderSelEnd(SliderSetting.UiState ui) {
        return hasSliderSelection(ui) ? Math.max(0, Math.min(ui.editSelection.end(), ui.editBuffer.length())) : -1;
    }

    static String selectedOrAllSliderText(SliderSetting.UiState ui) {
        if (hasSliderSelection(ui)) return ui.editBuffer.substring(sliderSelStart(ui), sliderSelEnd(ui));
        return ui.editBuffer == null ? "" : ui.editBuffer;
    }

    static boolean deleteSliderSelection(SliderSetting.UiState ui) {
        if (!hasSliderSelection(ui)) return false;
        int start = sliderSelStart(ui);
        int end = sliderSelEnd(ui);
        ui.editBuffer = ui.editBuffer.substring(0, start) + ui.editBuffer.substring(end);
        ui.editCursor = start;
        ui.editSelection.clear();
        return true;
    }

    static void insertSliderTextRaw(SliderSetting.UiState ui, String text) {
        if (text == null || text.isEmpty()) return;
        deleteSliderSelection(ui);
        int cursor = Math.max(0, Math.min(ui.editCursor, ui.editBuffer.length()));
        ui.editBuffer = ui.editBuffer.substring(0, cursor) + text + ui.editBuffer.substring(cursor);
        ui.editCursor = cursor + text.length();
        ui.editSelection.clear();
    }

    static void moveSliderCursor(SliderSetting.UiState ui, int dir, boolean shift) {
        setSliderCursor(ui, Math.max(0, Math.min(ui.editBuffer.length(), ui.editCursor + dir)), shift);
    }

    static void setSliderCursor(SliderSetting.UiState ui, int pos, boolean shift) {
        int next = Math.max(0, Math.min(ui.editBuffer == null ? 0 : ui.editBuffer.length(), pos));
        updateSelection(ui, next, shift);
        ui.editCursor = next;
        ui.cursorBlink = 0f;
    }

    private static void updateSelection(SliderSetting.UiState ui, int next, boolean shift) {
        if (shift) {
            if (!ui.editSelection.hasRange()) ui.editSelection.begin(0, ui.editCursor);
            ui.editSelection.updateCaret(0, next);
        } else {
            ui.editSelection.clear();
        }
    }

    static void pulseSliderError(SliderSetting.UiState ui) {
        ui.errorAnim = 1f;
    }

    static boolean sliderInteger(SliderSetting<?> setting) {
        Object current = setting.value().get();
        return current instanceof Integer || current instanceof Long;
    }

    static boolean sliderTextCanCommit(SliderSetting<?> setting, String raw) {
        if (raw == null) return false;
        String text = raw.trim().replace(',', '.');
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) return false;
        if (sliderInteger(setting) && text.indexOf('.') >= 0) return false;
        try {
            double parsed = Double.parseDouble(text);
            return Double.isFinite(parsed);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean sliderTextHasDecimal(String text) {
        return text != null && (text.indexOf('.') >= 0 || text.indexOf(',') >= 0);
    }

    static void validateSliderLive(SliderSetting<?> setting, SliderSetting.UiState ui) {
        if (ui.editBuffer == null || ui.editBuffer.isEmpty() || ui.editBuffer.equals("-") || ui.editBuffer.equals("."))
            return;
        if (!sliderTextCanCommit(setting, ui.editBuffer)) pulseSliderError(ui);
    }
}
