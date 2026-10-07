package dev.loki.lovisual.features.gui.hud.draggable.quick;

import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.SettingFactory;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Small settings popup shown over a HUD widget while the chat is open (right click on the widget).
 * Handles toggles, sliders and mode switches; anything fancier stays in the ClickGUI.
 */
public final class HudQuickSettings {
    static final float WIDTH = 150f;
    static final float PAD = 6f;
    static final float HEADER_H = 16f;
    static final float ROW_H = 14f;
    static final float SLIDER_ROW_H = 22f;

    private static DraggableHudElement widget;
    private static List<Setting> rows = List.of();
    private static float x;
    private static float y;
    private static Setting sliding;
    private static boolean dirty;

    private HudQuickSettings() {
    }

    public static boolean isOpen() {
        return widget != null;
    }

    static DraggableHudElement widget() {
        return widget;
    }

    static List<Setting> rows() {
        return rows;
    }

    static float x() {
        return x;
    }

    static float y() {
        return y;
    }

    static float rowH(Setting setting) {
        return setting.getConfigValue() instanceof NumberValue<?> ? SLIDER_ROW_H : ROW_H;
    }

    static float height() {
        float h = HEADER_H + PAD;
        for (Setting row : rows) h += rowH(row);
        return h;
    }

    /** Opens the popup next to the cursor; false when the widget has nothing it can edit here. */
    public static boolean open(DraggableHudElement target, float mouseX, float mouseY, float screenW, float screenH) {
        List<Setting> supported = new ArrayList<>();
        for (Setting setting : SettingFactory.fromDefs(target.getSettingDefs())) {
            if (!supports(setting.getConfigValue())) continue;
            setting.setParent(target);
            setting.preflightI18n();
            supported.add(setting);
        }
        if (supported.isEmpty()) return false;
        close();
        widget = target;
        rows = supported;
        x = clamp(mouseX + 6f, 4f, Math.max(4f, screenW - WIDTH - 4f));
        y = clamp(mouseY, 4f, Math.max(4f, screenH - height() - 4f));
        return true;
    }

    public static void close() {
        sliding = null;
        widget = null;
        rows = List.of();
        if (dirty) {
            dirty = false;
            CompletableFuture.runAsync(() -> {
                try {
                    ConfigSerializer.saveAll();
                } catch (Throwable ignored) {
                    // a failed autosave must not break the chat screen; the next save retries
                }
            });
        }
    }

    public static boolean contains(float mx, float my) {
        return widget != null && mx >= x && mx <= x + WIDTH && my >= y && my <= y + height();
    }

    /** Left click inside the popup. */
    public static void click(float mx, float my) {
        float rowY = y + HEADER_H;
        for (Setting row : rows) {
            float h = rowH(row);
            if (my >= rowY && my < rowY + h) {
                apply(row, mx, rowY);
                return;
            }
            rowY += h;
        }
    }

    public static void drag(float mx) {
        if (sliding != null && sliding.getConfigValue() instanceof NumberValue<?> number) setNumber(number, mx);
    }

    public static void release() {
        sliding = null;
    }

    static float trackX() {
        return x + PAD;
    }

    static float trackW() {
        return WIDTH - PAD * 2f;
    }

    private static void apply(Setting row, float mx, float rowY) {
        ConfigValue<?> value = row.getConfigValue();
        if (value instanceof BooleanValue flag) {
            flag.set(!flag.get());
        } else if (value instanceof ModeValue mode) {
            mode.next();
        } else if (value instanceof EnumValue<?> option) {
            cycle(option);
        } else if (value instanceof NumberValue<?> number) {
            sliding = row;
            setNumber(number, mx);
            return;
        } else {
            return;
        }
        dirty = true;
    }

    private static <E extends Enum<E>> void cycle(EnumValue<E> value) {
        E[] all = value.get().getDeclaringClass().getEnumConstants();
        value.set(all[(value.get().ordinal() + 1) % all.length]);
    }

    private static <N extends Number> void setNumber(NumberValue<N> number, float mx) {
        boolean integer = number.get() instanceof Integer || number.get() instanceof Long;
        double v = QuickSliderMath.valueAt(mx, trackX(), trackW(), number.getMin().doubleValue(), number.getMax().doubleValue(), integer);
        number.set(number.castToType(v));
        dirty = true;
    }

    private static boolean supports(ConfigValue<?> value) {
        return value instanceof BooleanValue || value instanceof NumberValue<?>
                || value instanceof ModeValue || value instanceof EnumValue<?>;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
